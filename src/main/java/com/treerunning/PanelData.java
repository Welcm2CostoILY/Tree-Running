package com.treerunning;

import com.treerunning.core.Route;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A snapshot of everything the side panel and overlay show. Built on the client thread and
 * never changed after it's published, so the Swing and render threads can read it safely.
 */
final class PanelData
{
	static final PanelData EMPTY = new PanelData();

	boolean loggedIn;
	int currentXp;
	int level;

	TreeRunningConfig.GoalType goalType = TreeRunningConfig.GoalType.LEVEL;
	int goalLevel = 99;
	int goalXp;

	double plannedXpPerRun;
	int plannedRuns = -1;
	double actualXpPerRun;
	int actualRunsCounted;
	/** Runs to the goal, or -1 if the plan can't reach it. */
	int runsLeft = -1;
	boolean runsLeftFromActual;
	int runsPerDay = 1;

	List<RunTask> tasks = Collections.emptyList();
	/** Patches and bank stops in the order you do them. */
	List<RouteItem> items = Collections.emptyList();
	Set<String> done = Collections.emptySet();
	boolean runActive;
	int runXpSoFar;
	/** The patch the last planting ticked, or null. */
	String lastMatch;

	List<RunNeeds.Line> needs = Collections.emptyList();
	List<SupplyCalculator.SupplyLine> supplies = Collections.emptyList();
	long bankSeenAt;
	long vaultSeenAt;

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

	/** The next patch or bank stop to do, or null when the run is finished. */
	RouteItem nextItem()
	{
		return Route.next(items, done);
	}
}
