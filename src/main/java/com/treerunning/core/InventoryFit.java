package com.treerunning.core;

import com.treerunning.data.CropType;
import com.treerunning.data.RouteStep;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a tree run fits in one inventory between bank stops. Saplings don't stack, so
 * each takes a slot; payments are counted as one noted stack per item; a spade, coins and
 * each stop's teleport item take a slot each.
 */
public final class InventoryFit
{
	public static final int SLOTS = 28;
	/** Spade and coins. */
	static final int FIXED_SLOTS = 2;

	/** A stretch of the route that needs more than one inventory. */
	public static final class Overflow
	{
		private final int slots;
		private final RouteStep bankBefore;

		Overflow(int slots, RouteStep bankBefore)
		{
			this.slots = slots;
			this.bankBefore = bankBefore;
		}

		/** Slots the stretch needs. */
		public int getSlots()
		{
			return slots;
		}

		/** The stop to add a bank stop before, so the first part fits. */
		public RouteStep getBankBefore()
		{
			return bankBefore;
		}
	}

	private InventoryFit()
	{
	}

	/** Slots the whole remaining run needs from the next bank stop on (ignoring later bank stops). */
	public static int slotsFor(List<RouteItem> items, Set<String> done)
	{
		Counter c = new Counter();
		for (RouteItem item : items)
		{
			if (!item.isBank() && item.isDue() && !done.contains(item.getKey()))
			{
				c.add(item);
			}
		}
		return c.slots();
	}

	/**
	 * The first stretch between bank stops that won't fit in 28 slots, or null if every
	 * stretch fits. Only patches still to do count.
	 */
	public static Overflow check(List<RouteItem> items, Set<String> done)
	{
		Counter c = new Counter();
		RouteStep overflowAt = null;
		for (RouteItem item : items)
		{
			if (item.isBank())
			{
				if (done.contains(item.getKey()))
				{
					continue;
				}
				if (c.slots() > SLOTS)
				{
					return new Overflow(c.slots(), overflowAt);
				}
				c = new Counter();
				overflowAt = null;
				continue;
			}
			if (!item.isDue() || done.contains(item.getKey()))
			{
				continue;
			}
			c.add(item);
			if (overflowAt == null && c.slots() > SLOTS)
			{
				overflowAt = item.getStep();
			}
		}
		return c.slots() > SLOTS ? new Overflow(c.slots(), overflowAt) : null;
	}

	private static final class Counter
	{
		int saplings;
		final Set<Integer> payments = new HashSet<>();
		final Set<String> teleports = new HashSet<>();

		void add(RouteItem item)
		{
			CropType crop = item.getTask().getCrop();
			saplings++;
			if (crop.getPayment() != null)
			{
				payments.add(crop.getPayment().getId());
			}
			String carry = item.getStep().getCarryItem();
			if (carry != null)
			{
				teleports.add(carry);
			}
		}

		int slots()
		{
			return saplings == 0 ? 0 : saplings + payments.size() + teleports.size() + FIXED_SLOTS;
		}
	}
}
