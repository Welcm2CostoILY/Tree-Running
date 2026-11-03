package com.treerunning.data;

import net.runelite.api.gameval.ItemID;

/**
 * Allotment crops: 3 seeds a patch. Levels, XP and gardener payments come from the OSRS
 * Wiki (Allotment patch/Seeds). Planting XP is per patch, harvest XP per crop picked.
 */
public enum AllotmentType
{
	POTATO("Potato", 1, 8, 9, ItemID.POTATO_SEED, ItemID.POTATO, "Compost", ItemID.BUCKET_COMPOST, 2),
	ONION("Onion", 5, 9.5, 10.5, ItemID.ONION_SEED, ItemID.ONION, "Potatoes(10)", ItemID.SACK_POTATO_10, 1),
	CABBAGE("Cabbage", 7, 10, 11.5, ItemID.CABBAGE_SEED, ItemID.CABBAGE, "Onions(10)", ItemID.SACK_ONION_10, 1),
	TOMATO("Tomato", 12, 12.5, 14, ItemID.TOMATO_SEED, ItemID.TOMATO, "Cabbages(10)", ItemID.SACK_CABBAGE_10, 2),
	SWEETCORN("Sweetcorn", 20, 17, 19, ItemID.SWEETCORN_SEED, ItemID.SWEETCORN, "Jute fibre", ItemID.JUTE_FIBRE, 10),
	STRAWBERRY("Strawberry", 31, 26, 29, ItemID.STRAWBERRY_SEED, ItemID.STRAWBERRY, "Apples(5)", ItemID.BASKET_APPLE_5, 1),
	WATERMELON("Watermelon", 47, 48.5, 54.5, ItemID.WATERMELON_SEED, ItemID.WATERMELON, "Curry leaf", ItemID.CURRY_LEAF, 10),
	SNAPE_GRASS("Snape grass", 61, 82, 82, ItemID.SNAPE_GRASS_SEED, ItemID.SNAPE_GRASS, "Jangerberries", ItemID.JANGERBERRIES, 5);

	public static final int SEEDS_PER_PATCH = 3;

	private final String displayName;
	private final int level;
	private final double plantXp;
	private final double harvestXp;
	private final int seedId;
	private final int produceId;
	private final String paymentName;
	private final int paymentId;
	private final int paymentQuantity;

	AllotmentType(String displayName, int level, double plantXp, double harvestXp, int seedId, int produceId,
		String paymentName, int paymentId, int paymentQuantity)
	{
		this.displayName = displayName;
		this.level = level;
		this.plantXp = plantXp;
		this.harvestXp = harvestXp;
		this.seedId = seedId;
		this.produceId = produceId;
		this.paymentName = paymentName;
		this.paymentId = paymentId;
		this.paymentQuantity = paymentQuantity;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getLevel()
	{
		return level;
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

	public int getProduceId()
	{
		return produceId;
	}

	public String getPaymentName()
	{
		return paymentName;
	}

	public int getPaymentId()
	{
		return paymentId;
	}

	public int getPaymentQuantity()
	{
		return paymentQuantity;
	}

	public static AllotmentType byCropName(String name)
	{
		for (AllotmentType a : values())
		{
			if (a.displayName.equalsIgnoreCase(name))
			{
				return a;
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
