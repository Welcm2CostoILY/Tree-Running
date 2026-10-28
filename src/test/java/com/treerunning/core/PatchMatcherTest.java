package com.treerunning.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class PatchMatcherTest
{
	private static final int FALADOR_REGION = 11828;
	private static final int FOSSIL_REGION = 14651;
	private static final int FARMING_GUILD_REGION = 4922;
	private static final int NOWHERE = 1;

	private static List<RunTask> tasks()
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
		return RunPlanner.buildTasks(s, 99, Collections.emptyMap(), 0);
	}

	@Test
	public void plantingOutOfOrderTicksThePatchYouAreAt()
	{
		// Route order starts at the Farming Guild, but you planted in Falador first.
		PatchMatcher.Match m = PatchMatcher.match(tasks(), new HashSet<>(), PatchType.TREE, FALADOR_REGION,
			Collections.singleton(PatchLocation.TREE_FALADOR));
		assertEquals(PatchLocation.TREE_FALADOR, m.getTask().getLocation());
		assertEquals(PatchMatcher.Confidence.PATCH, m.getConfidence());
	}

	@Test
	public void regionAloneIsEnoughWhenThereIsOnePatch()
	{
		PatchMatcher.Match m = PatchMatcher.match(tasks(), new HashSet<>(), PatchType.TREE, FALADOR_REGION, Collections.emptySet());
		assertEquals(PatchLocation.TREE_FALADOR, m.getTask().getLocation());
		assertEquals(PatchMatcher.Confidence.REGION, m.getConfidence());
	}

	@Test
	public void varbitTellsFossilIslandPatchesApart()
	{
		PatchMatcher.Match m = PatchMatcher.match(tasks(), new HashSet<>(), PatchType.HARDWOOD, FOSSIL_REGION,
			Collections.singleton(PatchLocation.HARDWOOD_FOSSIL_ISLAND_WEST));
		assertEquals(PatchLocation.HARDWOOD_FOSSIL_ISLAND_WEST, m.getTask().getLocation());
	}

	@Test
	public void sharedVarbitInAnotherRegionIsIgnored()
	{
		// 4771 is also Fossil Island east's varbit, but you're in Falador.
		PatchMatcher.Match m = PatchMatcher.match(tasks(), new HashSet<>(), PatchType.TREE, FALADOR_REGION,
			Collections.singleton(PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST));
		assertEquals(PatchLocation.TREE_FALADOR, m.getTask().getLocation());
	}

	@Test
	public void farmingGuildPatchesAreMatchedByType()
	{
		Set<PatchLocation> changed = EnumSet.of(PatchLocation.CELASTRUS_FARMING_GUILD);
		PatchMatcher.Match m = PatchMatcher.match(tasks(), new HashSet<>(), PatchType.CELASTRUS, FARMING_GUILD_REGION, changed);
		assertEquals(PatchLocation.CELASTRUS_FARMING_GUILD, m.getTask().getLocation());
	}

	@Test
	public void unknownPlaceFallsBackToRouteOrder()
	{
		PatchMatcher.Match m = PatchMatcher.match(tasks(), new HashSet<>(), PatchType.TREE, NOWHERE, Collections.emptySet());
		assertEquals(PatchLocation.TREE_FARMING_GUILD, m.getTask().getLocation());
		assertEquals(PatchMatcher.Confidence.ROUTE, m.getConfidence());
	}

	@Test
	public void alreadyTickedPatchIsSkipped()
	{
		Set<String> done = new HashSet<>();
		done.add(RunTask.keyFor(PatchLocation.TREE_FALADOR, 0));
		PatchMatcher.Match m = PatchMatcher.match(tasks(), done, PatchType.TREE, FALADOR_REGION, Collections.emptySet());
		// Falador is done, so it falls back to the next tree patch in route order.
		assertEquals(PatchLocation.TREE_FARMING_GUILD, m.getTask().getLocation());
	}

	@Test
	public void nothingLeftReturnsNull()
	{
		Set<String> done = new HashSet<>();
		for (RunTask t : tasks())
		{
			if (t.getLocation().getPatchType() == PatchType.CALQUAT)
			{
				done.add(t.getKey());
			}
		}
		assertNull(PatchMatcher.match(tasks(), done, PatchType.CALQUAT, NOWHERE, Collections.emptySet()));
	}
}
