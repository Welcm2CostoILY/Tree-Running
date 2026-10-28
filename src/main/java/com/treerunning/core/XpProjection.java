package com.treerunning.core;

import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates future runs to estimate how many runs reach the goal and how many of each
 * crop get planted along the way.
 *
 * Each run plants every enabled patch whose crop is unlocked at the current level. Crops
 * that take longer than the time between runs (hardwood, redwood, Hespori) are replanted
 * every N runs. XP is credited on the run it's planted, which matches the daily rhythm of
 * checking yesterday's tree and planting today's.
 */
public final class XpProjection
{
	/** Safety cap so a misconfigured plan can't loop forever. */
	public static final int MAX_RUNS = 5000;

	private final int runs;
	private final boolean reachesGoal;
	private final double averageXpPerRun;
	private final Map<CropType, Integer> plantings;

	private XpProjection(int runs, boolean reachesGoal, double averageXpPerRun, Map<CropType, Integer> plantings)
	{
		this.runs = runs;
		this.reachesGoal = reachesGoal;
		this.averageXpPerRun = averageXpPerRun;
		this.plantings = Collections.unmodifiableMap(plantings);
	}

	/** Runs needed to go from currentXp to the plan's goal using planned crop XP. */
	public static XpProjection toGoal(int currentXp, PlanSettings settings)
	{
		return simulate(currentXp, settings.getGoalXp(), settings, MAX_RUNS);
	}

	/** What a fixed number of runs plants (used when runs come from real run history). */
	public static XpProjection forRuns(int currentXp, int runs, PlanSettings settings)
	{
		return simulate(currentXp, Integer.MAX_VALUE, settings, runs);
	}

	private static XpProjection simulate(int currentXp, int goalXp, PlanSettings settings, int maxRuns)
	{
		Map<CropType, Integer> plantings = new EnumMap<>(CropType.class);
		Map<PatchLocation, Integer> lastPlantedRun = new HashMap<>();
		double xp = currentXp;
		int run = 0;

		while (xp < goalXp && run < maxRuns)
		{
			run++;
			int level = XpTable.levelForXp((int) xp);
			double gained = 0;

			for (PatchLocation location : PatchLocation.values())
			{
				if (!settings.isEnabled(location))
				{
					continue;
				}
				CropType crop = settings.cropFor(location.getPatchType());
				if (crop.getLevel() > level)
				{
					continue;
				}
				Integer last = lastPlantedRun.get(location);
				if (last != null && run - last < crop.getIntervalRuns(settings.getRunsPerDay()))
				{
					continue;
				}
				gained += crop.getXp() * location.getPatchCount();
				plantings.merge(crop, location.getPatchCount(), Integer::sum);
				lastPlantedRun.put(location, run);
			}

			if (gained <= 0)
			{
				// Nothing can be planted at this level, so the goal is unreachable.
				break;
			}
			xp += gained;
		}

		boolean reached = xp >= goalXp;
		double average = run == 0 ? 0 : (Math.min(xp, goalXp) - currentXp) / run;
		return new XpProjection(run, reached, average, plantings);
	}

	public int getRuns()
	{
		return runs;
	}

	public boolean reachesGoal()
	{
		return reachesGoal;
	}

	public double getAverageXpPerRun()
	{
		return averageXpPerRun;
	}

	public Map<CropType, Integer> getPlantings()
	{
		return plantings;
	}

	/** Average XP of the most recent completed runs (0 if there are none). */
	public static double averageOfRecent(List<Integer> runXp, int count)
	{
		if (runXp.isEmpty() || count <= 0)
		{
			return 0;
		}
		int from = Math.max(0, runXp.size() - count);
		long sum = 0;
		int n = 0;
		for (int i = from; i < runXp.size(); i++)
		{
			sum += runXp.get(i);
			n++;
		}
		return n == 0 ? 0 : (double) sum / n;
	}

	/** Runs needed at a given XP per run (0 if the goal is already reached). */
	public static int runsAtRate(int currentXp, int goalXp, double xpPerRun)
	{
		if (currentXp >= goalXp)
		{
			return 0;
		}
		if (xpPerRun <= 0)
		{
			return -1;
		}
		return (int) Math.ceil((goalXp - currentXp) / xpPerRun);
	}
}
