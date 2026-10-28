package com.treerunning.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class RouteTest
{
	private static List<RunTask> tasks(int level)
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
		return RunPlanner.buildTasks(s, level, Collections.emptyMap(), 0);
	}

	private static int indexOf(List<RouteItem> items, String key)
	{
		for (int i = 0; i < items.size(); i++)
		{
			if (items.get(i).getKey().equals(key))
			{
				return i;
			}
		}
		return -1;
	}

	@Test
	public void bankBeforeComesAheadOfThatStopsPatches()
	{
		BankStop before = new BankStop("a", RouteStep.CATHERBY, true, "Catherby bank");
		BankStop after = new BankStop("b", RouteStep.CATHERBY, false, "Catherby bank");
		List<RouteItem> items = Route.build(tasks(99), Arrays.asList(before, after));

		int patch = indexOf(items, RunTask.keyFor(PatchLocation.FRUIT_CATHERBY, 0));
		assertEquals(patch - 1, indexOf(items, "bank:a"));
		assertEquals(patch + 1, indexOf(items, "bank:b"));
		assertEquals(RouteStep.CATHERBY, items.get(patch - 1).getStep());
	}

	@Test
	public void nextIsTheBankBeforeAStopsPatches()
	{
		BankStop before = new BankStop("a", RouteStep.CATHERBY, true, "Catherby bank");
		List<RouteItem> items = Route.build(tasks(99), Collections.singletonList(before));
		Set<String> done = new HashSet<>();
		for (RouteItem item : items)
		{
			if (item.getStep().ordinal() < RouteStep.CATHERBY.ordinal())
			{
				done.add(item.getKey());
			}
		}
		RouteItem next = Route.next(items, done);
		assertTrue(next.isBank());
		assertEquals("Bank: Catherby bank", next.getBank().getLabel());

		done.add("bank:a");
		assertEquals(PatchLocation.FRUIT_CATHERBY, Route.next(items, done).getTask().getLocation());
	}

	@Test
	public void bankWithNothingLeftAfterItIsSkipped()
	{
		BankStop last = new BankStop("z", RouteStep.FOSSIL_ISLAND, false, "Bank chest");
		List<RouteItem> items = Route.build(tasks(99), Collections.singletonList(last));
		Set<String> done = new HashSet<>();
		for (RouteItem item : items)
		{
			if (!item.isBank())
			{
				done.add(item.getKey());
			}
		}
		assertNull(Route.next(items, done));
	}

	@Test
	public void levelLockedPatchesAreNotNext()
	{
		// At 89, redwood (90) is listed but not due.
		List<RouteItem> items = Route.build(tasks(89), Collections.emptyList());
		Set<String> done = new HashSet<>();
		done.add(RunTask.keyFor(PatchLocation.TREE_FARMING_GUILD, 0));
		done.add(RunTask.keyFor(PatchLocation.FRUIT_FARMING_GUILD, 0));
		done.add(RunTask.keyFor(PatchLocation.CELASTRUS_FARMING_GUILD, 0));
		RouteItem next = Route.next(items, done);
		assertEquals(PatchLocation.HESPORI_FARMING_GUILD, next.getTask().getLocation());
		assertFalse(items.get(indexOf(items, RunTask.keyFor(PatchLocation.REDWOOD_FARMING_GUILD, 0))).isDue());
	}

	@Test
	public void stepAfterSkipsFinishedStops()
	{
		List<RouteItem> items = Route.build(tasks(99), Collections.emptyList());
		RouteItem first = items.get(0);
		assertEquals(RouteStep.GNOME_STRONGHOLD, Route.stepAfter(items, first, new HashSet<>()));
	}

	@Test
	public void runNeedsCountsDuePatchesOnly()
	{
		List<RunNeeds.Line> needs = RunNeeds.compute(tasks(89));
		// Redwood needs 90, so it's not on the list at 89.
		for (RunNeeds.Line line : needs)
		{
			assertFalse(line.getCrop() == CropType.REDWOOD);
		}
		RunNeeds.Line yew = needs.get(0);
		assertEquals(CropType.YEW, yew.getCrop());
		assertEquals(7, yew.getSaplings());
		assertEquals(70, yew.getPayment());
		int total = RunNeeds.totalSaplings(needs);
		assertEquals(24, total);
		for (RunNeeds.Line line : needs)
		{
			if (line.getCrop() == CropType.HESPORI)
			{
				assertEquals(0, line.getPayment());
			}
		}
	}
}
