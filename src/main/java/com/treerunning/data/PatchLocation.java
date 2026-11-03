package com.treerunning.data;

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
	HARDWOOD_FOSSIL_ISLAND_WEST(PatchType.HARDWOOD, RouteStep.FOSSIL_ISLAND, "West", "Bone Voyage", 4773, Regions.FOSSIL_ISLAND);

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
