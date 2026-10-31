package com.treerunning.core;

/**
 * One finished run in the run history. Saved as JSON, so fields stay simple.
 */
public final class RunRecord
{
	/** When the run was finished (epoch millis), or 0 for runs saved by older versions. */
	public long endedAt;
	/** Farming XP gained on the run. */
	public int xp;
	/** Patches ticked on the run, or -1 if unknown. */
	public int patches = -1;

	public RunRecord()
	{
	}

	public RunRecord(long endedAt, int xp, int patches)
	{
		this.endedAt = endedAt;
		this.xp = xp;
		this.patches = patches;
	}
}
