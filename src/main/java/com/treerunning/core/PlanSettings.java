package com.treerunning.core;

import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * What the player has chosen: which crop goes in each patch type, which patches they use,
 * their goal (always stored as an XP total) and how many runs they do a day.
 */
public final class PlanSettings
{
	private final Map<PatchType, CropType> crops;
	private final Set<PatchLocation> enabled;
	private final int goalXp;
	private final int runsPerDay;

	public PlanSettings(Map<PatchType, CropType> crops, Set<PatchLocation> enabled, int goalXp, int runsPerDay)
	{
		this.crops = Collections.unmodifiableMap(new EnumMap<>(crops));
		this.enabled = enabled.isEmpty()
			? Collections.unmodifiableSet(EnumSet.noneOf(PatchLocation.class))
			: Collections.unmodifiableSet(EnumSet.copyOf(enabled));
		this.goalXp = Math.max(1, goalXp);
		this.runsPerDay = Math.max(1, runsPerDay);
	}

	/** The crop for a patch type, or null if that patch type is switched off. */
	public CropType cropFor(PatchType type)
	{
		return crops.get(type);
	}

	public boolean isEnabled(PatchLocation location)
	{
		return enabled.contains(location) && crops.get(location.getPatchType()) != null;
	}

	public int getGoalXp()
	{
		return goalXp;
	}

	public int getRunsPerDay()
	{
		return runsPerDay;
	}
}
