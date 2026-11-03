package com.treerunning.data;

import net.runelite.api.gameval.NpcID;

/**
 * Herb patches, with the allotments next to them. Varbits, regions, gardeners and the
 * region exceptions come from RuneLite's Time Tracking plugin (FarmingWorld), which calls
 * Hosidius "Kourend", Port Phasmatys "Morytania" and Ortus Farm "Civitas illa Fortis".
 * Disease-free unlocks and yield bonuses come from the OSRS Wiki (Herb patch).
 */
public enum HerbLocation
{
	FARMING_GUILD("Farming Guild", 4775, new int[]{4773, 4774}, NpcID.FARMING_GARDENER_FARMGUILD_T1, new int[]{1, 2},
		null, Bonus.KOUREND_HARD, "65 Farming", "Jewellery box → Farming Guild", RouteStep.FARMING_GUILD,
		4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667),
	HOSIDIUS("Hosidius", 4774, new int[]{4771, 4772}, NpcID.HOSIDIUS_ALLOTMENT_GARDENER, new int[]{0, 1},
		"Easy Kourend & Kebos diary", Bonus.KOUREND_HARD, null, "Portal nexus → Kourend Castle, run south-west", null,
		6967, 6711),
	ORTUS_FARM("Ortus Farm", 4774, new int[]{4771, 4772}, NpcID.FORTIS_GARDENER, new int[]{0, 1},
		"Champion rank at the Fortis Colosseum", Bonus.NONE, null, "Civitas illa Fortis teleport, run to Ortus Farm", null,
		6192, 6447, 6448, 6449, 6191, 6193),
	CATHERBY("Catherby", 4774, new int[]{4771, 4772}, NpcID.DANTAERA, new int[]{0, 1},
		null, Bonus.KANDARIN, null, "Portal nexus → Catherby", RouteStep.CATHERBY,
		11062, 11061, 11318, 11317),
	ARDOUGNE("Ardougne", 4774, new int[]{4771, 4772}, NpcID.KRAGEN, new int[]{0, 1},
		null, Bonus.NONE, null, "Ardougne cloak → Ardougne farm", null,
		10548),
	FALADOR("Falador", 4774, new int[]{4771, 4772}, NpcID.ELSTAN, new int[]{0, 1},
		null, Bonus.NONE, null, "Explorer's ring → cabbage patch, run north", RouteStep.FALADOR,
		12083),
	PORT_PHASMATYS("Port Phasmatys", 4774, new int[]{4771, 4772}, NpcID.LYRA, new int[]{0, 1},
		null, Bonus.NONE, null, "Ectophial → Port Phasmatys, run west", null,
		14391, 14390),
	HARMONY_ISLAND("Harmony Island", 4772, new int[]{4771}, -1, new int[]{0},
		"Always", Bonus.NONE, "Elite Morytania diary", "Harmony Island teleport", null,
		15148),
	TROLL_STRONGHOLD("Troll Stronghold", 4771, new int[0], -1, new int[0],
		"My Arm's Big Adventure", Bonus.NONE, "My Arm's Big Adventure", "Stronghold teleport, climb to the roof", null,
		11321),
	WEISS("Weiss", 4771, new int[0], -1, new int[0],
		"Making Friends with My Arm", Bonus.NONE, "Making Friends with My Arm", "Icy basalt → Weiss", null,
		11325);

	/** Where an extra yield bonus comes from. */
	public enum Bonus
	{
		NONE,
		/** Catherby: 5%, 10% or 15% with the medium, hard or elite Kandarin diary. */
		KANDARIN,
		/** Hosidius and the Farming Guild: 5% with the hard Kourend & Kebos diary. */
		KOUREND_HARD
	}

	private final String displayName;
	private final int herbVarbit;
	private final int[] allotmentVarbits;
	private final int allotmentGardener;
	private final int[] allotmentOptions;
	private final String diseaseFree;
	private final Bonus bonus;
	private final String requirement;
	private final String travel;
	private final RouteStep treeStep;
	private final int[] regions;

	HerbLocation(String displayName, int herbVarbit, int[] allotmentVarbits, int allotmentGardener, int[] allotmentOptions,
		String diseaseFree, Bonus bonus, String requirement, String travel, RouteStep treeStep, int... regions)
	{
		this.displayName = displayName;
		this.herbVarbit = herbVarbit;
		this.allotmentVarbits = allotmentVarbits;
		this.allotmentGardener = allotmentGardener;
		this.allotmentOptions = allotmentOptions;
		this.diseaseFree = diseaseFree;
		this.bonus = bonus;
		this.requirement = requirement;
		this.travel = travel;
		this.treeStep = treeStep;
		this.regions = regions;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getHerbVarbit()
	{
		return herbVarbit;
	}

	/** The allotment varbits here, north/west patch first. Empty where there are none. */
	public int[] getAllotmentVarbits()
	{
		return allotmentVarbits.clone();
	}

	public int getAllotmentCount()
	{
		return allotmentVarbits.length;
	}

	/** The gardener who protects the allotments, or -1. */
	public int getAllotmentGardener()
	{
		return allotmentGardener;
	}

	/** Which option in the gardener's "which patch?" dialogue is this allotment (0-based). */
	public int getAllotmentOption(int index)
	{
		return allotmentOptions[index];
	}

	/** How this patch becomes disease-free, or null if it never does. */
	public String getDiseaseFree()
	{
		return diseaseFree;
	}

	public boolean isDiseaseFree()
	{
		return diseaseFree != null;
	}

	public Bonus getBonus()
	{
		return bonus;
	}

	/** What you need to use the patch at all, or null. */
	public String getRequirement()
	{
		return requirement;
	}

	public String getTravel()
	{
		return travel;
	}

	/** The tree-run stop this herb patch is next to, for "herbs on the way", or null. */
	public RouteStep getTreeStep()
	{
		return treeStep;
	}

	/** Names for allotment patches ("North", "South"), matching their order. */
	public String allotmentName(int index)
	{
		if (allotmentVarbits.length < 2)
		{
			return "Allotment";
		}
		switch (this)
		{
			case FALADOR:
			case PORT_PHASMATYS:
				return index == 0 ? "North-west allotment" : "South-east allotment";
			case HOSIDIUS:
				return index == 0 ? "North-east allotment" : "South-west allotment";
			default:
				return index == 0 ? "North allotment" : "South allotment";
		}
	}

	/** Whether this location's varbits are sent where you're standing. */
	public boolean isAt(int x, int y, int plane)
	{
		int region = ((x >> 6) << 8) | (y >> 6);
		boolean inRegion = false;
		for (int r : regions)
		{
			if (r == region)
			{
				inRegion = true;
				break;
			}
		}
		if (!inRegion)
		{
			return false;
		}
		switch (this)
		{
			case CATHERBY:
				if (x >= 2816 && y < 3456)
				{
					// Upstairs and the fruit tree area send different varbits.
					return x < 2840 && y >= 3440 && plane == 0;
				}
				return true;
			case FALADOR:
				// Not on the region boundary, because of the Port Sarim spirit tree patch.
				return y >= 3272;
			default:
				return true;
		}
	}
}
