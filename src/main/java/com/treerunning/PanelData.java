package com.treerunning;

import com.treerunning.core.HerbPlanner;
import com.treerunning.core.HerbTask;
import com.treerunning.core.InventoryFit;
import com.treerunning.core.Route;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunRecord;
import com.treerunning.core.RunTask;
import com.treerunning.core.SeenPatch;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.data.Compost;
import com.treerunning.data.RouteStep;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A snapshot of everything the side panel and overlays show. Built on the client thread and
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
	/** Tree runs to the goal, or -1 if the plan can't reach it. */
	int runsLeft = -1;
	boolean runsLeftFromActual;
	int runsPerDay = 1;

	// Tree run
	List<RunTask> tasks = Collections.emptyList();
	List<RouteItem> items = Collections.emptyList();
	Set<String> done = Collections.emptySet();
	boolean runActive;
	long runStartedAt;
	int runXpSoFar;
	String lastMatch;
	List<RunNeeds.Line> needs = Collections.emptyList();
	/** Inventory slots the rest of the run needs, and the first stretch that won't fit (or null). */
	int slots;
	InventoryFit.Overflow overflow;
	/** What each patch looked like when last seen, by key. */
	Map<String, SeenPatch> seen = Collections.emptyMap();
	Set<String> paid = Collections.emptySet();
	/** Ticked patches whose gardener hasn't been paid. */
	Set<String> unpaid = Collections.emptySet();
	boolean highlightGardeners = true;
	/** Grown herb patches next to tree-run stops ("herbs on the way"); empty when that's off. */
	Map<RouteStep, List<HerbTask>> onTheWay = Collections.emptyMap();

	List<SupplyCalculator.SupplyLine> supplies = Collections.emptyList();
	long bankSeenAt;
	long vaultSeenAt;
	List<RunRecord> runs = Collections.emptyList();
	Map<RouteStep, String> travel = Collections.emptyMap();
	TreeRunningConfig.BankFilter bankFilter = TreeRunningConfig.BankFilter.OFF;
	/** Changes when the bank filter's items change. */
	int tagKey;

	// Herb run
	List<HerbTask> herbTasks = Collections.emptyList();
	Set<String> herbDone = Collections.emptySet();
	boolean herbActive;
	long herbStartedAt;
	List<HerbPlanner.Need> herbNeeds = Collections.emptyList();
	Compost compost = Compost.BOTTOMLESS;
	boolean compostCarried = true;
	boolean secateursExpected;
	boolean secateursCarried;
	int herbPicked;
	long herbValue;
	long herbCost;
	int herbXp;
	List<RunRecord> herbRuns = Collections.emptyList();
	String herbLastMatch;
	List<HerbPlanner.Estimate> picker = Collections.emptyList();
	/** Estimated profit per herb patch, by key. */
	Map<String, Long> herbEstimates = Collections.emptyMap();
	double herbXpPerRun;
	int herbRunsPerDay;

	// Profit
	long lifetimeTreeProfit;
	long lifetimeHerbProfit;
	RunRecord lastTreeRun;
	RunRecord lastHerbRun;

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

	RouteItem nextItem()
	{
		return Route.next(items, done);
	}

	String travelNote(RouteStep step)
	{
		String note = travel.get(step);
		return note != null ? note : step.getTravel();
	}

	/** Days to the goal with tree and herb runs together, or -1. */
	int daysLeft()
	{
		double treePerRun = actualXpPerRun > 0 ? actualXpPerRun : plannedXpPerRun;
		double perDay = treePerRun * runsPerDay + herbXpPerRun * herbRunsPerDay;
		if (perDay <= 0)
		{
			return -1;
		}
		return (int) Math.ceil(xpRemaining() / perDay);
	}

	int herbDueCount()
	{
		int n = 0;
		for (HerbTask t : herbTasks)
		{
			if (t.isDue())
			{
				n++;
			}
		}
		return n;
	}

	int herbPlantedCount()
	{
		int n = 0;
		for (HerbTask t : herbTasks)
		{
			if (t.isDue() && herbDone.contains(t.stepKey(HerbTask.PLANT)))
			{
				n++;
			}
		}
		return n;
	}
}
