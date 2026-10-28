package com.treerunning.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class PlannerTest
{
	private static final int LEVEL_89_XP = 4_842_295;
	private static final long NOW = 1_800_000_000_000L;

	private static PlanSettings settings(CropType tree, CropType fruit)
	{
		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, tree);
		crops.put(PatchType.FRUIT_TREE, fruit);
		crops.put(PatchType.HARDWOOD, CropType.IRONWOOD);
		crops.put(PatchType.CALQUAT, CropType.CALQUAT);
		crops.put(PatchType.CELASTRUS, CropType.CELASTRUS);
		crops.put(PatchType.REDWOOD, CropType.REDWOOD);
		crops.put(PatchType.HESPORI, CropType.HESPORI);
		return new PlanSettings(crops, EnumSet.allOf(PatchLocation.class), XpTable.xpForLevel(99), 1);
	}

	@Test
	public void xpTableMatchesKnownValues()
	{
		assertEquals(LEVEL_89_XP, XpTable.xpForLevel(89));
		assertEquals(5_346_332, XpTable.xpForLevel(90));
		assertEquals(13_034_431, XpTable.xpForLevel(99));
		assertEquals(89, XpTable.levelForXp(LEVEL_89_XP));
		assertEquals(89, XpTable.levelForXp(5_346_331));
		assertEquals(90, XpTable.levelForXp(5_346_332));
		assertEquals(99, XpTable.levelForXp(200_000_000));
	}

	@Test
	public void intervalsMatchGrowthTimes()
	{
		assertEquals(1, CropType.YEW.getIntervalRuns(1));
		assertEquals(1, CropType.CALQUAT.getIntervalRuns(1));
		assertEquals(4, CropType.IRONWOOD.getIntervalRuns(1));
		assertEquals(5, CropType.REDWOOD.getIntervalRuns(1));
		assertEquals(2, CropType.HESPORI.getIntervalRuns(1));
	}

	@Test
	public void yewPapayaAllPatchesTakesAbout45Runs()
	{
		XpProjection projection = XpProjection.toGoal(LEVEL_89_XP, settings(CropType.YEW, CropType.PAPAYA));
		assertTrue(projection.reachesGoal());
		// Five hardwood patches (Fossil Island x3, Locus Oasis, Anglers' Retreat).
		assertEquals(45, projection.getRuns());

		int runs = projection.getRuns();
		Map<CropType, Integer> plantings = projection.getPlantings();
		assertEquals(7 * runs, (int) plantings.get(CropType.YEW));
		assertEquals(7 * runs, (int) plantings.get(CropType.PAPAYA));
		assertEquals(3 * runs, (int) plantings.get(CropType.CALQUAT));
		assertEquals(runs, (int) plantings.get(CropType.CELASTRUS));
		assertEquals(5 * (int) Math.ceil(runs / 4.0), (int) plantings.get(CropType.IRONWOOD));
		assertEquals((int) Math.ceil(runs / 2.0), (int) plantings.get(CropType.HESPORI));
		// Redwood only starts once you hit 90.
		assertTrue(plantings.get(CropType.REDWOOD) < Math.ceil(runs / 5.0) + 1);
	}

	@Test
	public void fasterCropsTakeFewerRuns()
	{
		int magic = XpProjection.toGoal(LEVEL_89_XP, settings(CropType.MAGIC, CropType.DRAGONFRUIT)).getRuns();
		int yewPalm = XpProjection.toGoal(LEVEL_89_XP, settings(CropType.YEW, CropType.PALM)).getRuns();
		int yewPapaya = XpProjection.toGoal(LEVEL_89_XP, settings(CropType.YEW, CropType.PAPAYA)).getRuns();
		int maple = XpProjection.toGoal(LEVEL_89_XP, settings(CropType.MAPLE, CropType.PAPAYA)).getRuns();
		assertTrue(magic < yewPalm);
		assertTrue(yewPalm < yewPapaya);
		assertTrue(yewPapaya < maple);
	}

	@Test
	public void forRunsPlantsExactlyThatManyRuns()
	{
		XpProjection p = XpProjection.forRuns(LEVEL_89_XP, 10, settings(CropType.YEW, CropType.PAPAYA));
		assertEquals(10, p.getRuns());
		assertEquals(70, (int) p.getPlantings().get(CropType.YEW));
	}

	@Test
	public void runRateHelpers()
	{
		// Last four runs: 200, 200, 100, 200
		assertEquals(175.0, XpProjection.averageOfRecent(java.util.Arrays.asList(100, 100, 200, 200, 100, 200), 4), 0.001);
		assertEquals(0, XpProjection.runsAtRate(100, 100, 50));
		assertEquals(3, XpProjection.runsAtRate(0, 101, 50));
		assertEquals(-1, XpProjection.runsAtRate(0, 101, 0));
	}

	@Test
	public void growingHardwoodIsNotDue()
	{
		PlanSettings s = settings(CropType.YEW, CropType.PAPAYA);
		Map<String, Long> planted = new HashMap<>();
		planted.put(RunTask.keyFor(PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST, 0), NOW - 60 * 60_000L);
		List<RunTask> tasks = RunPlanner.buildTasks(s, 89, planted, NOW);

		RunTask fossil0 = find(tasks, PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST, 0);
		RunTask fossil1 = find(tasks, PatchLocation.HARDWOOD_FOSSIL_ISLAND_MIDDLE, 0);
		assertFalse(fossil0.isDue());
		assertTrue(fossil0.getNote().startsWith("Ready in"));
		assertTrue(fossil1.isDue());

		// Redwood needs 90.
		assertFalse(find(tasks, PatchLocation.REDWOOD_FARMING_GUILD, 0).isDue());
		// 25 patches, including Anglers' Retreat and three at Fossil Island.
		assertEquals(25, tasks.size());
		// Route order: Farming Guild first, Fossil Island last.
		assertEquals(RouteStep.FARMING_GUILD, tasks.get(0).getStep());
		assertEquals(RouteStep.FOSSIL_ISLAND, tasks.get(tasks.size() - 1).getStep());
	}


	@Test
	public void supplyLinesCombineSharedPayments()
	{
		Map<CropType, Integer> plantings = new EnumMap<>(CropType.class);
		plantings.put(CropType.MAGIC, 10);
		plantings.put(CropType.DRAGONFRUIT, 4);
		List<SupplyCalculator.SupplyLine> lines = SupplyCalculator.compute(plantings, id -> 5, id -> 100);

		SupplyCalculator.SupplyLine coconuts = lines.stream().filter(l -> l.getName().equals("Coconut")).findFirst().get();
		assertEquals(10 * 25 + 4 * 15, coconuts.getNeed());
		assertTrue(coconuts.isPayment());

		SupplyCalculator.SupplyLine magic = lines.stream().filter(l -> l.getName().equals("Magic")).findFirst().get();
		assertEquals(10, magic.getNeed());
		// seed + 2 seedling stages + sapling, 5 of each
		assertEquals(20, magic.getHave());
		assertEquals(0, magic.getShort());
		// Prices: the seed for crop rows, the item for payments.
		assertEquals(100, magic.getPriceEach());
		assertEquals((10 * 25 + 4 * 15 - 5) * 100L, coconuts.getShortCost());
	}

	@Test
	public void xpGoalPastNinetyNineKeepsGoing()
	{
		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, CropType.YEW);
		PlanSettings s = new PlanSettings(crops, EnumSet.allOf(PatchLocation.class), 50_000_000, 1);
		XpProjection p = XpProjection.toGoal(LEVEL_89_XP, s);
		assertTrue(p.reachesGoal());
		// 7 yews a run: (50M - 4.84M) / 50,056 per run.
		assertEquals((int) Math.ceil((50_000_000 - LEVEL_89_XP) / (7 * 7150.9)), p.getRuns());
	}

	private static RunTask find(List<RunTask> tasks, PatchLocation location, int index)
	{
		return tasks.stream().filter(t -> t.getLocation() == location && t.getIndex() == index).findFirst().get();
	}
}
