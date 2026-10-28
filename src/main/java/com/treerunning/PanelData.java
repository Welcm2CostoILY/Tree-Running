package com.treerunning;

import com.treerunning.core.InventoryPlanner;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An immutable snapshot of everything the side panel and overlay show. Built on the
 * client thread, read on the Swing and render threads.
 */
final class PanelData
{
	static final PanelData EMPTY = new PanelData(false, 0, 0, 99, 0, 0, 0, 0, 0, 0, false, 0, Collections.emptyList(),
		Collections.emptySet(), false, 0, Collections.emptyList(), 0, null, null);

	final boolean loggedIn;
	final int currentXp;
	final int level;
	final int goalLevel;
	final int goalXp;
	final double plannedXpPerRun;
	final int plannedRuns;
	final double actualXpPerRun;
	final int actualRunsCounted;
	final int runsLeft;
	final boolean runsLeftFromActual;
	final int runsPerDay;
	final List<RunTask> tasks;
	final Set<String> done;
	final boolean runActive;
	final int runXpSoFar;
	final List<SupplyCalculator.SupplyLine> supplies;
	final long bankSeenAt;
	final InventoryPlanner.Plan inventoryPlan;
	/** The patch the last planting ticked, or null. */
	final String lastMatch;

	PanelData(boolean loggedIn, int currentXp, int level, int goalLevel, int goalXp, double plannedXpPerRun, int plannedRuns,
		double actualXpPerRun, int actualRunsCounted, int runsLeft, boolean runsLeftFromActual, int runsPerDay,
		List<RunTask> tasks, Set<String> done, boolean runActive, int runXpSoFar, List<SupplyCalculator.SupplyLine> supplies,
		long bankSeenAt, InventoryPlanner.Plan inventoryPlan, String lastMatch)
	{
		this.loggedIn = loggedIn;
		this.currentXp = currentXp;
		this.level = level;
		this.goalLevel = goalLevel;
		this.goalXp = goalXp;
		this.plannedXpPerRun = plannedXpPerRun;
		this.plannedRuns = plannedRuns;
		this.actualXpPerRun = actualXpPerRun;
		this.actualRunsCounted = actualRunsCounted;
		this.runsLeft = runsLeft;
		this.runsLeftFromActual = runsLeftFromActual;
		this.runsPerDay = runsPerDay;
		this.tasks = tasks;
		this.done = done;
		this.runActive = runActive;
		this.runXpSoFar = runXpSoFar;
		this.supplies = supplies;
		this.bankSeenAt = bankSeenAt;
		this.inventoryPlan = inventoryPlan;
		this.lastMatch = lastMatch;
	}

	int xpRemaining()
	{
		return Math.max(0, goalXp - currentXp);
	}

	int dueCount()
	{
		int n = 0;
		for (RunTask t : tasks)
		{
			if (t.isDue())
			{
				n++;
			}
		}
		return n;
	}

	int doneCount()
	{
		int n = 0;
		for (RunTask t : tasks)
		{
			if (t.isDue() && done.contains(t.getKey()))
			{
				n++;
			}
		}
		return n;
	}

	/** The first due task not yet planted, or null when the run is finished. */
	RunTask nextTask()
	{
		for (RunTask t : tasks)
		{
			if (t.isDue() && !done.contains(t.getKey()))
			{
				return t;
			}
		}
		return null;
	}
}
