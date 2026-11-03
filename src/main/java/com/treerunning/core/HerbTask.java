package com.treerunning.core;

import com.treerunning.data.AllotmentType;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchCondition;
import java.util.Collections;
import java.util.List;

/** One herb or allotment patch on a herb run, with the steps to do there. */
public final class HerbTask
{
	public static final String HARVEST = "harvest";
	public static final String CURE = "cure";
	public static final String CLEAR = "clear";
	public static final String COMPOST = "compost";
	public static final String PLANT = "plant";
	public static final String PAY = "pay";

	/** A step at a patch: harvest, compost, plant and so on. */
	public static final class Step
	{
		private final String id;
		private final String label;

		Step(String id, String label)
		{
			this.id = id;
			this.label = label;
		}

		public String getId()
		{
			return id;
		}

		public String getLabel()
		{
			return label;
		}
	}

	private final HerbLocation location;
	private final int allotment;
	private final HerbType herb;
	private final AllotmentType allotmentCrop;
	private final PatchCondition condition;
	private final String growingCrop;
	private final long readyAt;
	private final boolean seen;
	private final List<Step> steps;

	HerbTask(HerbLocation location, int allotment, HerbType herb, AllotmentType allotmentCrop, PatchCondition condition,
		String growingCrop, long readyAt, boolean seen, List<Step> steps)
	{
		this.location = location;
		this.allotment = allotment;
		this.herb = herb;
		this.allotmentCrop = allotmentCrop;
		this.condition = condition;
		this.growingCrop = growingCrop;
		this.readyAt = readyAt;
		this.seen = seen;
		this.steps = Collections.unmodifiableList(steps);
	}

	public static String keyFor(HerbLocation location, int allotment)
	{
		return allotment < 0 ? "herb:" + location.name() : "allot:" + location.name() + ":" + allotment;
	}

	public static String stepKey(String taskKey, String step)
	{
		return taskKey + "|" + step;
	}

	public String getKey()
	{
		return keyFor(location, allotment);
	}

	public String stepKey(String step)
	{
		return stepKey(getKey(), step);
	}

	public HerbLocation getLocation()
	{
		return location;
	}

	public boolean isAllotment()
	{
		return allotment >= 0;
	}

	/** Which allotment at the location (0 or 1), or -1 for the herb patch. */
	public int getAllotmentIndex()
	{
		return allotment;
	}

	/** The herb to plant, or null for an allotment. */
	public HerbType getHerb()
	{
		return herb;
	}

	/** The allotment crop to plant, or null for a herb patch. */
	public AllotmentType getAllotmentCrop()
	{
		return allotmentCrop;
	}

	public String getPlantName()
	{
		return herb != null ? herb.getDisplayName() : allotmentCrop.getDisplayName();
	}

	public String getLabel()
	{
		return isAllotment() ? location.allotmentName(allotment) : "Herb patch";
	}

	/** What the patch most likely looks like now. */
	public PatchCondition getCondition()
	{
		return condition;
	}

	/** What's in the patch, from when it was last seen, or null. */
	public String getGrowingCrop()
	{
		return growingCrop;
	}

	/** When it's grown at the latest, if growing; 0 otherwise. */
	public long getReadyAt()
	{
		return readyAt;
	}

	/** False until the patch has been seen in game. */
	public boolean isSeen()
	{
		return seen;
	}

	/** Whether there's something to do here now. */
	public boolean isDue()
	{
		return condition != PatchCondition.GROWING;
	}

	public List<Step> getSteps()
	{
		return steps;
	}
}
