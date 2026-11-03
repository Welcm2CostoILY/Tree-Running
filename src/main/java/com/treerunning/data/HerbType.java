package com.treerunning.data;

import net.runelite.api.gameval.ItemID;

/**
 * Herb seeds. Levels and XP come from the OSRS Wiki; the "chance to save a life" at
 * level 1 comes from the Wiki's herb calculator (Module:Herb Farming calculator), as used
 * by the Herb Farming Calculator plugin. The chance at 99 is 80 for every herb.
 */
public enum HerbType
{
	GUAM("Guam", 9, 25, 11, 12.5, ItemID.GUAM_SEED, ItemID.UNIDENTIFIED_GUAM),
	MARRENTILL("Marrentill", 14, 28, 13.5, 15, ItemID.MARRENTILL_SEED, ItemID.UNIDENTIFIED_MARENTILL),
	TARROMIN("Tarromin", 19, 31, 16, 18, ItemID.TARROMIN_SEED, ItemID.UNIDENTIFIED_TARROMIN),
	HARRALANDER("Harralander", 26, 36, 21.5, 24, ItemID.HARRALANDER_SEED, ItemID.UNIDENTIFIED_HARRALANDER),
	RANARR("Ranarr", 32, 39, 27, 30.5, ItemID.RANARR_SEED, ItemID.UNIDENTIFIED_RANARR),
	TOADFLAX("Toadflax", 38, 43, 34, 38.5, ItemID.TOADFLAX_SEED, ItemID.UNIDENTIFIED_TOADFLAX),
	IRIT("Irit", 44, 46, 43, 48.5, ItemID.IRIT_SEED, ItemID.UNIDENTIFIED_IRIT),
	AVANTOE("Avantoe", 50, 50, 54.5, 61.5, ItemID.AVANTOE_SEED, ItemID.UNIDENTIFIED_AVANTOE),
	KWUARM("Kwuarm", 56, 54, 69, 78, ItemID.KWUARM_SEED, ItemID.UNIDENTIFIED_KWUARM),
	SNAPDRAGON("Snapdragon", 62, 57, 87.5, 98.5, ItemID.SNAPDRAGON_SEED, ItemID.UNIDENTIFIED_SNAPDRAGON),
	HUASCA("Huasca", 65, 58, 86.5, 110, ItemID.HUASCA_SEED, ItemID.UNIDENTIFIED_HUASCA),
	CADANTINE("Cadantine", 67, 60, 106.5, 120, ItemID.CADANTINE_SEED, ItemID.UNIDENTIFIED_CADANTINE),
	LANTADYME("Lantadyme", 73, 64, 134.5, 151.5, ItemID.LANTADYME_SEED, ItemID.UNIDENTIFIED_LANTADYME),
	DWARF_WEED("Dwarf weed", 79, 67, 170.5, 192, ItemID.DWARF_WEED_SEED, ItemID.UNIDENTIFIED_DWARF_WEED),
	TORSTOL("Torstol", 85, 71, 199.5, 224.5, ItemID.TORSTOL_SEED, ItemID.UNIDENTIFIED_TORSTOL);

	private final String displayName;
	private final int level;
	private final double chanceToSaveAt1;
	private final double plantXp;
	private final double harvestXp;
	private final int seedId;
	private final int herbId;

	HerbType(String displayName, int level, double chanceToSaveAt1, double plantXp, double harvestXp, int seedId, int herbId)
	{
		this.displayName = displayName;
		this.level = level;
		this.chanceToSaveAt1 = chanceToSaveAt1;
		this.plantXp = plantXp;
		this.harvestXp = harvestXp;
		this.seedId = seedId;
		this.herbId = herbId;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getLevel()
	{
		return level;
	}

	public double getChanceToSaveAt1()
	{
		return chanceToSaveAt1;
	}

	public double getPlantXp()
	{
		return plantXp;
	}

	public double getHarvestXp()
	{
		return harvestXp;
	}

	public int getSeedId()
	{
		return seedId;
	}

	/** The grimy herb you pick. */
	public int getHerbId()
	{
		return herbId;
	}

	/** The herb growing in a patch, from the crop name in its varbit reading. */
	public static HerbType byCropName(String name)
	{
		for (HerbType h : values())
		{
			if (h.displayName.equalsIgnoreCase(name))
			{
				return h;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
