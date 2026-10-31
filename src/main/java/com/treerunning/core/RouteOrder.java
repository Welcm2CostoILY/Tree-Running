package com.treerunning.core;

import com.treerunning.data.PatchLocation;
import com.treerunning.data.RouteStep;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The order you visit the stops in, and the order of the patches at each stop. Anything the
 * player hasn't moved keeps its built-in order. Saved as two JSON settings.
 */
public final class RouteOrder
{
	private final List<RouteStep> stops;
	private final Map<RouteStep, List<PatchLocation>> patches;

	private RouteOrder(List<RouteStep> stops, Map<RouteStep, List<PatchLocation>> patches)
	{
		this.stops = stops;
		this.patches = patches;
	}

	/** The built-in order. */
	public static RouteOrder defaults()
	{
		return of(Collections.emptyList(), Collections.emptyMap());
	}

	/**
	 * Builds an order from saved names. Unknown names are ignored and anything missing is
	 * added in its built-in position, so new stops and patches always appear.
	 */
	public static RouteOrder fromNames(List<String> stopNames, Map<String, List<String>> patchNames)
	{
		List<RouteStep> stops = new ArrayList<>();
		if (stopNames != null)
		{
			for (String name : stopNames)
			{
				RouteStep step = step(name);
				if (step != null && !stops.contains(step))
				{
					stops.add(step);
				}
			}
		}
		Map<RouteStep, List<PatchLocation>> patches = new EnumMap<>(RouteStep.class);
		if (patchNames != null)
		{
			for (Map.Entry<String, List<String>> e : patchNames.entrySet())
			{
				RouteStep step = step(e.getKey());
				if (step == null || e.getValue() == null)
				{
					continue;
				}
				List<PatchLocation> list = new ArrayList<>();
				for (String name : e.getValue())
				{
					PatchLocation location = location(name);
					if (location != null && location.getStep() == step && !list.contains(location))
					{
						list.add(location);
					}
				}
				patches.put(step, list);
			}
		}
		return of(stops, patches);
	}

	private static RouteOrder of(List<RouteStep> savedStops, Map<RouteStep, List<PatchLocation>> savedPatches)
	{
		List<RouteStep> stops = new ArrayList<>(savedStops);
		for (RouteStep step : RouteStep.values())
		{
			if (!stops.contains(step))
			{
				// Put new stops after the stop that comes before them in the built-in order.
				int at = stops.size();
				for (int i = step.ordinal() - 1; i >= 0; i--)
				{
					int idx = stops.indexOf(RouteStep.values()[i]);
					if (idx >= 0)
					{
						at = idx + 1;
						break;
					}
				}
				if (step.ordinal() == 0)
				{
					at = 0;
				}
				stops.add(at, step);
			}
		}
		Map<RouteStep, List<PatchLocation>> patches = new EnumMap<>(RouteStep.class);
		for (RouteStep step : RouteStep.values())
		{
			List<PatchLocation> list = new ArrayList<>(savedPatches.getOrDefault(step, Collections.emptyList()));
			for (PatchLocation location : PatchLocation.values())
			{
				if (location.getStep() == step && !list.contains(location))
				{
					list.add(location);
				}
			}
			patches.put(step, Collections.unmodifiableList(list));
		}
		return new RouteOrder(Collections.unmodifiableList(stops), Collections.unmodifiableMap(patches));
	}

	public List<RouteStep> getStops()
	{
		return stops;
	}

	public List<PatchLocation> getPatches(RouteStep step)
	{
		return patches.get(step);
	}

	public List<String> stopNames()
	{
		List<String> names = new ArrayList<>();
		for (RouteStep step : stops)
		{
			names.add(step.name());
		}
		return names;
	}

	public Map<String, List<String>> patchNames()
	{
		Map<String, List<String>> names = new HashMap<>();
		for (Map.Entry<RouteStep, List<PatchLocation>> e : patches.entrySet())
		{
			List<String> list = new ArrayList<>();
			for (PatchLocation location : e.getValue())
			{
				list.add(location.name());
			}
			names.put(e.getKey().name(), list);
		}
		return names;
	}

	/** Moves a stop earlier (-1) or later (+1) in the route. */
	public RouteOrder moveStop(RouteStep step, int direction)
	{
		List<RouteStep> list = new ArrayList<>(stops);
		int i = list.indexOf(step);
		int j = i + direction;
		if (i >= 0 && j >= 0 && j < list.size())
		{
			Collections.swap(list, i, j);
		}
		return of(list, patches);
	}

	/** Moves a patch up (-1) or down (+1) within its stop. */
	public RouteOrder movePatch(PatchLocation location, int direction)
	{
		Map<RouteStep, List<PatchLocation>> copy = new EnumMap<>(RouteStep.class);
		copy.putAll(patches);
		List<PatchLocation> list = new ArrayList<>(patches.get(location.getStep()));
		int i = list.indexOf(location);
		int j = i + direction;
		if (i >= 0 && j >= 0 && j < list.size())
		{
			Collections.swap(list, i, j);
		}
		copy.put(location.getStep(), list);
		return of(stops, copy);
	}

	/** Sorts tasks by stop, then by patch, in this order. */
	public List<RunTask> sort(List<RunTask> tasks)
	{
		List<RunTask> sorted = new ArrayList<>(tasks);
		sorted.sort(Comparator
			.comparingInt((RunTask t) -> stops.indexOf(t.getStep()))
			.thenComparingInt(t -> patches.get(t.getStep()).indexOf(t.getLocation()))
			.thenComparingInt(RunTask::getIndex));
		return Collections.unmodifiableList(sorted);
	}

	private static RouteStep step(String name)
	{
		for (RouteStep s : RouteStep.values())
		{
			if (s.name().equals(name))
			{
				return s;
			}
		}
		return null;
	}

	private static PatchLocation location(String name)
	{
		for (PatchLocation l : PatchLocation.values())
		{
			if (l.name().equals(name))
			{
				return l;
			}
		}
		return null;
	}
}
