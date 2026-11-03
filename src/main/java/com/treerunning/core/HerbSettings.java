package com.treerunning.core;

import com.treerunning.data.AllotmentType;
import com.treerunning.data.Compost;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Everything the herb planner needs from the settings. */
public final class HerbSettings
{
	public final Map<HerbLocation, HerbType> herbs;
	public final Set<HerbLocation> enabled;
	public final Set<HerbLocation> allotments;
	public final AllotmentType allotmentCrop;
	public final boolean payForAllotments;
	public final Compost compost;
	public final int farmingLevel;
	/** Magic secateurs (+10%) and a farming cape (+5%), as fractions added together. */
	public final double itemBonus;
	/** 0, 0.05, 0.10 or 0.15 at Catherby from the Kandarin diary. */
	public final double kandarinBonus;
	public final boolean kourendHard;

	public HerbSettings(Map<HerbLocation, HerbType> herbs, Set<HerbLocation> enabled, Set<HerbLocation> allotments,
		AllotmentType allotmentCrop, boolean payForAllotments, Compost compost, int farmingLevel, double itemBonus,
		double kandarinBonus, boolean kourendHard)
	{
		this.herbs = Collections.unmodifiableMap(new EnumMap<>(herbs.isEmpty() ? new EnumMap<>(HerbLocation.class) : herbs));
		this.enabled = Collections.unmodifiableSet(enabled.isEmpty() ? EnumSet.noneOf(HerbLocation.class) : EnumSet.copyOf(enabled));
		this.allotments = Collections.unmodifiableSet(allotments.isEmpty() ? EnumSet.noneOf(HerbLocation.class) : EnumSet.copyOf(allotments));
		this.allotmentCrop = allotmentCrop;
		this.payForAllotments = payForAllotments;
		this.compost = compost;
		this.farmingLevel = farmingLevel;
		this.itemBonus = itemBonus;
		this.kandarinBonus = kandarinBonus;
		this.kourendHard = kourendHard;
	}

	public HerbType herbAt(HerbLocation location)
	{
		HerbType h = herbs.get(location);
		return h != null ? h : HerbType.RANARR;
	}

	/** The location's extra yield, from diaries. */
	public double patchBonus(HerbLocation location)
	{
		switch (location.getBonus())
		{
			case KANDARIN:
				return kandarinBonus;
			case KOUREND_HARD:
				return kourendHard ? 0.05 : 0;
			default:
				return 0;
		}
	}
}
