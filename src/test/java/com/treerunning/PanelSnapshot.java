package com.treerunning;

import com.treerunning.core.BankStop;
import com.treerunning.core.Route;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RouteOrder;
import com.treerunning.core.HerbPlanner;
import com.treerunning.core.HerbSettings;
import com.treerunning.core.HerbTask;
import com.treerunning.core.SeenPatch;
import com.treerunning.data.AllotmentType;
import com.treerunning.data.Compost;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchCondition;
import com.treerunning.core.RunRecord;
import java.util.ArrayList;
import com.treerunning.core.XpTable;
import com.treerunning.data.RouteStep;
import java.util.Collections;
import com.treerunning.core.PlanSettings;
import com.treerunning.core.RunPlanner;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.core.XpProjection;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * Dev tool: renders each tab of the side panel to a PNG with sample data, without
 * starting the client. Run with java.awt.headless=true.
 */
public class PanelSnapshot
{
	public static void main(String[] args) throws Exception
	{
		File outDir = new File(args.length > 0 ? args[0] : "build/snapshots");
		outDir.mkdirs();

		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, CropType.YEW);
		crops.put(PatchType.FRUIT_TREE, CropType.PAPAYA);
		crops.put(PatchType.HARDWOOD, CropType.IRONWOOD);
		crops.put(PatchType.CALQUAT, CropType.CALQUAT);
		crops.put(PatchType.CELASTRUS, CropType.CELASTRUS);
		crops.put(PatchType.REDWOOD, CropType.REDWOOD);
		crops.put(PatchType.HESPORI, CropType.HESPORI);
		PlanSettings settings = new PlanSettings(crops, EnumSet.allOf(PatchLocation.class), XpTable.xpForLevel(99), 1);

		int xp = 4_842_295;
		XpProjection planned = XpProjection.toGoal(xp, settings);
		List<RunTask> tasks = RunPlanner.buildTasks(settings, 89, new HashMap<>(), System.currentTimeMillis());
		Set<String> done = new HashSet<>();
		for (int i = 0; i < 4; i++)
		{
			done.add(tasks.get(i).getKey());
		}
		Map<Integer, Integer> have = new HashMap<>();
		have.put(CropType.YEW.getSapling().getId(), 60);
		have.put(CropType.PAPAYA.getSeed().getId(), 400);
		have.put(CropType.YEW.getPayment().getId(), 1200);
		Map<Integer, Long> price = new HashMap<>();
		price.put(CropType.YEW.getSeed().getId(), 12_655L);
		price.put(CropType.PAPAYA.getSeed().getId(), 1_046L);
		price.put(CropType.IRONWOOD.getSeed().getId(), 31_248L);
		price.put(CropType.YEW.getPayment().getId(), 947L);
		price.put(CropType.PAPAYA.getPayment().getId(), 171L);
		List<BankStop> banks = Collections.singletonList(new BankStop("a", RouteStep.CATHERBY, true, "Catherby bank"));

