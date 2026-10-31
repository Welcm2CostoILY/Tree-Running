package com.treerunning.core;

import com.treerunning.data.CropType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Saplings and protection payments the rest of this run needs, one line per crop in route
 * order, against what's in your inventory.
 */
public final class RunNeeds
{
	public static final class Line
	{
		private final CropType crop;
		private final int saplings;
		private final int saplingsCarried;
		private final int payment;
		private final int paymentCarried;

		Line(CropType crop, int saplings, int saplingsCarried, int payment, int paymentCarried)
		{
			this.crop = crop;
			this.saplings = saplings;
			this.saplingsCarried = saplingsCarried;
			this.payment = payment;
			this.paymentCarried = paymentCarried;
		}

		public CropType getCrop()
		{
			return crop;
		}

		/** Saplings (or Hespori seeds) still to plant. */
		public int getSaplings()
		{
			return saplings;
		}

		/** How many of those are in your inventory. */
		public int getSaplingsCarried()
		{
			return saplingsCarried;
		}

		/** Protection items still needed, 0 when the crop can't be protected (Hespori). */
		public int getPayment()
		{
			return payment;
		}

		/** How many of those are in your inventory (noted counts). */
		public int getPaymentCarried()
		{
			return paymentCarried;
		}

		public boolean isShort()
		{
			return saplingsCarried < saplings || paymentCarried < payment;
		}
	}

	private RunNeeds()
	{
	}

	/** Totals for the whole run, ignoring your inventory. */
	public static List<Line> compute(List<RunTask> tasks)
	{
		return compute(tasks, java.util.Collections.emptySet(), id -> Integer.MAX_VALUE / 4);
	}

	/**
	 * @param tasks     the run's patches, in route order
	 * @param done      patches already ticked (they don't need anything any more)
	 * @param inventory how many of an unnoted item ID you're carrying (noted copies included)
	 */
	public static List<Line> compute(List<RunTask> tasks, Set<String> done, IntUnaryOperator inventory)
	{
		Map<CropType, Integer> counts = new LinkedHashMap<>();
		for (RunTask task : tasks)
		{
			if (task.isDue() && !done.contains(task.getKey()))
			{
				counts.merge(task.getCrop(), 1, Integer::sum);
			}
		}

		// Several crops can share a payment item (coconuts for magic and dragonfruit), so
		// what you carry is handed out in route order.
		Map<Integer, Integer> pool = new HashMap<>();
		List<Line> lines = new ArrayList<>();
		for (Map.Entry<CropType, Integer> e : counts.entrySet())
		{
			CropType crop = e.getKey();
			int saplings = e.getValue();
			int saplingId = crop.getPlantedItem().getId();
			int saplingsCarried = take(pool, saplingId, saplings, inventory);

			int payment = 0;
			int paymentCarried = 0;
			if (crop.getPayment() != null)
			{
				payment = saplings * crop.getPaymentQuantity();
				paymentCarried = take(pool, crop.getPayment().getId(), payment, inventory);
			}
			lines.add(new Line(crop, saplings, saplingsCarried, payment, paymentCarried));
		}
		return lines;
	}

	private static int take(Map<Integer, Integer> pool, int id, int wanted, IntUnaryOperator inventory)
	{
		int left = pool.computeIfAbsent(id, inventory::applyAsInt);
		int got = Math.min(left, wanted);
		pool.put(id, left - got);
		return got;
	}

	public static int totalSaplings(List<Line> lines)
	{
		int total = 0;
		for (Line line : lines)
		{
			total += line.getSaplings();
		}
		return total;
	}

	public static int shortCount(List<Line> lines)
	{
		int n = 0;
		for (Line line : lines)
		{
			if (line.isShort())
			{
				n++;
			}
		}
		return n;
	}
}
