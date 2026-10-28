package com.treerunning.core;

import com.treerunning.data.CropType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;

/**
 * Compares what the remaining runs need against what you have (bank, seed vault and
 * inventory), with the Grand Exchange price of each item.
 */
public final class SupplyCalculator
{
	private SupplyCalculator()
	{
	}

	public static final class SupplyLine
	{
		private final String name;
		private final boolean payment;
		private final int need;
		private final int have;
		private final long priceEach;

		SupplyLine(String name, boolean payment, int need, int have, long priceEach)
		{
			this.name = name;
			this.payment = payment;
			this.need = need;
			this.have = have;
			this.priceEach = priceEach;
		}

		/** For crops: the crop name (seeds + seedlings + saplings). For payments: the item name. */
		public String getName()
		{
			return name;
		}

		public boolean isPayment()
		{
			return payment;
		}

		public int getNeed()
		{
			return need;
		}

		public int getHave()
		{
			return have;
		}

		public int getShort()
		{
			return Math.max(0, need - have);
		}

		/** GE price of one (the seed, for crops). 0 when it can't be traded or has no price. */
		public long getPriceEach()
		{
			return priceEach;
		}

		/** What it would cost to buy what you're short of. */
		public long getShortCost()
		{
			return getShort() * priceEach;
		}
	}

	/**
	 * @param plantings how many of each crop the remaining runs plant
	 * @param haveById  how many of an unnoted item ID you own (noted copies included)
	 * @param priceById GE price of one of an item ID (0 if unknown)
	 */
	public static List<SupplyLine> compute(Map<CropType, Integer> plantings, IntUnaryOperator haveById, IntToLongFunction priceById)
	{
		List<SupplyLine> crops = new ArrayList<>();
		Map<Integer, String> paymentNames = new LinkedHashMap<>();
		Map<Integer, Integer> paymentNeed = new LinkedHashMap<>();

		for (Map.Entry<CropType, Integer> entry : plantings.entrySet())
		{
			CropType crop = entry.getKey();
			int count = entry.getValue();
			if (count <= 0)
			{
				continue;
			}

			int have = 0;
			for (int id : crop.getStockItemIds())
			{
				if (id >= 0)
				{
					have += haveById.applyAsInt(id);
				}
			}
			// Crop rows count seeds, seedlings and saplings together.
			String label = crop.getSapling() != null ? crop.getDisplayName() : crop.getSeed().getName();
			crops.add(new SupplyLine(label, false, count, have, Math.max(0, priceById.applyAsLong(crop.getSeed().getId()))));

			if (crop.getPayment() != null)
			{
				int id = crop.getPayment().getId();
				paymentNames.putIfAbsent(id, crop.getPayment().getName());
				paymentNeed.merge(id, count * crop.getPaymentQuantity(), Integer::sum);
			}
		}

		List<SupplyLine> lines = new ArrayList<>(crops);
		for (Map.Entry<Integer, Integer> entry : paymentNeed.entrySet())
		{
			int id = entry.getKey();
			lines.add(new SupplyLine(paymentNames.get(id), true, entry.getValue(), haveById.applyAsInt(id),
				Math.max(0, priceById.applyAsLong(id))));
		}
		return lines;
	}

	/** Total cost of everything you're short of. */
	public static long shortCost(List<SupplyLine> lines)
	{
		long total = 0;
		for (SupplyLine line : lines)
		{
			total += line.getShortCost();
		}
		return total;
	}
}
