package com.treerunning.core;

import com.treerunning.data.AllotmentType;
import com.treerunning.data.Compost;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchCondition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.ToLongFunction;

/**
 * Herb runs: which patches need a visit, what to do at each, what to bring and what each
 * herb is worth per patch. Pure logic; the plugin supplies what it saw and the prices.
 */
public final class HerbPlanner
{
	private HerbPlanner()
	{
	}

	/**
	 * @param seen what each patch looked like when last seen, by task key
	 * @return every enabled herb patch (and allotment, where turned on), in location order
	 */
	public static List<HerbTask> buildTasks(HerbSettings settings, Map<String, SeenPatch> seen, long now)
	{
		List<HerbTask> tasks = new ArrayList<>();
		for (HerbLocation location : HerbLocation.values())
		{
			if (!settings.enabled.contains(location))
			{
				continue;
			}
			tasks.add(task(location, java.util.Collections.singletonList(HerbTask.keyFor(location, -1)),
				settings.herbAt(location), null, settings, seen, now));
			if (settings.allotments.contains(location) && location.getAllotmentCount() > 0)
			{
				List<String> keys = new ArrayList<>();
				for (int i = 0; i < location.getAllotmentCount(); i++)
				{
					keys.add(HerbTask.keyFor(location, i));
				}
				tasks.add(task(location, keys, null, settings.allotmentCrop, settings, seen, now));
			}
		}
		return tasks;
	}

	/** What a patch most likely looks like now; weeds on an empty patch still count as empty. */
	private static PatchCondition predict(SeenPatch s, long now)
	{
		if (s == null)
		{
			return PatchCondition.UNKNOWN;
		}
		PatchCondition c = s.predict(now);
		if ((c == PatchCondition.GROWING || c == PatchCondition.GROWN) && s.isWeeds())
		{
			return PatchCondition.EMPTY;
		}
		return c;
	}

	/** The one condition shown for several patches together: the most urgent one. */
	private static int urgency(PatchCondition c)
	{
		switch (c)
		{
			case DISEASED:
				return 6;
			case DEAD:
				return 5;
			case GROWN:
			case CHECK_HEALTH:
				return 4;
			case EMPTY:
			case STUMP:
				return 3;
			case UNKNOWN:
				return 2;
			default:
				return 1;
		}
	}

	private static HerbTask task(HerbLocation location, List<String> keys, HerbType herb, AllotmentType crop, HerbSettings settings,
		Map<String, SeenPatch> seenMap, long now)
	{
		boolean allotments = crop != null;
		int n = keys.size();
		PatchCondition condition = PatchCondition.GROWING;
		boolean anyGrown = false;
		boolean anyDiseased = false;
		boolean anyDead = false;
		boolean anySeen = false;
		String growing = null;
		long readyAt = 0;
		for (String key : keys)
		{
			SeenPatch s = seenMap.get(key);
			PatchCondition c = predict(s, now);
			anySeen |= s != null;
			if (urgency(c) > urgency(condition))
			{
				condition = c;
			}
			anyGrown |= c == PatchCondition.GROWN || c == PatchCondition.UNKNOWN;
			anyDiseased |= c == PatchCondition.DISEASED;
			anyDead |= c == PatchCondition.DEAD;
			if (s != null && !s.isWeeds() && s.crop != null && growing == null)
			{
				growing = s.crop;
			}
			if (c == PatchCondition.GROWING && s != null)
			{
				readyAt = readyAt == 0 ? s.readyBy() : Math.min(readyAt, s.readyBy());
			}
		}
		if (condition != PatchCondition.GROWING)
		{
			readyAt = 0;
		}

		String both = n > 1 ? (n == 2 ? " both allotments" : " the allotments") : allotments ? " the allotment" : "";
		String plant = herb != null ? herb.getDisplayName() : crop.getDisplayName();
		List<HerbTask.Step> steps = new ArrayList<>();
		if (anyDiseased)
		{
			steps.add(new HerbTask.Step(HerbTask.CURE, allotments ? "Cure or clear" + both.replace(" both", " the diseased") : "Cure it (plant cure) or clear it"));
		}
		if (anyDead)
		{
			steps.add(new HerbTask.Step(HerbTask.CLEAR, allotments ? "Clear the dead allotment" : "Clear the dead patch"));
		}
		if (anyGrown)
		{
			steps.add(new HerbTask.Step(HerbTask.HARVEST, allotments ? "Harvest" + both
				: "Harvest" + (growing != null ? " " + growing : "")));
		}
		if (settings.compost != Compost.NONE)
		{
			steps.add(new HerbTask.Step(HerbTask.COMPOST, allotments && n > 1 ? "Compost both" : "Compost"));
		}
		steps.add(new HerbTask.Step(HerbTask.PLANT, allotments
			? "Plant " + (AllotmentType.SEEDS_PER_PATCH * n) + " " + plant + " seeds"
			: "Plant " + plant));
		if (allotments && settings.payForAllotments && location.getAllotmentGardener() >= 0)
		{
			steps.add(new HerbTask.Step(HerbTask.PAY, "Pay " + crop.getPaymentQuantity() * n + " " + crop.getPaymentName()
				+ (n > 1 ? " (" + crop.getPaymentQuantity() + " each)" : "")));
		}
		return new HerbTask(location, allotments, herb, crop, condition, readyAt, anySeen, keys, steps);
	}

