package com.treerunning.data;

import net.runelite.api.gameval.ItemID;

/**
 * Every crop the planner can schedule.
 *
 * XP is the full amount for one planting (plant + check-health, plus picking all the
 * produce for fruit trees). Levels, XP, payments and growth times come from the OSRS
 * Wiki (Calculator:Farming/Trees and the Special tree patch page).
 *
 * A crop's growth time only matters when it is longer than a day (hardwood, redwood,
 * Hespori); anything that grows in under 24 hours is planted on every run.
 */
public enum CropType
{
	// Tree patches
	OAK(PatchType.TREE, "Oak", 15, 481.3, 200,
		item("Acorn", ItemID.ACORN), ItemID.PLANTPOT_ACORN, ItemID.PLANTPOT_ACORN_WATERED,
		item("Oak sapling", ItemID.PLANTPOT_OAK_SAPLING), item("Tomatoes(5)", ItemID.BASKET_TOMATO_5), 1, null),
	WILLOW(PatchType.TREE, "Willow", 30, 1481.5, 280,
		item("Willow seed", ItemID.WILLOW_SEED), ItemID.PLANTPOT_WILLOW_SEED, ItemID.PLANTPOT_WILLOW_SEED_WATERED,
		item("Willow sapling", ItemID.PLANTPOT_WILLOW_SAPLING), item("Apples(5)", ItemID.BASKET_APPLE_5), 1, null),
	MAPLE(PatchType.TREE, "Maple", 45, 3448.4, 320,
		item("Maple seed", ItemID.MAPLE_SEED), ItemID.PLANTPOT_MAPLE_SEED, ItemID.PLANTPOT_MAPLE_SEED_WATERED,
		item("Maple sapling", ItemID.PLANTPOT_MAPLE_SAPLING), item("Oranges(5)", ItemID.BASKET_ORANGE_5), 1, null),
	YEW(PatchType.TREE, "Yew", 60, 7150.9, 400,
		item("Yew seed", ItemID.YEW_SEED), ItemID.PLANTPOT_YEW_SEED, ItemID.PLANTPOT_YEW_SEED_WATERED,
		item("Yew sapling", ItemID.PLANTPOT_YEW_SAPLING), item("Cactus spine", ItemID.CACTUS_SPINE), 10, null),
	MAGIC(PatchType.TREE, "Magic", 75, 13913.8, 480,
		item("Magic seed", ItemID.MAGIC_TREE_SEED), ItemID.PLANTPOT_MAGIC_TREE_SEED, ItemID.PLANTPOT_MAGIC_TREE_SEED_WATERED,
		item("Magic sapling", ItemID.PLANTPOT_MAGIC_TREE_SAPLING), item("Coconut", ItemID.COCONUT), 25, null),

	// Fruit tree patches
	APPLE(PatchType.FRUIT_TREE, "Apple", 27, 1272.5, 960,
		item("Apple tree seed", ItemID.APPLE_TREE_SEED), ItemID.PLANTPOT_APPLE_SEED, ItemID.PLANTPOT_APPLE_SEED_WATERED,
		item("Apple sapling", ItemID.PLANTPOT_APPLE_SAPLING), item("Sweetcorn", ItemID.SWEETCORN), 9,
		item("Cooking apple", ItemID.COOKING_APPLE)),
	BANANA(PatchType.FRUIT_TREE, "Banana", 33, 1841.5, 960,
		item("Banana tree seed", ItemID.BANANA_TREE_SEED), ItemID.PLANTPOT_BANANA_SEED, ItemID.PLANTPOT_BANANA_SEED_WATERED,
		item("Banana sapling", ItemID.PLANTPOT_BANANA_SAPLING), item("Apples(5)", ItemID.BASKET_APPLE_5), 4,
		item("Banana", ItemID.BANANA)),
	ORANGE(PatchType.FRUIT_TREE, "Orange", 39, 2586.7, 960,
		item("Orange tree seed", ItemID.ORANGE_TREE_SEED), ItemID.PLANTPOT_ORANGE_SEED, ItemID.PLANTPOT_ORANGE_SEED_WATERED,
		item("Orange sapling", ItemID.PLANTPOT_ORANGE_SAPLING), item("Strawberries(5)", ItemID.BASKET_STRAWBERRY_5), 3,
		item("Orange", ItemID.ORANGE)),
	CURRY(PatchType.FRUIT_TREE, "Curry", 42, 3036.9, 960,
		item("Curry tree seed", ItemID.CURRY_TREE_SEED), ItemID.PLANTPOT_CURRY_SEED, ItemID.PLANTPOT_CURRY_SEED_WATERED,
		item("Curry sapling", ItemID.PLANTPOT_CURRY_SAPLING), item("Bananas(5)", ItemID.BASKET_BANANA_5), 5,
		item("Curry leaf", ItemID.CURRY_LEAF)),
	PINEAPPLE(PatchType.FRUIT_TREE, "Pineapple", 51, 4791.7, 960,
		item("Pineapple seed", ItemID.PINEAPPLE_TREE_SEED), ItemID.PLANTPOT_PINEAPPLE_SEED, ItemID.PLANTPOT_PINEAPPLE_SEED_WATERED,
		item("Pineapple sapling", ItemID.PLANTPOT_PINEAPPLE_SAPLING), item("Watermelon", ItemID.WATERMELON), 10,
		item("Pineapple", ItemID.PINEAPPLE)),
	PAPAYA(PatchType.FRUIT_TREE, "Papaya", 57, 6453.9, 960,
		item("Papaya tree seed", ItemID.PAPAYA_TREE_SEED), ItemID.PLANTPOT_PAPAYA_SEED, ItemID.PLANTPOT_PAPAYA_SEED_WATERED,
		item("Papaya sapling", ItemID.PLANTPOT_PAPAYA_SAPLING), item("Pineapple", ItemID.PINEAPPLE), 10,
		item("Papaya fruit", ItemID.PAPAYA)),
	PALM(PatchType.FRUIT_TREE, "Palm", 68, 10509.6, 960,
		item("Palm tree seed", ItemID.PALM_TREE_SEED), ItemID.PLANTPOT_PALM_SEED, ItemID.PLANTPOT_PALM_SEED_WATERED,
		item("Palm sapling", ItemID.PLANTPOT_PALM_SAPLING), item("Papaya fruit", ItemID.PAPAYA), 15,
		item("Coconut", ItemID.COCONUT)),
	DRAGONFRUIT(PatchType.FRUIT_TREE, "Dragonfruit", 81, 17895, 960,
		item("Dragonfruit tree seed", ItemID.DRAGONFRUIT_TREE_SEED), ItemID.PLANTPOT_DRAGONFRUIT_SEED, ItemID.PLANTPOT_DRAGONFRUIT_SEED_WATERED,
		item("Dragonfruit sapling", ItemID.PLANTPOT_DRAGONFRUIT_SAPLING), item("Coconut", ItemID.COCONUT), 15,
		item("Dragonfruit", ItemID.DRAGONFRUIT)),

