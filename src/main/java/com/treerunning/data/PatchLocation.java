package com.treerunning.data;

import net.runelite.api.gameval.NpcID;

/**
 * Every patch the planner can use: its stop, requirements, the map regions it sits in and
 * the varbit that changes when it's planted.
 *
 * Regions and varbits come from RuneLite's built-in Time Tracking plugin (FarmingWorld),
 * which calls Nemus Retreat "Auburnvale", Locus Oasis "Avium Savannah" and the Summer
 * Shore "Great Conch". The same varbit IDs are reused in different regions, so a patch is
 * identified by the varbit and the region you're standing in together.
 * Requirements come from the OSRS Wiki patch location pages.
 */
public enum PatchLocation
{
	// Tree patches
	TREE_FARMING_GUILD(PatchType.TREE, RouteStep.FARMING_GUILD, null, "65 Farming", 7905, Regions.FARMING_GUILD),
	TREE_GNOME_STRONGHOLD(PatchType.TREE, RouteStep.GNOME_STRONGHOLD, null, null, 4771, Regions.GNOME_STRONGHOLD),
	TREE_FALADOR(PatchType.TREE, RouteStep.FALADOR, null, null, 4771, 11828, 12084),
	TREE_TAVERLEY(PatchType.TREE, RouteStep.TAVERLEY, null, null, 4771, 11573, 11829),
	TREE_VARROCK(PatchType.TREE, RouteStep.VARROCK, null, null, 4771, 12854, 12853),
	TREE_LUMBRIDGE(PatchType.TREE, RouteStep.LUMBRIDGE, null, null, 4771, 12594, 12850),
	TREE_NEMUS_RETREAT(PatchType.TREE, RouteStep.NEMUS_RETREAT, null, "Children of the Sun", 4771, 5427, 5428, 5684),

	// Fruit tree patches
	FRUIT_FARMING_GUILD(PatchType.FRUIT_TREE, RouteStep.FARMING_GUILD, null, "85 Farming", 7909, Regions.FARMING_GUILD),
	FRUIT_GNOME_STRONGHOLD(PatchType.FRUIT_TREE, RouteStep.GNOME_STRONGHOLD, null, null, 4772, Regions.GNOME_STRONGHOLD),
	FRUIT_TREE_GNOME_VILLAGE(PatchType.FRUIT_TREE, RouteStep.TREE_GNOME_VILLAGE, null, "Tree Gnome Village (for the spirit tree)", 4771, 9777, 10033),
	FRUIT_CATHERBY(PatchType.FRUIT_TREE, RouteStep.CATHERBY, null, null, 4771, 11317),
	FRUIT_BRIMHAVEN(PatchType.FRUIT_TREE, RouteStep.BRIMHAVEN, null, null, 4771, 11058, 11057),
	FRUIT_KASTORI(PatchType.FRUIT_TREE, RouteStep.KASTORI, null, "Children of the Sun", 4772, Regions.KASTORI),
	FRUIT_LLETYA(PatchType.FRUIT_TREE, RouteStep.LLETYA, null, "Mourning's End Part I (partial)", 4771, 9265, 11103),

	// Calquat patches
	CALQUAT_TAI_BWO_WANNAI(PatchType.CALQUAT, RouteStep.TAI_BWO_WANNAI, null, null, 4771, 11056),
	CALQUAT_KASTORI(PatchType.CALQUAT, RouteStep.KASTORI, null, "Children of the Sun", 4771, Regions.KASTORI),
	CALQUAT_SUMMER_SHORE(PatchType.CALQUAT, RouteStep.SUMMER_SHORE, null, "Troubled Tortugans (partial); 50 Agility via fairy ring", 4773,
		12581, 12325, 12326, 12327, 12580, 12582, 12583, 12836, 12837, 12838, 12839, 13092, 13093, 13194),

	// Farming Guild specials
	CELASTRUS_FARMING_GUILD(PatchType.CELASTRUS, RouteStep.FARMING_GUILD, null, "85 Farming", 7910, Regions.FARMING_GUILD),
	REDWOOD_FARMING_GUILD(PatchType.REDWOOD, RouteStep.FARMING_GUILD, null, "90 Farming", 7907, Regions.FARMING_GUILD),
	HESPORI_FARMING_GUILD(PatchType.HESPORI, RouteStep.FARMING_GUILD, null, "65 Farming", 7908, 5021),