	// ------------------------------------------------------------------ what to bring

	/** One line of "This run needs". */
	public static final class Need
	{
		private final String name;
		private final int itemId;
		private final int needed;
		private final int carried;

		Need(String name, int itemId, int needed, int carried)
		{
			this.name = name;
			this.itemId = itemId;
			this.needed = needed;
			this.carried = carried;
		}

		public String getName()
		{
			return name;
		}

		public int getItemId()
		{
			return itemId;
		}

		public int getNeeded()
		{
			return needed;
		}

		public int getCarried()
		{
			return carried;
		}

		public boolean isShort()
		{
			return carried < needed;
		}
	}

	/**
	 * Seeds, compost and payments for the due patches not planted yet.
	 *
	 * @param done      step keys done this run
	 * @param inventory carried count by item ID (noted folded in)
	 */
	public static List<Need> needs(List<HerbTask> tasks, Set<String> done, HerbSettings settings, IntUnaryOperator inventory)
	{
		Map<Integer, Integer> seeds = new LinkedHashMap<>();
		Map<Integer, String> names = new LinkedHashMap<>();
		Map<Integer, Integer> payments = new LinkedHashMap<>();
		int compost = 0;
		for (HerbTask task : tasks)
		{
			if (!task.isDue())
			{
				continue;
			}
			int toPlant = task.getPatchCount() - task.doneCount(done, HerbTask.PLANT);
			if (toPlant <= 0)
			{
				continue;
			}
			if (task.getHerb() != null)
			{
				seeds.merge(task.getHerb().getSeedId(), 1, Integer::sum);
				names.put(task.getHerb().getSeedId(), task.getHerb().getDisplayName() + " seed");
			}
			else
			{
				AllotmentType crop = task.getAllotmentCrop();
				seeds.merge(crop.getSeedId(), AllotmentType.SEEDS_PER_PATCH * toPlant, Integer::sum);
				names.put(crop.getSeedId(), crop.getDisplayName() + " seed");
				int toPay = task.getPatchCount() - task.doneCount(done, HerbTask.PAY);
				if (settings.payForAllotments && task.getLocation().getAllotmentGardener() >= 0 && toPay > 0)
				{
					payments.merge(crop.getPaymentId(), crop.getPaymentQuantity() * toPay, Integer::sum);
					names.put(crop.getPaymentId(), crop.getPaymentName());
				}
			}
			if (settings.compost != Compost.NONE)
			{
				compost += task.getPatchCount() - task.doneCount(done, HerbTask.COMPOST);
			}
		}
		List<Need> lines = new ArrayList<>();
		for (Map.Entry<Integer, Integer> e : seeds.entrySet())
		{
			lines.add(new Need(names.get(e.getKey()), e.getKey(), e.getValue(), inventory.applyAsInt(e.getKey())));
		}
		for (Map.Entry<Integer, Integer> e : payments.entrySet())
		{
			lines.add(new Need(names.get(e.getKey()), e.getKey(), e.getValue(), inventory.applyAsInt(e.getKey())));
		}
		if (compost > 0 && !settings.compost.isBottomless())
		{
			int id = settings.compost.getItemId();
			lines.add(new Need(settings.compost.toString(), id, compost, inventory.applyAsInt(id)));
		}
		return lines;
	}

	// ------------------------------------------------------------------ yield, XP and profit

