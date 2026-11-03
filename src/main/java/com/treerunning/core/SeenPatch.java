package com.treerunning.core;

import com.treerunning.data.PatchCondition;

/** The last time a patch was seen in game, and what it looked like. Saved per account. */
public final class SeenPatch
{
	public PatchCondition condition = PatchCondition.UNKNOWN;
	public int stage;
	public int stages;
	public int minutesPerStage;
	public String crop;
	public long seenAt;

	public SeenPatch()
	{
	}

	public SeenPatch(PatchCondition condition, int stage, int stages, int minutesPerStage, String crop, long seenAt)
	{
		this.condition = condition;
		this.stage = stage;
		this.stages = stages;
		this.minutesPerStage = minutesPerStage;
		this.crop = crop;
		this.seenAt = seenAt;
	}

	/** When it'll be grown at the latest, if it was growing; 0 otherwise. */
	public long readyBy()
	{
		if (condition != PatchCondition.GROWING || stages == 0)
		{
			return 0;
		}
		return seenAt + Math.max(0, stages - 1 - stage) * (long) minutesPerStage * 60_000L;
	}

	/** What it most likely looks like now: a growing patch becomes grown once its time is up. */
	public PatchCondition predict(long now)
	{
		if (condition == PatchCondition.GROWING && now >= readyBy())
		{
			return PatchCondition.GROWN;
		}
		return condition;
	}

	public boolean isWeeds()
	{
		return crop == null || "Weeds".equals(crop);
	}
}