	// Hardwood patches
	HARDWOOD_LOCUS_OASIS(PatchType.HARDWOOD, RouteStep.LOCUS_OASIS, null, "The Ribbiting Tale of a Lily Pad Labour Dispute", 4771, 6702, 6446),
	HARDWOOD_ANGLERS_RETREAT(PatchType.HARDWOOD, RouteStep.ANGLERS_RETREAT, null, "51 Sailing (rowboat route: build it with 50 Construction)", 4771, 9770),
	HARDWOOD_FOSSIL_ISLAND_EAST(PatchType.HARDWOOD, RouteStep.FOSSIL_ISLAND, "East", "Bone Voyage", 4771, Regions.FOSSIL_ISLAND),
	HARDWOOD_FOSSIL_ISLAND_MIDDLE(PatchType.HARDWOOD, RouteStep.FOSSIL_ISLAND, "Middle", "Bone Voyage", 4772, Regions.FOSSIL_ISLAND),
	HARDWOOD_FOSSIL_ISLAND_WEST(PatchType.HARDWOOD, RouteStep.FOSSIL_ISLAND, "West", "Bone Voyage", 4773, Regions.FOSSIL_ISLAND),
	// Off by default: crystal shards are untradeable and most runs skip it.
	CRYSTAL_PRIFDDINAS(PatchType.CRYSTAL, RouteStep.PRIFDDINAS, null, "Song of the Elves", 4775,
		13151, 12895, 12894, 13150);

	/** Region groups shared by several patches. */
	private static final class Regions
	{
		static final int[] FARMING_GUILD = {4922, 5177, 5178, 5179, 4921, 4923, 4665, 4666, 4667};
		static final int[] GNOME_STRONGHOLD = {9781, 9782, 9526, 9525};
		static final int[] KASTORI = {5423, 5167, 5424};
		static final int[] FOSSIL_ISLAND = {14651, 14907, 14908, 15164, 14652, 14906, 14650, 15162, 15163};
	}

	private final PatchType patchType;
	private final RouteStep step;
	private final String patchName;
	private final String requirement;
	private final int varbit;
	private final int[] regions;

	PatchLocation(PatchType patchType, RouteStep step, String patchName, String requirement, int varbit, int... regions)
	{
		this.patchType = patchType;
		this.step = step;
		this.patchName = patchName;
		this.requirement = requirement;
		this.varbit = varbit;
		this.regions = regions;
	}

	public PatchType getPatchType()
	{
		return patchType;
	}

	public RouteStep getStep()
	{
		return step;
	}

	/** Every location is a single patch (Fossil Island's three are listed separately). */
	public int getPatchCount()
	{
		return 1;
	}

	/** Which patch at a stop with several of the same type ("East"), or null. */
	public String getPatchName()
	{
		return patchName;
	}

	/** Quest or skill requirement, or null if none. */
	public String getRequirement()
	{
		return requirement;
	}

	/** The varbit holding this patch's state while you're in one of its regions. */
	public int getVarbit()
	{
		return varbit;
	}

	/** How this patch's varbit is read. */
	public PatchKind getKind()
	{
		switch (patchType)
		{
			case FRUIT_TREE:
				return PatchKind.FRUIT_TREE;
			case HARDWOOD:
				return PatchKind.HARDWOOD_TREE;
			case CALQUAT:
				return PatchKind.CALQUAT;
			case CELASTRUS:
				return PatchKind.CELASTRUS;
			case REDWOOD:
				return PatchKind.REDWOOD;
			case HESPORI:
				return PatchKind.HESPORI;
			case CRYSTAL:
				return PatchKind.CRYSTAL_TREE;
			default:
				return PatchKind.TREE;
		}
	}

	/** Patches that are off until you turn them on. */
	public boolean isOffByDefault()
	{
		return this == CRYSTAL_PRIFDDINAS;
	}

