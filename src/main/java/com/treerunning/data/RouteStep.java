package com.treerunning.data;

/**
 * The stops of a run, in the order you visit them. Travel notes assume a maxed house
 * with a spirit tree, fairy ring, portal nexus and ornate jewellery box.
 */
public enum RouteStep
{
	FARMING_GUILD("Farming Guild",
		"Jewellery box: Skills necklace → Farming Guild (or house spirit tree)",
		"Farming Guild bank (inside the guild)",
		null),
	GNOME_STRONGHOLD("Gnome Stronghold",
		"House spirit tree → Gnome Stronghold",
		"Grand Tree bank",
		null),
	TREE_GNOME_VILLAGE("Tree Gnome Village",
		"Spirit tree → Tree Gnome Village, squeeze through the railing, follow Elkoy, run south-west",
		null,
		null),
	CATHERBY("Catherby",
		"Portal nexus → Camelot, run to the Catherby beach patch",
		"Catherby bank",
		null),
	BRIMHAVEN("Brimhaven",
		"House spirit tree → Brimhaven, run west",
		null,
		null),
	TAI_BWO_WANNAI("Tai Bwo Wannai",
		"Run south from Brimhaven to the calquat patch",
		null,
		null),
	FALADOR("Falador Park",
		"Jewellery box: Ring of wealth → Falador Park",
		"Falador West bank",
		null),
	TAVERLEY("Taverley",
		"Falador wall shortcut west (66 Agility)",
		null,
		null),
	VARROCK("Varrock Castle",
		"Portal nexus → Varrock",
		"Varrock West bank",
		null),
	LUMBRIDGE("Lumbridge",
		"Portal nexus → Lumbridge, run west of the castle",
		"Lumbridge Castle bank (top floor)",
		null),
	NEMUS_RETREAT("Nemus Retreat",
		"Pendant of Ates → Nemus Retreat, run north (or quetzal to Auburnvale)",
		null,
		"Pendant of Ates"),
	KASTORI("Kastori",
		"Quetzal or Pendant of Ates → Kastori (fruit tree, then calquat)",
		null,
		"Pendant of Ates"),
	LOCUS_OASIS("Locus Oasis",
		"Fairy ring AJP, run south-east",
		null,
		null),
	LLETYA("Lletya",
		"Teleport crystal → Lletya, run south-east",
		"Lletya bank",
		"Teleport crystal"),
	SUMMER_SHORE("The Summer Shore",
		"Fairy ring CJQ, run south then west",
		null,
		null),
	ANGLERS_RETREAT("Anglers' Retreat",
		"Sail or take the rowboat from Corsair Cove (51 Sailing)",
		null,
		null),
	FOSSIL_ISLAND("Fossil Island",
		"Digsite pendant → Fossil Island, walk to the Mushroom Forest",
		null,
		"Digsite pendant");

	/** Used when neither side of a bank stop has a bank nearby. */
	public static final String FALLBACK_BANK = "Teleport to house → jewellery box: Ring of dueling → Castle Wars bank";

	private final String displayName;
	private final String travel;
	private final String bank;
	private final String carryItem;

	RouteStep(String displayName, String travel, String bank, String carryItem)
	{
		this.displayName = displayName;
		this.travel = travel;
		this.bank = bank;
		this.carryItem = carryItem;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getTravel()
	{
		return travel;
	}

	/** The nearest bank to this stop, or null if there isn't a convenient one. */
	public String getBank()
	{
		return bank;
	}

	/** A teleport item you need in your inventory for this stop, or null (house/jewellery box covers it). */
	public String getCarryItem()
	{
		return carryItem;
	}
}
