package com.treerunning.core;

import java.util.List;

/**
 * One finished run in a run history (tree or herb). Saved as JSON, so fields stay simple.
 */
public final class RunRecord
{
	/** When the run was finished (epoch millis), or 0 for runs saved by older versions. */
	public long endedAt;
	/** Farming XP gained on the run. */
	public int xp;
	/** Patches ticked on the run, or -1 if unknown. */
	public int patches = -1;
	/** When the run started, or 0 if unknown (older versions). */
	public long startedAt;
	/** What the harvest was worth at the time (herbs, fruit, produce). */
	public long value;
	/** What the seeds, saplings, compost and payments used cost at the time. */
	public long cost;
	/** False for runs saved before profit was tracked. */
	public boolean profitTracked;
	/** Herbs or crops picked (herb runs). */
	public int picked;

	public RunRecord()
	{
	}

	public RunRecord(long endedAt, int xp, int patches)
	{
		this.endedAt = endedAt;
		this.xp = xp;
		this.patches = patches;
	}

	public long profit()
	{
		return value - cost;
	}

	/** How long the run took, or 0 if unknown. */
	public long durationMs()
	{
		return startedAt > 0 && endedAt > startedAt ? endedAt - startedAt : 0;
	}

	/** The fastest run with this many patches, or 0 if none has a time. */
	public static long bestTime(List<RunRecord> runs, int patches)
	{
		long best = 0;
		for (RunRecord r : runs)
		{
			long d = r.durationMs();
			if (d > 0 && r.patches == patches && (best == 0 || d < best))
			{
				best = d;
			}
		}
		return best;
	}

	/** Profit over every run with profit tracked. */
	public static long lifetimeProfit(List<RunRecord> runs)
	{
		long total = 0;
		for (RunRecord r : runs)
		{
			if (r.profitTracked)
			{
				total += r.profit();
			}
		}
		return total;
	}

	/** The most recent run with profit tracked, or null. */
	public static RunRecord lastWithProfit(List<RunRecord> runs)
	{
		for (int i = runs.size() - 1; i >= 0; i--)
		{
			if (runs.get(i).profitTracked)
			{
				return runs.get(i);
			}
		}
		return null;
	}
}
