// JavaScript port of the plugin's planner (XpTable, RunPlanner, XpProjection,
// SupplyCalculator, InventoryPlanner). Kept line-for-line with the Java so the preview
// shows the same numbers. DATA is the JSON exported by ExportPreviewData.
function makePlanner(DATA) {
  const CROPS = Object.fromEntries(DATA.crops.map(c => [c.key, c]));
  const STEPS = Object.fromEntries(DATA.steps.map((s, i) => [s.key, Object.assign({ order: i }, s)]));
  const LOCS = DATA.locations.map((l, i) => Object.assign({ order: i }, l));

  const XP = [0];
  (function () {
    let points = 0;
    for (let level = 1; level < 127; level++) {
      XP[level] = Math.floor(points / 4);
      points += Math.floor(level + 300 * Math.pow(2, level / 7));
    }
  })();
  const xpForLevel = l => XP[Math.max(1, Math.min(126, l))];
  const levelForXp = xp => { let l = 1; while (l < 99 && xp >= XP[l + 1]) l++; return l; };

  const interval = (crop, rpd) => Math.max(1, Math.ceil(crop.growth / (1440 / Math.max(1, rpd))));

  // settings: { crops: {TREE: key, ...}, enabled: Set(locKey), goalLevel, runsPerDay, reservedSlots, pickProduce }
  const isEnabled = (s, loc) => s.enabled.has(loc.key) && !!s.crops[loc.type];
  const cropFor = (s, type) => CROPS[s.crops[type]];

  function buildTasks(s, level, plantedAt, now) {
    const locs = LOCS.slice().sort((a, b) => STEPS[a.step].order - STEPS[b.step].order || a.order - b.order);
    const between = 1440 / s.runsPerDay;
    const tasks = [];
    for (const loc of locs) {
      if (!isEnabled(s, loc)) continue;
      const crop = cropFor(s, loc.type);
      for (let i = 0; i < loc.count; i++) {
        const key = loc.key + '#' + i;
        let due = true, note = null;
        if (crop.level > level) { due = false; note = 'Needs ' + crop.level + ' Farming'; }
        else if (crop.growth > between && plantedAt[key] != null) {
          const readyAt = plantedAt[key] + crop.growth * 60000;
          if (now < readyAt) { due = false; note = 'Ready in ' + fmtDuration(readyAt - now); }
        }
        tasks.push({ key, loc, index: i, crop, due, note, step: STEPS[loc.step] });
      }
    }
    return tasks;
  }

  function fmtDuration(ms) {
    const total = Math.floor(Math.max(0, ms) / 60000), h = Math.floor(total / 60), m = total % 60;
    return h >= 24 ? Math.floor(h / 24) + 'd ' + (h % 24) + 'h' : h + 'h ' + m + 'm';
  }

  function simulate(currentXp, goalXp, s, maxRuns) {
    const plantings = {}, last = {};
    let xp = currentXp, run = 0;
    while (xp < goalXp && run < maxRuns) {
      run++;
      const level = levelForXp(Math.floor(xp));
      let gained = 0;
      for (const loc of LOCS) {
        if (!isEnabled(s, loc)) continue;
        const crop = cropFor(s, loc.type);
        if (crop.level > level) continue;
        if (last[loc.key] != null && run - last[loc.key] < interval(crop, s.runsPerDay)) continue;
        gained += crop.xp * loc.count;
        plantings[crop.key] = (plantings[crop.key] || 0) + loc.count;
        last[loc.key] = run;
      }
      if (gained <= 0) break;
      xp += gained;
    }
    const reached = xp >= goalXp;
    return { runs: run, reached, avg: run === 0 ? 0 : (Math.min(xp, goalXp) - currentXp) / run, plantings };
  }
  const toGoal = (xp, s) => simulate(xp, xpForLevel(s.goalLevel), s, 5000);
  const forRuns = (xp, runs, s) => simulate(xp, Infinity, s, runs);

  function supplies(plantings, have) {
    const crops = [], payNeed = new Map();
    for (const c of DATA.crops) {
      const count = plantings[c.key];
      if (!count) continue;
      crops.push({ name: c.seedOnly ? c.planted : c.name, payment: false, need: count, have: have(c.key) });
      if (c.payment) payNeed.set(c.payment, (payNeed.get(c.payment) || 0) + count * c.paymentQty);
    }
    const pays = [...payNeed].map(([name, need]) => ({ name, payment: true, need, have: have(name) }));
    return crops.concat(pays);
  }

  function inventoryPlan(tasks, s) {
    const byStep = new Map();
    for (const t of tasks) if (t.due) { if (!byStep.has(t.step.key)) byStep.set(t.step.key, []); byStep.get(t.step.key).push(t); }
    const carry = new Set(), payments = new Map(), produce = new Set();
    for (const [stepKey, list] of byStep) {
      if (STEPS[stepKey].carry) carry.add(STEPS[stepKey].carry);
      for (const t of list) {
        if (t.crop.payment) payments.set(t.crop.payment, (payments.get(t.crop.payment) || 0) + t.crop.paymentQty);
        if (s.pickProduce && t.crop.produce) produce.add(t.crop.produce);
      }
    }
    const picking = produce.size ? 6 : 0;
    const fixed = s.reservedSlots + carry.size + payments.size + produce.size + picking;
    const capacity = Math.max(1, 28 - fixed);
    const legs = [];
    let legSteps = [], withdraw = new Map(), slots = 0, bankAt = 'Any bank before you start', prev = null;
    for (const [stepKey, list] of byStep) {
      const step = STEPS[stepKey];
      if (legSteps.length && slots + list.length > capacity) {
        legs.push({ bankAt, steps: legSteps, withdraw, slots });
        bankAt = bankBetween(prev, step);
        legSteps = []; withdraw = new Map(); slots = 0;
      }
      legSteps.push(step);
      for (const t of list) withdraw.set(t.crop.planted, (withdraw.get(t.crop.planted) || 0) + 1);
      slots += list.length;
      prev = step;
    }
    if (legSteps.length) legs.push({ bankAt, steps: legSteps, withdraw, slots });
    return { carry: [...carry], payments, produce: [...produce], reserved: s.reservedSlots, picking, capacity, legs };
  }

  function bankBetween(finished, next) {
    if (finished && finished.bank) return finished.bank + ' (after ' + finished.name + ')';
    if (next.bank) return next.bank + ' (when you arrive at ' + next.name + ')';
    return DATA.fallbackBank;
  }

  return { CROPS, STEPS, LOCS, xpForLevel, levelForXp, buildTasks, toGoal, forRuns, supplies, inventoryPlan, interval };
}
if (typeof module !== 'undefined') module.exports = { makePlanner };
