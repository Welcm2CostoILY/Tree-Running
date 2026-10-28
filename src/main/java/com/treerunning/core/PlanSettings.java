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
 * What the player has chosen: which crop goes in each patch type, which patches they
 * use, their goal, and how they want their inventory planned.
 */
public final class PlanSettings
{
	private final Map<PatchType, CropType> crops;
	private final Set<PatchLocation> enabled;
	private final int goalLevel;
	private final int runsPerDay;
	private final int reservedSlots;
	private final boolean pickProduce;

	public PlanSettings(Map<PatchType, CropType> crops, Set<PatchLocation> enabled, int goalLevel,
		int runsPerDay, int reservedSlots, boolean pickProduce)
	{
		this.crops = Collections.unmodifiableMap(new EnumMap<>(crops));
		this.enabled = enabled.isEmpty()
			? Collections.unmodifiableSet(EnumSet.noneOf(PatchLocation.class))
			: Collections.unmodifiableSet(EnumSet.copyOf(enabled));
		this.goalLevel = goalLevel;
		this.runsPerDay = Math.max(1, runsPerDay);
		this.reservedSlots = Math.max(0, reservedSlots);
		this.pickProduce = pickProduce;
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

	public int getGoalLevel()
	{
		return goalLevel;
	}

	public int getGoalXp()
	{
		return XpTable.xpForLevel(goalLevel);
	}

	public int getRunsPerDay()
	{
		return runsPerDay;
	}

	public int getReservedSlots()
	{
		return reservedSlots;
	}

	public boolean isPickProduce()
	{
		return pickProduce;
	}
}
