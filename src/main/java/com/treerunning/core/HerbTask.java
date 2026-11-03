package com.treerunning.core;

import com.treerunning.data.AllotmentType;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchCondition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * One entry on a herb run: a location's herb patch, or all of its allotments together.
 * Steps are tracked per patch underneath ("allot:HOSIDIUS:0|plant"), and a step on the
 * allotment entry counts as done once every allotment there has done it.
 */
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
	private final boolean allotments;
	private final HerbType herb;
	private final AllotmentType allotmentCrop;
	private final PatchCondition condition;
	private final long readyAt;
	private final boolean seen;
	private final List<String> patchKeys;
	private final List<Step> steps;

	HerbTask(HerbLocation location, boolean allotments, HerbType herb, AllotmentType allotmentCrop, PatchCondition condition,
		long readyAt, boolean seen, List<String> patchKeys, List<Step> steps)
	{
		this.location = location;
		this.allotments = allotments;
		this.herb = herb;
		this.allotmentCrop = allotmentCrop;
		this.condition = condition;
		this.readyAt = readyAt;
		this.seen = seen;
		this.patchKeys = Collections.unmodifiableList(new ArrayList<>(patchKeys));
		this.steps = Collections.unmodifiableList(steps);
	}

	/** The key of one patch: "herb:FALADOR", or "allot:FALADOR:0" for an allotment. */
	public static String keyFor(HerbLocation location, int allotment)
	{
		return allotment < 0 ? "herb:" + location.name() : "allot:" + location.name() + ":" + allotment;
	}

	/** The key of a location's allotments together, as one entry. */
	public static String allotmentsKey(HerbLocation location)
	{
		return "allot:" + location.name();
	}

	public static String stepKey(String patchKey, String step)
	{
		return patchKey + "|" + step;
	}

	public String getKey()
	{
		return allotments ? allotmentsKey(location) : keyFor(location, -1);
	}

	/** The patches this entry covers: one herb patch, or every allotment at the location. */
	public List<String> getPatchKeys()
	{
		return patchKeys;
	}

	/** The per-patch step keys for a step. */
	public List<String> stepKeys(String step)
	{
		List<String> keys = new ArrayList<>(patchKeys.size());
		for (String k : patchKeys)
		{
			keys.add(stepKey(k, step));
		}
		return keys;
	}

	/** Done once every patch here has done it. */
	public boolean isDone(Set<String> done, String step)
	{
		for (String k : patchKeys)
		{
			if (!done.contains(stepKey(k, step)))
			{
				return false;
			}
		}
		return true;
	}

	/** How many of the patches here have done it. */
	public int doneCount(Set<String> done, String step)
	{
		int n = 0;
		for (String k : patchKeys)
		{
			if (done.contains(stepKey(k, step)))
			{
				n++;
			}
		}
		return n;
	}

	public HerbLocation getLocation()
	{
		return location;
	}

	public boolean isAllotment()
	{
		return allotments;
	}

	/** How many patches this entry covers (2 for most allotment pairs). */
	public int getPatchCount()
	{
		return patchKeys.size();
	}

	/** The herb to plant, or null for allotments. */
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
		return allotments ? (patchKeys.size() > 1 ? "Allotments" : "Allotment") : "Herb patch";
	}

	/** What the patch (or the allotments, taken together) most likely looks like now. */
	public PatchCondition getCondition()
	{
		return condition;
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
