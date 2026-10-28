package com.treerunning.core;

import com.treerunning.data.CropType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Compares what the remaining runs need against what you have (bank + inventory).
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

		SupplyLine(String name, boolean payment, int need, int have)
		{
			this.name = name;
			this.payment = payment;
			this.need = need;
			this.have = have;
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
	}

	/**
	 * @param plantings how many of each crop the remaining runs plant
	 * @param haveById  how many of an unnoted item ID you own (noted copies included)
	 */
	public static List<SupplyLine> compute(Map<CropType, Integer> plantings, IntUnaryOperator haveById)
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
			crops.add(new SupplyLine(label, false, count, have));

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
			lines.add(new SupplyLine(paymentNames.get(id), true, entry.getValue(), haveById.applyAsInt(id)));
		}
		return lines;
	}
}
