package com.treerunning.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class OrderAndNeedsTest
{
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

	// ---------------------------------------------------------------- RouteOrder

	@Test
	public void defaultsCoverEveryStopAndPatch()
	{
		RouteOrder order = RouteOrder.defaults();
		int patches = 0;
		for (RouteStep step : order.getStops())
		{
			patches += order.getPatches(step).size();
		}
		assertEquals(PatchLocation.values().length, patches);
	}

	@Test
	public void movingAStopChangesTheRoute()
	{
		RouteOrder order = RouteOrder.defaults();
		RouteStep second = order.getStops().get(1);
		RouteOrder moved = order.moveStop(second, -1);
		assertSame(second, moved.getStops().get(0));
		// Moving the first stop up does nothing.
		assertEquals(moved.getStops(), moved.moveStop(second, -1).getStops());

		List<RouteItem> items = Route.build(moved.sort(tasks()), Collections.emptyList(), moved);
		assertSame(second, items.get(0).getStep());
	}

	@Test
	public void savedNamesRoundTripAndUnknownNamesAreIgnored()
	{
		RouteOrder order = RouteOrder.defaults().moveStop(RouteStep.ANGLERS_RETREAT, -1);
		List<String> stops = new ArrayList<>(order.stopNames());
		stops.add("NOT_A_STOP");
		Map<String, List<String>> patches = new HashMap<>(order.patchNames());
		RouteOrder loaded = RouteOrder.fromNames(stops, patches);
		assertEquals(order.getStops(), loaded.getStops());

		// Nothing saved gives the defaults.
		assertEquals(RouteOrder.defaults().getStops(), RouteOrder.fromNames(null, null).getStops());
		// A partial save still has every stop; missing ones go in their built-in place.
		RouteOrder partial = RouteOrder.fromNames(Collections.singletonList("ANGLERS_RETREAT"), null);
		assertEquals(RouteStep.values().length, partial.getStops().size());
		assertSame(RouteStep.values()[0], partial.getStops().get(0));
	}

	@Test
	public void movingAPatchReordersItWithinItsStop()
	{
		RouteStep fossil = PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST.getStep();
		RouteOrder order = RouteOrder.defaults();
		List<PatchLocation> before = order.getPatches(fossil);
		assertTrue(before.size() > 1);
		PatchLocation last = before.get(before.size() - 1);
		RouteOrder moved = order.movePatch(last, -1);
		List<PatchLocation> after = moved.getPatches(fossil);
		assertEquals(last, after.get(after.size() - 2));

		List<RunTask> sorted = moved.sort(tasks());
		int a = -1;
		int b = -1;
		for (int i = 0; i < sorted.size(); i++)
		{
			if (sorted.get(i).getLocation() == last)
			{
				a = i;
			}
			if (sorted.get(i).getLocation() == before.get(before.size() - 2))
			{
				b = i;
			}
		}
		assertTrue(a < b);
	}

	// ---------------------------------------------------------------- RunNeeds

	private static RunNeeds.Line line(List<RunNeeds.Line> lines, CropType crop)
	{
		for (RunNeeds.Line l : lines)
		{
			if (l.getCrop() == crop)
			{
				return l;
			}
		}
		return null;
	}

	@Test
	public void needsCountWhatYouCarryAndSkipDonePatches()
	{
		List<RunTask> tasks = tasks();
		Set<String> done = new HashSet<>();
		int yews = 0;
		for (RunTask t : tasks)
		{
			if (t.getCrop() == CropType.YEW && t.isDue())
			{
				yews++;
			}
		}
		for (RunTask t : tasks)
		{
			if (t.getCrop() == CropType.YEW)
			{
				done.add(t.getKey());
				break;
			}
		}
		Map<Integer, Integer> inv = new HashMap<>();
		inv.put(CropType.YEW.getSapling().getId(), 2);
		List<RunNeeds.Line> lines = RunNeeds.compute(tasks, done, id -> inv.getOrDefault(id, 0));
		RunNeeds.Line yew = line(lines, CropType.YEW);
		assertEquals(yews - 1, yew.getSaplings());
		assertEquals(2, yew.getSaplingsCarried());
		assertTrue(yew.isShort());
		assertTrue(RunNeeds.shortCount(lines) >= 1);
	}

	@Test
	public void carriedItemsAreSharedNotDoubleCounted()
	{
		// Calquat and magic both pay with coconuts in some setups; whatever shares an item must split it.
		List<RunTask> tasks = tasks();
		Map<Integer, Integer> inv = new HashMap<>();
		for (RunTask t : tasks)
		{
			if (t.getCrop().getPayment() != null)
			{
				inv.put(t.getCrop().getPayment().getId(), 1);
			}
		}
		List<RunNeeds.Line> lines = RunNeeds.compute(tasks, new HashSet<>(), id -> inv.getOrDefault(id, 0));
		Map<Integer, Integer> handedOut = new HashMap<>();
		for (RunNeeds.Line l : lines)
		{
			if (l.getCrop().getPayment() != null)
			{
				handedOut.merge(l.getCrop().getPayment().getId(), l.getPaymentCarried(), Integer::sum);
			}
		}
		for (int given : handedOut.values())
		{
			assertTrue(given <= 1);
		}
	}

	@Test
	public void nothingDueMeansNoLines()
	{
		List<RunTask> tasks = tasks();
		Set<String> done = new HashSet<>();
		for (RunTask t : tasks)
		{
			done.add(t.getKey());
		}
		assertTrue(RunNeeds.compute(tasks, done, id -> 0).isEmpty());
	}

	// ---------------------------------------------------------------- ActionMatcher

	@Test
	public void xpWithOnePatchChangeMatches()
	{
		List<ActionMatcher.Change> changes = Arrays.asList(
			new ActionMatcher.Change(100, "TREE_FALADOR#0"),
			new ActionMatcher.Change(90, "TREE_VARROCK#0"));
		assertEquals("TREE_FALADOR#0", ActionMatcher.find(101, changes, new HashSet<>()));
		assertNull(ActionMatcher.find(103, changes, new HashSet<>()));
	}

	@Test
	public void twoChangesAtOnceAreTooAmbiguous()
	{
		List<ActionMatcher.Change> changes = Arrays.asList(
			new ActionMatcher.Change(100, "east"),
			new ActionMatcher.Change(100, "west"));
		assertNull(ActionMatcher.find(100, changes, new HashSet<>()));
		// Unless one of them is already done.
		assertEquals("west", ActionMatcher.find(100, changes, new HashSet<>(Collections.singletonList("east"))));
	}
}