		PanelData data = new PanelData();
		data.loggedIn = true;
		data.currentXp = xp + 52_000;
		data.level = 89;
		data.goalXp = settings.getGoalXp();
		data.plannedXpPerRun = planned.getAverageXpPerRun();
		data.plannedRuns = planned.getRuns();
		data.runsLeft = planned.getRuns();
		data.tasks = tasks;
		data.items = Route.build(tasks, banks, RouteOrder.defaults());
		data.done = done;
		data.runActive = true;
		data.runXpSoFar = 52_000;
		data.lastMatch = "Yew – Gnome Stronghold";
		Map<Integer, Integer> inv = new HashMap<>();
		inv.put(CropType.YEW.getSapling().getId(), 3);
		inv.put(CropType.YEW.getPayment().getId(), 30);
		inv.put(CropType.PAPAYA.getSapling().getId(), 6);
		inv.put(CropType.PAPAYA.getPayment().getId(), 60);
		data.needs = RunNeeds.compute(tasks, done, id -> inv.getOrDefault(id, 0));
		List<RunRecord> runs = new ArrayList<>();
		long now = System.currentTimeMillis();
		runs.add(new RunRecord(now - 2 * 86_400_000L, 98_412, 21));
		runs.add(new RunRecord(now - 86_400_000L, 101_870, 22));
		runs.add(new RunRecord(0, 95_002, -1));
		data.runs = runs;
		Map<RouteStep, String> travel = new EnumMap<>(RouteStep.class);
		travel.put(RouteStep.CATHERBY, "Catherby teleport tab, walk east");
		data.travel = travel;
		data.supplies = SupplyCalculator.compute(XpProjection.forRuns(xp, planned.getRuns(), settings).getPlantings(),
			id -> have.getOrDefault(id, 0), id -> price.getOrDefault(id, 0L));
		data.bankSeenAt = System.currentTimeMillis() - 2 * 3_600_000L;
		data.slots = 27;
		Set<String> paid = new HashSet<>();
		paid.add(tasks.get(0).getKey());
		data.paid = paid;
		Set<String> unpaid = new HashSet<>();
		unpaid.add(tasks.get(1).getKey());
		data.unpaid = unpaid;
		Map<String, SeenPatch> seenTrees = new HashMap<>();
		seenTrees.put(tasks.get(5).getKey(), new SeenPatch(PatchCondition.CHECK_HEALTH, 10, 11, 40, "Yew", now - 3_600_000L));
		seenTrees.put(tasks.get(6).getKey(), new SeenPatch(PatchCondition.DISEASED, 4, 11, 40, "Yew", now - 600_000L));

