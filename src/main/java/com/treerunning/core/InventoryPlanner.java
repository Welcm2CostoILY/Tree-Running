package com.treerunning.core;

import com.treerunning.data.CropType;
import com.treerunning.data.RouteStep;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Splits a run into legs that fit in a 28-slot inventory.
 *
 * Everything stackable (noted payments, coins) and every teleport item is carried for the
 * whole run. Saplings and Hespori seeds take one slot each, so when the next stop's
 * saplings won't fit, the plan inserts a bank stop.
 *
 * Picked produce: the tool leprechaun next to each patch only notes items, it doesn't
 * bank them. Noted produce stays in your inventory as one stack per kind, and you need
 * room for a full unnoted harvest before you can note it, so that space is reserved too.
 */
public final class InventoryPlanner
{
	public static final int INVENTORY_SIZE = 28;
	/** Room to pick a full tree (6 produce) before noting it at the leprechaun. */
	public static final int PICKING_BUFFER = 6;

	private InventoryPlanner()
	{
	}

	public static final class Leg
	{
		private final String bankAt;
		private final List<RouteStep> steps;
		private final Map<String, Integer> withdraw;

		Leg(String bankAt, List<RouteStep> steps, Map<String, Integer> withdraw)
		{
			this.bankAt = bankAt;
			this.steps = Collections.unmodifiableList(steps);
			this.withdraw = Collections.unmodifiableMap(withdraw);
		}

		/** Where to bank before this leg. */
		public String getBankAt()
		{
			return bankAt;
		}

		public List<RouteStep> getSteps()
		{
			return steps;
		}

		/** Saplings / seeds to withdraw (unnoted, one slot each). */
		public Map<String, Integer> getWithdraw()
		{
			return withdraw;
		}

		public int getSlots()
		{
			int slots = 0;
			for (int qty : withdraw.values())
			{
				slots += qty;
			}
			return slots;
		}
	}

	public static final class Plan
	{
		private final List<String> carryItems;
		private final Map<String, Integer> payments;
		private final List<String> notedProduce;
		private final int reservedSlots;
		private final int pickingSlots;
		private final int saplingCapacity;
		private final List<Leg> legs;

		Plan(List<String> carryItems, Map<String, Integer> payments, List<String> notedProduce, int reservedSlots,
			int pickingSlots, int saplingCapacity, List<Leg> legs)
		{
			this.carryItems = Collections.unmodifiableList(carryItems);
			this.payments = Collections.unmodifiableMap(payments);
			this.notedProduce = Collections.unmodifiableList(notedProduce);
			this.reservedSlots = reservedSlots;
			this.pickingSlots = pickingSlots;
			this.saplingCapacity = saplingCapacity;
			this.legs = Collections.unmodifiableList(legs);
		}

		/** Teleport items the route needs in your inventory. */
		public List<String> getCarryItems()
		{
			return carryItems;
		}

		/** Protection payments for the whole run (take them noted). */
		public Map<String, Integer> getPayments()
		{
			return payments;
		}

		/** Produce you'll pick and note at the leprechaun (one noted stack each). */
		public List<String> getNotedProduce()
		{
			return notedProduce;
		}

		/** Slots for your own always-carried items (spade, coins, house teleport...). */
		public int getReservedSlots()
		{
			return reservedSlots;
		}

		/** Slots kept free to pick a harvest before noting it. */
		public int getPickingSlots()
		{
			return pickingSlots;
		}

		public int getSaplingCapacity()
		{
			return saplingCapacity;
		}

		public List<Leg> getLegs()
		{
			return legs;
		}

		public int getBankStops()
		{
			return Math.max(0, legs.size() - 1);
		}
	}

	public static Plan plan(List<RunTask> tasks, PlanSettings settings)
	{
		// Group due tasks by stop, keeping route order.
		Map<RouteStep, List<RunTask>> byStep = new LinkedHashMap<>();
		for (RunTask task : tasks)
		{
			if (task.isDue())
			{
				byStep.computeIfAbsent(task.getStep(), s -> new ArrayList<>()).add(task);
			}
		}

		Set<String> carryItems = new LinkedHashSet<>();
		Map<String, Integer> payments = new LinkedHashMap<>();
		Set<String> produce = new LinkedHashSet<>();
		for (Map.Entry<RouteStep, List<RunTask>> entry : byStep.entrySet())
		{
			if (entry.getKey().getCarryItem() != null)
			{
				carryItems.add(entry.getKey().getCarryItem());
			}
			for (RunTask task : entry.getValue())
			{
				CropType crop = task.getCrop();
				if (crop.getPayment() != null)
				{
					payments.merge(crop.getPayment().getName(), crop.getPaymentQuantity(), Integer::sum);
				}
				if (settings.isPickProduce() && crop.getProduce() != null)
				{
					produce.add(crop.getProduce().getName());
				}
			}
		}

		int pickingSlots = produce.isEmpty() ? 0 : PICKING_BUFFER;
		int fixed = settings.getReservedSlots() + carryItems.size() + payments.size() + produce.size() + pickingSlots;
		int capacity = Math.max(1, INVENTORY_SIZE - fixed);

		List<Leg> legs = new ArrayList<>();
		List<RouteStep> legSteps = new ArrayList<>();
		Map<String, Integer> legWithdraw = new LinkedHashMap<>();
		int legSlots = 0;
		String bankAt = "Any bank before you start";
		RouteStep previous = null;

		for (Map.Entry<RouteStep, List<RunTask>> entry : byStep.entrySet())
		{
			RouteStep step = entry.getKey();
			int stepSlots = entry.getValue().size();

			if (!legSteps.isEmpty() && legSlots + stepSlots > capacity)
			{
				legs.add(new Leg(bankAt, legSteps, legWithdraw));
				bankAt = bankBetween(previous, step);
				legSteps = new ArrayList<>();
				legWithdraw = new LinkedHashMap<>();
				legSlots = 0;
			}

			legSteps.add(step);
			for (RunTask task : entry.getValue())
			{
				legWithdraw.merge(task.getCrop().getPlantedItem().getName(), 1, Integer::sum);
			}
			legSlots += stepSlots;
			previous = step;
		}

		if (!legSteps.isEmpty())
		{
			legs.add(new Leg(bankAt, legSteps, legWithdraw));
		}

		return new Plan(new ArrayList<>(carryItems), payments, new ArrayList<>(produce), settings.getReservedSlots(),
			pickingSlots, capacity, legs);
	}

	/** Prefer banking right after the last stop, then right at the next stop. */
	static String bankBetween(RouteStep finished, RouteStep next)
	{
		if (finished != null && finished.getBank() != null)
		{
			return finished.getBank() + " (after " + finished.getDisplayName() + ")";
		}
		if (next.getBank() != null)
		{
			return next.getBank() + " (when you arrive at " + next.getDisplayName() + ")";
		}
		return RouteStep.FALLBACK_BANK;
	}
}
