package com.treerunning.core;

import com.treerunning.data.CropType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saplings and protection payments the patches due on this run need, one line per crop in
 * route order.
 */
public final class RunNeeds
{
	public static final class Line
	{
		private final CropType crop;
		private final int saplings;

		Line(CropType crop, int saplings)
		{
			this.crop = crop;
			this.saplings = saplings;
		}

		public CropType getCrop()
		{
			return crop;
		}

		public int getSaplings()
		{
			return saplings;
		}

		/** Total protection items, 0 when the crop can't be protected (Hespori). */
		public int getPayment()
		{
			return crop.getPayment() == null ? 0 : saplings * crop.getPaymentQuantity();
		}
	}

	private RunNeeds()
	{
	}

	public static List<Line> compute(List<RunTask> tasks)
	{
		Map<CropType, Integer> counts = new LinkedHashMap<>();
		for (RunTask task : tasks)
		{
			if (task.isDue())
			{
				counts.merge(task.getCrop(), 1, Integer::sum);
			}
		}
		List<Line> lines = new ArrayList<>();
		for (Map.Entry<CropType, Integer> e : counts.entrySet())
		{
			lines.add(new Line(e.getKey(), e.getValue()));
		}
		return lines;
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
}