		// Herbs: sample patches in a few states.
		Map<HerbLocation, HerbType> herbs = new EnumMap<>(HerbLocation.class);
		for (HerbLocation l : HerbLocation.values())
		{
			herbs.put(l, l.isDiseaseFree() ? HerbType.SNAPDRAGON : HerbType.RANARR);
		}
		Set<HerbLocation> on = EnumSet.allOf(HerbLocation.class);
		on.remove(HerbLocation.HARMONY_ISLAND);
		HerbSettings herbSettings = new HerbSettings(herbs, on, EnumSet.of(HerbLocation.CATHERBY), AllotmentType.SNAPE_GRASS,
			false, Compost.BOTTOMLESS, 90, 0.10, 0.05, false, true);
		Map<String, SeenPatch> seen = new HashMap<>(seenTrees);
		seen.put(HerbTask.keyFor(HerbLocation.FARMING_GUILD, -1), new SeenPatch(PatchCondition.GROWN, 0, 1, 20, "Snapdragon", now - 900_000L));
		seen.put(HerbTask.keyFor(HerbLocation.FALADOR, -1), new SeenPatch(PatchCondition.DISEASED, 2, 5, 20, "Ranarr", now - 900_000L));
		seen.put(HerbTask.keyFor(HerbLocation.WEISS, -1), new SeenPatch(PatchCondition.GROWING, 2, 5, 20, "Snapdragon", now - 600_000L));
		seen.put(HerbTask.keyFor(HerbLocation.CATHERBY, 0), new SeenPatch(PatchCondition.GROWING, 1, 8, 10, "Snape grass", now - 600_000L));
		data.seen = seen;
		Map<Integer, Long> herbPrice = new HashMap<>(price);
		herbPrice.put(HerbType.RANARR.getSeedId(), 38_000L);
		herbPrice.put(HerbType.RANARR.getHerbId(), 7_400L);
		herbPrice.put(HerbType.SNAPDRAGON.getSeedId(), 60_000L);
		herbPrice.put(HerbType.SNAPDRAGON.getHerbId(), 9_800L);
		herbPrice.put(HerbType.TORSTOL.getSeedId(), 55_000L);
		herbPrice.put(HerbType.TORSTOL.getHerbId(), 7_500L);
		herbPrice.put(HerbType.TOADFLAX.getSeedId(), 3_000L);
		herbPrice.put(HerbType.TOADFLAX.getHerbId(), 2_500L);
		herbPrice.put(21483, 1_200L);
		data.herbTasks = HerbPlanner.buildTasks(herbSettings, seen, now);
		Set<String> herbDone = new HashSet<>();
		herbDone.add(HerbTask.stepKey(HerbTask.keyFor(HerbLocation.FARMING_GUILD, -1), HerbTask.HARVEST));
		data.herbDone = herbDone;
		data.herbActive = true;
		data.herbStartedAt = now - 4 * 60_000L;
		Map<Integer, Integer> herbInv = new HashMap<>();
		herbInv.put(HerbType.RANARR.getSeedId(), 3);
		herbInv.put(HerbType.SNAPDRAGON.getSeedId(), 4);
		data.herbNeeds = HerbPlanner.needs(data.herbTasks, herbDone, herbSettings, id -> herbInv.getOrDefault(id, 0));
		data.secateursExpected = true;
		data.secateursCarried = false;
		data.herbPicked = 9;
		data.herbValue = 88_200;
		data.herbCost = 60_300;
		data.herbSeedCost = 60_000;
		data.herbCompostCost = 300;
		Map<String, Integer> seedsPlanted = new java.util.LinkedHashMap<>();
		seedsPlanted.put("Snapdragon", 1);
		data.herbSeeds = seedsPlanted;
		data.herbXp = 973;
		data.herbLastMatch = "Picked 9 Snapdragon at Farming Guild herb patch";
		data.picker = HerbPlanner.picker(herbSettings, id -> herbPrice.getOrDefault(id, 0L));
		Map<String, Long> est = new HashMap<>();
		for (HerbTask t : data.herbTasks)
		{
			if (!t.isAllotment())
			{
				est.put(t.getKey(), HerbPlanner.estimate(t.getHerb(), t.getLocation(), herbSettings, id -> herbPrice.getOrDefault(id, 0L)).getProfit());
			}
		}
		data.herbEstimates = est;
		data.herbXpPerRun = HerbPlanner.xpPerRun(herbSettings);
		data.herbRunsPerDay = 4;
		List<RunRecord> herbRuns = new ArrayList<>();
		RunRecord h1 = new RunRecord(now - 3 * 3_600_000L, 6_110, 8);
		h1.startedAt = h1.endedAt - 9 * 60_000L;
		h1.value = 512_000;
		h1.cost = 361_000;
		h1.picked = 64;
		h1.seedCost = 356_000;
		h1.profitTracked = true;
		herbRuns.add(h1);
		data.herbRuns = herbRuns;
		RunRecord t1 = runs.get(1);
		t1.startedAt = t1.endedAt - 14 * 60_000L;
		t1.cost = 412_000;
		t1.value = 96_000;
		t1.profitTracked = true;
		data.lastTreeRun = t1;
		data.lastHerbRun = h1;
		data.lifetimeHerbProfit = h1.profit();
		data.lifetimeTreeProfit = t1.profit();
		Map<RouteStep, List<HerbTask>> otw = new EnumMap<>(RouteStep.class);
		for (HerbTask t : data.herbTasks)
		{
			if (t.getLocation() == HerbLocation.FARMING_GUILD && !t.isAllotment())
			{
				otw.put(RouteStep.FARMING_GUILD, java.util.Collections.singletonList(t));
			}
		}
		data.onTheWay = otw;

		SwingUtilities.invokeAndWait(() ->
		{
			TreeRunningPanel panel = new TreeRunningPanel(null);
			panel.loadRouteEditor(banks, RouteOrder.defaults(), travel, new EnumMap<>(HerbLocation.class),
				location -> location != PatchLocation.CRYSTAL_PRIFDDINAS, location -> location != HerbLocation.HARMONY_ISLAND);
			panel.update(data);
			JTabbedPane tabs = (JTabbedPane) panel.getComponent(0);
			for (int i = 0; i < tabs.getTabCount(); i++)
			{
				tabs.setSelectedIndex(i);
				panel.setSize(new Dimension(242, 2600));
				layoutAll(panel);
				BufferedImage image = new BufferedImage(242, 2600, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = image.createGraphics();
				panel.printAll(g);
				g.dispose();
				try
				{
					ImageIO.write(image, "png", new File(outDir, "tab-" + i + ".png"));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		});
		System.out.println("Wrote snapshots to " + outDir.getAbsolutePath());
	}

	private static void layoutAll(Component c)
	{
		c.doLayout();
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				layoutAll(child);
			}
		}
		c.validate();
	}
}
