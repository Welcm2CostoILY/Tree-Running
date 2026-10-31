package com.treerunning.core;

import com.treerunning.data.RouteStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Puts patches and bank stops in the order you do them: stops in the player's order, and at
 * each stop the bank stops set to "before", then its patches, then the bank stops set to "after".
 */
public final class Route
{
	private Route()
	{
	}

	public static List<RouteItem> build(List<RunTask> tasks, List<BankStop> bankStops)
	{
		return build(tasks, bankStops, RouteOrder.defaults());
	}

	/** Tasks should already be sorted with {@link RouteOrder#sort}. */
	public static List<RouteItem> build(List<RunTask> tasks, List<BankStop> bankStops, RouteOrder order)
	{
		List<RouteItem> items = new ArrayList<>();
		for (RouteStep step : order.getStops())
		{
			addBanks(items, step, bankStops, true);
			for (RunTask task : tasks)
			{
				if (task.getStep() == step)
				{
					items.add(RouteItem.patch(task));
				}
			}
			addBanks(items, step, bankStops, false);
		}
		return items;
	}

	private static void addBanks(List<RouteItem> items, RouteStep step, List<BankStop> bankStops, boolean before)
	{
		for (BankStop bank : bankStops)
		{
			if (bank.getRouteStep() == step && bank.before == before)
			{
				items.add(RouteItem.bank(step, bank));
			}
		}
	}

	/**
	 * The next thing to do: the first unplanted due patch, or a bank stop that still has
	 * unplanted due patches after it. Null when the run is finished.
	 */
	public static RouteItem next(List<RouteItem> items, Set<String> done)
	{
		for (int i = 0; i < items.size(); i++)
		{
			RouteItem item = items.get(i);
			if (done.contains(item.getKey()))
			{
				continue;
			}
			if (!item.isBank())
			{
				if (item.getTask().isDue())
				{
					return item;
				}
			}
			else if (hasPatchAfter(items, i, done))
			{
				return item;
			}
		}
		return null;
	}

	private static boolean hasPatchAfter(List<RouteItem> items, int index, Set<String> done)
	{
		for (int j = index + 1; j < items.size(); j++)
		{
			RouteItem later = items.get(j);
			if (!later.isBank() && later.getTask().isDue() && !done.contains(later.getKey()))
			{
				return true;
			}
		}
		return false;
	}

	/** The first stop after this one that still has something to do. */
	public static RouteStep stepAfter(List<RouteItem> items, RouteItem current, Set<String> done)
	{
		boolean passed = false;
		for (RouteItem item : items)
		{
			if (item == current)
			{
				passed = true;
				continue;
			}
			if (passed && item.getStep() != current.getStep() && item.isDue() && !done.contains(item.getKey()))
			{
				return item.getStep();
			}
		}
		return null;
	}
}
