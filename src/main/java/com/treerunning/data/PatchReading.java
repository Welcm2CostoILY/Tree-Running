package com.treerunning.data;

/** One decoded patch varbit: its condition, growth stage and crop. */
public final class PatchReading
{
	public static final PatchReading UNKNOWN = new PatchReading(PatchCondition.UNKNOWN, 0, 0, 0, "");

	private final PatchCondition condition;
	private final int stage;
	private final int stages;
	private final int minutesPerStage;
	private final String crop;

	PatchReading(PatchCondition condition, int stage, int stages, int minutesPerStage, String crop)
	{
		this.condition = condition;
		this.stage = stage;
		this.stages = stages;
		this.minutesPerStage = minutesPerStage;
		this.crop = crop;
	}

	public PatchCondition getCondition()
	{
		return condition;
	}

	public int getStage()
	{
		return stage;
	}

	public int getStages()
	{
		return stages;
	}

	public int getMinutesPerStage()
	{
		return minutesPerStage;
	}

	/** The crop's name as the game calls it ("Yew", "Ranarr", "Weeds"). */
	public String getCrop()
	{
		return crop;
	}

	public boolean isWeeds()
	{
		return "Weeds".equals(crop);
	}

	/** At most this many minutes until it's grown, or 0 if it isn't growing. */
	public int maxMinutesLeft()
	{
		if (condition != PatchCondition.GROWING || stages == 0)
		{
			return 0;
		}
		return Math.max(0, stages - 1 - stage) * minutesPerStage;
	}
}