	// Hardwood patches
	TEAK(PatchType.HARDWOOD, "Teak", 35, 7325, 4480,
		item("Teak seed", ItemID.TEAK_SEED), ItemID.PLANTPOT_TEAK_SEED, ItemID.PLANTPOT_TEAK_SEED_WATERED,
		item("Teak sapling", ItemID.PLANTPOT_TEAK_SAPLING), item("Limpwurt root", ItemID.LIMPWURT_ROOT), 15, null),
	MAHOGANY(PatchType.HARDWOOD, "Mahogany", 55, 15783, 5120,
		item("Mahogany seed", ItemID.MAHOGANY_SEED), ItemID.PLANTPOT_MAHOGANY_SEED, ItemID.PLANTPOT_MAHOGANY_SEED_WATERED,
		item("Mahogany sapling", ItemID.PLANTPOT_MAHOGANY_SAPLING), item("Yanillian hops", ItemID.YANILLIAN_HOPS), 25, null),
	CAMPHOR(PatchType.HARDWOOD, "Camphor", 66, 17928, 5120,
		item("Camphor seed", ItemID.CAMPHOR_SEED), ItemID.PLANTPOT_CAMPHOR_SEED, ItemID.PLANTPOT_CAMPHOR_SEED_WATERED,
		item("Camphor sapling", ItemID.PLANTPOT_CAMPHOR_SAPLING), item("White berries", ItemID.WHITE_BERRIES), 10, null),
	IRONWOOD(PatchType.HARDWOOD, "Ironwood", 80, 20525, 5120,
		item("Ironwood seed", ItemID.IRONWOOD_SEED), ItemID.PLANTPOT_IRONWOOD_SEED, ItemID.PLANTPOT_IRONWOOD_SEED_WATERED,
		item("Ironwood sapling", ItemID.PLANTPOT_IRONWOOD_SAPLING), item("Curry leaf", ItemID.CURRY_LEAF), 10, null),
	ROSEWOOD(PatchType.HARDWOOD, "Rosewood", 92, 23352, 6400,
		item("Rosewood seed", ItemID.ROSEWOOD_SEED), ItemID.PLANTPOT_ROSEWOOD_SEED, ItemID.PLANTPOT_ROSEWOOD_SEED_WATERED,
		item("Rosewood sapling", ItemID.PLANTPOT_ROSEWOOD_SAPLING), item("Dragonfruit", ItemID.DRAGONFRUIT), 8, null),