	/**
	 * The chance to keep a harvest life on each pick, from the OSRS Wiki's herb calculator:
	 * floor(floor(low × (99 − L)/98 + 80 × (L − 1)/98) × (1 + items) × (1 + diary) + 1) / 256.
	 */
	public static double chanceToSave(HerbType herb, int level, double itemBonus, double patchBonus)
	{
		double base = Math.floor(herb.getChanceToSaveAt1() * (99.0 - level) / 98.0 + 80.0 * (level - 1.0) / 98.0);
		return Math.floor(base * (1.0 + itemBonus) * (1.0 + patchBonus) + 1.0) / 256.0;
	}

	/** Herbs picked from a grown patch on average: lives ÷ (1 − chance to save). */
	public static double expectedYield(HerbType herb, HerbLocation location, HerbSettings settings)
	{
		int lives = settings.compost.getHarvestLives();
		double save = chanceToSave(herb, settings.farmingLevel, settings.itemBonus, settings.patchBonus(location));
		return lives / (1.0 - save);
	}

	/**
	 * The chance the patch survives to harvest: 1 for a disease-free patch, otherwise the
	 * chance of avoiding disease on each of its three later growth cycles. Plant cure and
	 * Resurrect Crops are left out.
	 */
	public static double survival(HerbLocation location, HerbSettings settings)
	{
		if (location.isDiseaseFree())
		{
			return 1.0;
		}
		double perCycle = settings.compost.getDiseaseChance() / 128.0;
		return Math.pow(1.0 - perCycle, 3);
	}

	/** Estimated profit and XP for one herb in one patch. */
	public static final class Estimate
	{
		private final HerbType herb;
		private final double herbs;
		private final double xp;
		private final long value;
		private final long seedCost;
		private final long profit;

		Estimate(HerbType herb, double herbs, double xp, long value, long seedCost, long profit)
		{
			this.herb = herb;
			this.herbs = herbs;
			this.xp = xp;
			this.value = value;
			this.seedCost = seedCost;
			this.profit = profit;
		}

		/** The herbs picked on average, at the herb's price. */
		public long getValue()
		{
			return value;
		}

		/** The seed's price, or 0 when seed costs aren't counted (ironman). */
		public long getSeedCost()
		{
			return seedCost;
		}

		public HerbType getHerb()
		{
			return herb;
		}

		public double getHerbs()
		{
			return herbs;
		}

		public double getXp()
		{
			return xp;
		}

		public long getProfit()
		{
			return profit;
		}
	}

	/**
	 * @param price price of an item by ID (seeds, herbs, compost)
	 */
	public static Estimate estimate(HerbType herb, HerbLocation location, HerbSettings settings, ToLongFunction<Integer> price)
	{
		double alive = survival(location, settings);
		double herbs = expectedYield(herb, location, settings) * alive;
		// Planting XP is given with the last herb, so a patch that dies gives none.
		double xp = herb.getPlantXp() * alive + herb.getHarvestXp() * herbs;
		double compostCost = settings.compost.getItemId() > 0
			? price.applyAsLong(settings.compost.getItemId()) * settings.compost.getBucketsPerUse() : 0;
		long value = Math.round(herbs * price.applyAsLong(herb.getHerbId()));
		long seed = settings.countSeeds ? price.applyAsLong(herb.getSeedId()) : 0;
		long profit = Math.round(value - seed - compostCost);
		return new Estimate(herb, herbs, xp, value, seed, profit);
	}

	/** Every herb you can plant, best profit first, for an average (not disease-free) patch. */
	public static List<Estimate> picker(HerbSettings settings, ToLongFunction<Integer> price)
	{
		List<Estimate> rows = new ArrayList<>();
		for (HerbType herb : HerbType.values())
		{
			if (herb.getLevel() <= settings.farmingLevel)
			{
				rows.add(estimate(herb, HerbLocation.ARDOUGNE, settings, price));
			}
		}
		rows.sort((a, b) -> Long.compare(b.getProfit(), a.getProfit()));
		return rows;
	}

	/** Expected Farming XP from one herb run over every enabled herb patch. */
	public static double xpPerRun(HerbSettings settings)
	{
		double xp = 0;
		for (HerbLocation location : settings.enabled)
		{
			HerbType herb = settings.herbAt(location);
			if (herb.getLevel() <= settings.farmingLevel)
			{
				xp += estimate(herb, location, settings, id -> 0L).getXp();
			}
		}
		return xp;
	}
}
