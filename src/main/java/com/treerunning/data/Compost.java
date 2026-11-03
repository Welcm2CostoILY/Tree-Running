package com.treerunning.data;

import net.runelite.api.gameval.ItemID;

/**
 * Compost for herb and allotment patches. Harvest lives come from the OSRS Wiki (Farming,
 * variable crop yield); disease chances per growth cycle (out of 128) from the Wiki's herb
 * calculator, as used by the Herb Farming Calculator plugin.
 */
public enum Compost
{
	NONE("None", 3, 27, -1, 0),
	COMPOST("Compost", 4, 14, ItemID.BUCKET_COMPOST, 1),
	SUPERCOMPOST("Supercompost", 5, 6, ItemID.BUCKET_SUPERCOMPOST, 1),
	ULTRACOMPOST("Ultracompost", 6, 3, ItemID.BUCKET_ULTRACOMPOST, 1),
	/** A bottomless bucket of ultracompost: one bucket of ultracompost fills two uses. */
	BOTTOMLESS("Bottomless bucket (ultracompost)", 6, 3, ItemID.BUCKET_ULTRACOMPOST, 0.5);

	private final String displayName;
	private final int harvestLives;
	private final double diseaseChance;
	private final int itemId;
	private final double bucketsPerUse;

	Compost(String displayName, int harvestLives, double diseaseChance, int itemId, double bucketsPerUse)
	{
		this.displayName = displayName;
		this.harvestLives = harvestLives;
		this.diseaseChance = diseaseChance;
		this.itemId = itemId;
		this.bucketsPerUse = bucketsPerUse;
	}

	public int getHarvestLives()
	{
		return harvestLives;
	}

	/** Chance of disease per growth cycle, out of 128. */
	public double getDiseaseChance()
	{
		return diseaseChance;
	}

	/** The compost item bought for this, or -1. */
	public int getItemId()
	{
		return itemId;
	}

	/** Buckets of compost one use costs (half for the bottomless bucket). */
	public double getBucketsPerUse()
	{
		return bucketsPerUse;
	}

	public boolean isBottomless()
	{
		return this == BOTTOMLESS;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