	// Special patches
	CALQUAT(PatchType.CALQUAT, "Calquat", 72, 12516.5, 1280,
		item("Calquat tree seed", ItemID.CALQUAT_TREE_SEED), ItemID.PLANTPOT_CALQUAT_SEED, ItemID.PLANTPOT_CALQUAT_SEED_WATERED,
		item("Calquat sapling", ItemID.PLANTPOT_CALQUAT_SAPLING), item("Poison ivy berries", ItemID.POISONIVY_BERRIES), 8,
		item("Calquat fruit", ItemID.CALQUAT_FRUIT)),
	CELASTRUS(PatchType.CELASTRUS, "Celastrus", 85, 14400.5, 800,
		item("Celastrus seed", ItemID.CELASTRUS_TREE_SEED), ItemID.PLANTPOT_CELASTRUS_TREE_SEED, ItemID.PLANTPOT_CELASTRUS_TREE_SEED_WATERED,
		item("Celastrus sapling", ItemID.PLANTPOT_CELASTRUS_TREE_SAPLING), item("Potato cactus", ItemID.CACTUS_POTATO), 8,
		item("Celastrus bark", ItemID.CELASTRUS_WOOD)),
	REDWOOD(PatchType.REDWOOD, "Redwood", 90, 22680, 6400,
		item("Redwood tree seed", ItemID.REDWOOD_TREE_SEED), ItemID.PLANTPOT_REDWOOD_TREE_SEED, ItemID.PLANTPOT_REDWOOD_TREE_SEED_WATERED,
		item("Redwood sapling", ItemID.PLANTPOT_REDWOOD_TREE_SAPLING), item("Dragonfruit", ItemID.DRAGONFRUIT), 6, null),
	// Hespori: 62 for planting + 12,600 for harvesting after the kill. The seed is planted directly.
	HESPORI(PatchType.HESPORI, "Hespori", 65, 12662, 1920,
		item("Hespori seed", ItemID.HESPORI_SEED), -1, -1, null, null, 0, null),
	// Crystal tree: 126 for planting + 13,240 for checking (OSRS Wiki). It can't get diseased,
	// so there's no payment, and its shards are untradeable.
	CRYSTAL(PatchType.CRYSTAL, "Crystal", 74, 13366, 480,
		item("Crystal acorn", ItemID.CRYSTAL_TREE_SEED), ItemID.PLANTPOT_CRYSTAL_TREE_SEED, ItemID.PLANTPOT_CRYSTAL_TREE_SEED_WATERED,
		item("Crystal sapling", ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING), null, 0, null);

	/** An item's display name and unnoted item ID. */
	public static final class Item
	{
		private final String name;
		private final int id;

		Item(String name, int id)
		{
			this.name = name;
			this.id = id;
		}

		public String getName()
		{
			return name;
		}

		public int getId()
		{
			return id;
		}
	}

	private static Item item(String name, int id)
	{
		return new Item(name, id);
	}

	private final PatchType patchType;
	private final String displayName;
	private final int level;
	private final double xp;
	private final int growthMinutes;
	private final Item seed;
	private final int seedlingId;
	private final int wateredSeedlingId;
	private final Item sapling;
	private final Item payment;
	private final int paymentQuantity;
	private final Item produce;

	CropType(PatchType patchType, String displayName, int level, double xp, int growthMinutes,
		Item seed, int seedlingId, int wateredSeedlingId, Item sapling, Item payment, int paymentQuantity, Item produce)
	{
		this.patchType = patchType;
		this.displayName = displayName;
		this.level = level;
		this.xp = xp;
		this.growthMinutes = growthMinutes;
		this.seed = seed;
		this.seedlingId = seedlingId;
		this.wateredSeedlingId = wateredSeedlingId;
		this.sapling = sapling;
		this.payment = payment;
		this.paymentQuantity = paymentQuantity;
		this.produce = produce;
	}

	public PatchType getPatchType()
	{
		return patchType;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getLevel()
	{
		return level;
	}

	public double getXp()
	{
		return xp;
	}

	public int getGrowthMinutes()
	{
		return growthMinutes;
	}

	public Item getSeed()
	{
		return seed;
	}

	/** Null for crops planted straight from the seed (Hespori). */
	public Item getSapling()
	{
		return sapling;
	}

	/** The item you carry and plant: the sapling, or the seed for Hespori. */
	public Item getPlantedItem()
	{
		return sapling != null ? sapling : seed;
	}

	/**
	 * Every item that counts toward "have" for this crop: seed, both seedling stages
	 * and the sapling.
	 */
	public int[] getStockItemIds()
	{
		if (sapling == null)
		{
			return new int[]{seed.getId()};
		}
		return new int[]{seed.getId(), seedlingId, wateredSeedlingId, sapling.getId()};
	}

	/** Null when the patch can't be protected (Hespori). */
	public Item getPayment()
	{
		return payment;
	}

	public int getPaymentQuantity()
	{
		return paymentQuantity;
	}

	/** What you pick from the tree into your inventory, or null if nothing. */
	public Item getProduce()
	{
		return produce;
	}

	/**
	 * How many runs apart this crop gets replanted. Crops that grow faster than the time
	 * between runs are replanted every run.
	 */
	public int getIntervalRuns(int runsPerDay)
	{
		double minutesBetweenRuns = 1440.0 / Math.max(1, runsPerDay);
		return Math.max(1, (int) Math.ceil(growthMinutes / minutesBetweenRuns));
	}
}
