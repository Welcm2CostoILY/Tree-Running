package com.treerunning.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.treerunning.data.AllotmentType;
import com.treerunning.data.Compost;
import com.treerunning.data.CropType;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchCondition;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class HerbPlannerTest
{
	private static final long NOW = 1_800_000_000_000L;

	private static HerbSettings settings(Set<HerbLocation> allotments, boolean pay)
	{
		Map<HerbLocation, HerbType> herbs = new EnumMap<>(HerbLocation.class);
		herbs.put(HerbLocation.TROLL_STRONGHOLD, HerbType.SNAPDRAGON);
		return new HerbSettings(herbs, EnumSet.of(HerbLocation.FALADOR, HerbLocation.CATHERBY, HerbLocation.TROLL_STRONGHOLD),
			allotments, AllotmentType.SNAPE_GRASS, pay, Compost.ULTRACOMPOST, 90, 0.10, 0.10, false);
	}

	@Test
	public void onlyEnabledPatchesAreListed()
	{
		List<HerbTask> tasks = HerbPlanner.buildTasks(settings(EnumSet.noneOf(HerbLocation.class), false), new HashMap<>(), NOW);
		assertEquals(3, tasks.size());
		for (HerbTask t : tasks)
		{
			assertFalse(t.getLocation() == HerbLocation.ARDOUGNE);
		}
		// Unseen patches count as due, with harvest, compost and plant.
		assertEquals(PatchCondition.UNKNOWN, tasks.get(0).getCondition());
		assertTrue(tasks.get(0).isDue());
		assertEquals(3, tasks.get(0).getSteps().size());
	}

	@Test
	public void allotmentsAreAddedWhereTurnedOn()
	{
		List<HerbTask> tasks = HerbPlanner.buildTasks(settings(EnumSet.of(HerbLocation.CATHERBY), true), new HashMap<>(), NOW);
		assertEquals(5, tasks.size());
		HerbTask allot = null;
		for (HerbTask t : tasks)
		{
			if (t.isAllotment())
			{
				allot = t;
				break;
			}
		}
		assertEquals(AllotmentType.SNAPE_GRASS, allot.getAllotmentCrop());
		assertEquals(HerbTask.PAY, allot.getSteps().get(allot.getSteps().size() - 1).getId());

		List<HerbPlanner.Need> needs = HerbPlanner.needs(tasks, new HashSet<>(), settings(EnumSet.of(HerbLocation.CATHERBY), true), id -> 0);
		int snapeSeeds = 0;
		int jangerberries = 0;
		for (HerbPlanner.Need n : needs)
		{
			if (n.getItemId() == AllotmentType.SNAPE_GRASS.getSeedId())
			{
				snapeSeeds = n.getNeeded();
			}
			if (n.getItemId() == AllotmentType.SNAPE_GRASS.getPaymentId())
			{
				jangerberries = n.getNeeded();
			}
		}
		assertEquals(6, snapeSeeds);
		assertEquals(10, jangerberries);
	}

	@Test
	public void seenStatesDriveSteps()
	{
		Map<String, SeenPatch> seen = new HashMap<>();
		seen.put(HerbTask.keyFor(HerbLocation.FALADOR, -1), new SeenPatch(PatchCondition.DISEASED, 2, 5, 20, "Ranarr", NOW - 1000));
		seen.put(HerbTask.keyFor(HerbLocation.CATHERBY, -1), new SeenPatch(PatchCondition.GROWING, 1, 5, 20, "Ranarr", NOW - 10 * 60_000L));
		// Planted 90 minutes ago: grown by now.
		seen.put(HerbTask.keyFor(HerbLocation.TROLL_STRONGHOLD, -1), new SeenPatch(PatchCondition.GROWING, 0, 5, 20, "Snapdragon", NOW - 90 * 60_000L));
		List<HerbTask> tasks = HerbPlanner.buildTasks(settings(EnumSet.noneOf(HerbLocation.class), false), seen, NOW);
		HerbTask catherby = tasks.get(0);
		HerbTask falador = tasks.get(1);
		HerbTask troll = tasks.get(2);
		assertEquals(HerbLocation.CATHERBY, catherby.getLocation());
		assertFalse(catherby.isDue());
		assertEquals(NOW - 10 * 60_000L + 60 * 60_000L, catherby.getReadyAt());
		assertEquals(HerbTask.CURE, falador.getSteps().get(0).getId());
		assertEquals(PatchCondition.GROWN, troll.getCondition());

		// Catherby is still growing, so its seed isn't needed.
		Set<String> done = new HashSet<>();
		done.add(falador.stepKey(HerbTask.PLANT));
		List<HerbPlanner.Need> needs = HerbPlanner.needs(tasks, done, settings(EnumSet.noneOf(HerbLocation.class), false), id -> 0);
		assertEquals(2, needs.size());
		assertEquals(HerbType.SNAPDRAGON.getSeedId(), needs.get(0).getItemId());
	}

	@Test
	public void chanceToSaveFollowsTheWikiFormula()
	{
		// Ranarr at 99 with no bonuses: floor(floor(80) * 1 + 1) / 256 = 81/256.
		assertEquals(81 / 256.0, HerbPlanner.chanceToSave(HerbType.RANARR, 99, 0, 0), 1e-9);
		// At level 1 it's the herb's low value: floor(39 + 1) / 256.
		assertEquals(40 / 256.0, HerbPlanner.chanceToSave(HerbType.RANARR, 1, 0, 0), 1e-9);
		// Secateurs and a diary multiply in before the +1.
		assertEquals(Math.floor(80 * 1.1 * 1.1 + 1) / 256.0, HerbPlanner.chanceToSave(HerbType.TORSTOL, 99, 0.10, 0.10), 1e-9);
		// Ultracompost's 6 lives at 99 with secateurs: about 8.6 herbs.
		HerbSettings s = settings(EnumSet.noneOf(HerbLocation.class), false);
		double y = HerbPlanner.expectedYield(HerbType.RANARR, HerbLocation.ARDOUGNE, s);
		assertTrue(y > 8.0 && y < 9.5);
		assertEquals(1.0, HerbPlanner.survival(HerbLocation.TROLL_STRONGHOLD, s), 1e-9);
		assertTrue(HerbPlanner.survival(HerbLocation.ARDOUGNE, s) < 1.0);
	}

	@Test
	public void estimateIsHerbsTimesPriceLessSeedAndCompost()
	{
		HerbSettings s = settings(EnumSet.noneOf(HerbLocation.class), false);
		Map<Integer, Long> prices = new HashMap<>();
		prices.put(HerbType.RANARR.getHerbId(), 7000L);
		prices.put(HerbType.RANARR.getSeedId(), 40000L);
		prices.put(Compost.ULTRACOMPOST.getItemId(), 1000L);
		HerbPlanner.Estimate e = HerbPlanner.estimate(HerbType.RANARR, HerbLocation.TROLL_STRONGHOLD, s, id -> prices.getOrDefault(id, 0L));
		double herbs = HerbPlanner.expectedYield(HerbType.RANARR, HerbLocation.TROLL_STRONGHOLD, s);
		assertEquals(Math.round(herbs * 7000 - 40000 - 1000), e.getProfit());
	}

	// ---------------------------------------------------------------- tree helpers

	private static List<RunTask> treeTasks()
	{
		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, CropType.YEW);
		crops.put(PatchType.FRUIT_TREE, CropType.PAPAYA);
		crops.put(PatchType.HARDWOOD, CropType.IRONWOOD);
		crops.put(PatchType.CALQUAT, CropType.CALQUAT);
		crops.put(PatchType.CELASTRUS, CropType.CELASTRUS);
		crops.put(PatchType.REDWOOD, CropType.REDWOOD);
		crops.put(PatchType.HESPORI, CropType.HESPORI);
		PlanSettings s = new PlanSettings(crops, EnumSet.allOf(PatchLocation.class), XpTable.xpForLevel(99), 1);
		return RunPlanner.buildTasks(s, 99, Collections.emptyMap(), NOW);
	}

	@Test
	public void seenGrowingTreeIsNotDue()
	{
		List<RunTask> tasks = treeTasks();
		Map<String, SeenPatch> seen = new HashMap<>();
		String varrock = RunTask.keyFor(PatchLocation.TREE_VARROCK, 0);
		String falador = RunTask.keyFor(PatchLocation.TREE_FALADOR, 0);
		seen.put(varrock, new SeenPatch(PatchCondition.GROWING, 3, 11, 40, "Yew", NOW - 60_000L));
		seen.put(falador, new SeenPatch(PatchCondition.DISEASED, 3, 11, 40, "Yew", NOW - 60_000L));
		List<RunTask> out = RunPlanner.applySeen(tasks, seen, new HashMap<>(), NOW);
		for (RunTask t : out)
		{
			if (t.getKey().equals(varrock))
			{
				assertFalse(t.isDue());
				assertTrue(t.getNote().startsWith("Ready in"));
			}
			if (t.getKey().equals(falador))
			{
				assertTrue(t.isDue());
			}
		}
		// A sighting older than the last planting is ignored.
		Map<String, Long> planted = new HashMap<>();
		planted.put(varrock, NOW);
		for (RunTask t : RunPlanner.applySeen(tasks, seen, planted, NOW))
		{
			if (t.getKey().equals(varrock))
			{
				assertTrue(t.isDue());
			}
		}
	}

	@Test
	public void inventoryWarningSuggestsABankStop()
	{
		List<RunTask> tasks = treeTasks();
		List<RouteItem> items = Route.build(tasks, new ArrayList<>(), RouteOrder.defaults());
		InventoryFit.Overflow o = InventoryFit.check(items, new HashSet<>());
		// Every patch at 99 with all teleports doesn't fit in one inventory.
		assertTrue(o != null && o.getSlots() > 28 && o.getBankBefore() != null);
		// Everything done: nothing to warn about.
		Set<String> done = new HashSet<>();
		for (RunTask t : tasks)
		{
			done.add(t.getKey());
		}
		assertNull(InventoryFit.check(items, done));
	}

	@Test
	public void bestTimeIsPerPatchCount()
	{
		List<RunRecord> runs = new ArrayList<>();
		RunRecord a = new RunRecord(10_000_000, 100, 19);
		a.startedAt = 10_000_000 - 900_000;
		RunRecord b = new RunRecord(20_000_000, 100, 19);
		b.startedAt = 20_000_000 - 800_000;
		RunRecord c = new RunRecord(30_000_000, 100, 24);
		c.startedAt = 30_000_000 - 600_000;
		runs.add(a);
		runs.add(b);
		runs.add(c);
		assertEquals(800_000, RunRecord.bestTime(runs, 19));
		assertEquals(600_000, RunRecord.bestTime(runs, 24));
		a.profitTracked = true;
		a.value = 500;
		a.cost = 200;
		assertEquals(300, RunRecord.lifetimeProfit(runs));
		assertEquals(a, RunRecord.lastWithProfit(runs));
	}
}