	/**
	 * The gardener you pay to protect this patch, or -1 if it has none (Hespori, crystal tree).
	 * NPC IDs come from RuneLite's FarmingWorld.
	 */
	public int getGardener()
	{
		switch (this)
		{
			case TREE_FARMING_GUILD:
				return NpcID.FARMING_GARDENER_FARMGUILD_T2;
			case TREE_GNOME_STRONGHOLD:
				return NpcID.FARMING_GARDENER_TREE_GNOME;
			case TREE_FALADOR:
				return NpcID.FARMING_GARDENER_TREE_2;
			case TREE_TAVERLEY:
				return NpcID.FARMING_GARDENER_TREE_1;
			case TREE_VARROCK:
				return NpcID.FARMING_GARDENER_TREE_3_02;
			case TREE_LUMBRIDGE:
				return NpcID.FARMING_GARDENER_TREE_4;
			case TREE_NEMUS_RETREAT:
				return NpcID.FARMING_GARDENER_TREE_7;
			case FRUIT_FARMING_GUILD:
				return NpcID.FARMING_GARDENER_FARMGUILD_T3;
			case FRUIT_GNOME_STRONGHOLD:
				return NpcID.FARMING_GARDENER_FRUIT_1;
			case FRUIT_TREE_GNOME_VILLAGE:
				return NpcID.FARMING_GARDENER_FRUIT_2;
			case FRUIT_CATHERBY:
				return NpcID.FARMING_GARDENER_FRUIT_4;
			case FRUIT_BRIMHAVEN:
				return NpcID.GARTH;
			case FRUIT_KASTORI:
				return NpcID.FARMING_GARDENER_FRUIT_7;
			case FRUIT_LLETYA:
				return NpcID.FARMING_GARDENER_FRUIT_TREE_5;
			case CALQUAT_TAI_BWO_WANNAI:
				return NpcID.FARMING_GARDENER_CALQUAT;
			case CALQUAT_KASTORI:
				return NpcID.FARMING_GARDENER_CALQUAT_2;
			case CALQUAT_SUMMER_SHORE:
				return NpcID.FARMING_GARDENER_CALQUAT_3;
			case CELASTRUS_FARMING_GUILD:
				return NpcID.FARMING_GARDENER_FARMGUILD_CELASTRUS;
			case REDWOOD_FARMING_GUILD:
				return NpcID.FARMING_GARDENER_FARMGUILD_REDWOOD;
			case HARDWOOD_LOCUS_OASIS:
				return NpcID.FROG_QUEST_MARCELLUS;
			case HARDWOOD_ANGLERS_RETREAT:
				return NpcID.FARMING_GARDENER_HARDWOOD_TREE_5;
			case HARDWOOD_FOSSIL_ISLAND_EAST:
				return NpcID.FOSSIL_SQUIRREL_GARDENER1;
			case HARDWOOD_FOSSIL_ISLAND_MIDDLE:
				return NpcID.FOSSIL_SQUIRREL_GARDENER2;
			case HARDWOOD_FOSSIL_ISLAND_WEST:
				return NpcID.FOSSIL_SQUIRREL_GARDENER3;
			default:
				return -1;
		}
	}

	/**
	 * Whether this patch's varbit is sent where you're standing. Mostly the region, with
	 * RuneLite's exceptions: part of Catherby's region sends the herb farm's varbits, and
	 * Fossil Island's hardwood varbits are only sent on the ground floor.
	 */
	public boolean isAt(int x, int y, int plane)
	{
		if (!isInRegion(((x >> 6) << 8) | (y >> 6)))
		{
			return false;
		}
		switch (this)
		{
			case FRUIT_CATHERBY:
				return x >= 2840 || y < 3440 || plane == 1;
			case HARDWOOD_FOSSIL_ISLAND_EAST:
			case HARDWOOD_FOSSIL_ISLAND_MIDDLE:
			case HARDWOOD_FOSSIL_ISLAND_WEST:
				if (x == 3753 && y >= 3868 && y <= 3870)
				{
					return false;
				}
				if ((x == 3729 || x == 3728 || x == 3747 || x == 3746) && y <= 3832 && y >= 3830)
				{
					return false;
				}
				return plane == 0;
			default:
				return true;
		}
	}

	public boolean isInRegion(int regionId)
	{
		for (int region : regions)
		{
			if (region == regionId)
			{
				return true;
			}
		}
		return false;
	}

	public String getDisplayName()
	{
		return patchType.getDisplayName() + " – " + step.getDisplayName() + (patchName != null ? " (" + patchName + ")" : "");
	}
}
