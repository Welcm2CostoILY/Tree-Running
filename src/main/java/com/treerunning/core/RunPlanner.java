package com.treerunning.core;

import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds the checklist for a run: every enabled patch in route order, with patches that
 * are still growing (or need a higher level) marked as not due.
 */
public final class RunPlanner
{
	private RunPlanner()
	{
	}

	/**
	 * @param plantedAt when each long-growing patch was last planted (task key → epoch millis)
	 */
	public static List<RunTask> buildTasks(PlanSettings settings, int farmingLevel, Map<String, Long> plantedAt, long nowMillis)
	{
		List<PatchLocation> locations = new ArrayList<>(Arrays.asList(PatchLocation.values()));
		locations.sort(Comparator
			.comparingInt((PatchLocation l) -> l.getStep().ordinal())
			.thenComparingInt(Enum::ordinal));

		List<RunTask> tasks = new ArrayList<>();
		for (PatchLocation location : locations)
		{
			if (!settings.isEnabled(location))
			{
				continue;
			}

			CropType crop = settings.cropFor(location.getPatchType());
			for (int i = 0; i < location.getPatchCount(); i++)
			{
				String key = RunTask.keyFor(location, i);
				boolean due = true;
				String note = null;

				if (crop.getLevel() > farmingLevel)
				{
					due = false;
					note = "Needs " + crop.getLevel() + " Farming";
				}
				else
				{
					// Every patch is timed from its last finished run, so a calquat (21 h) or fruit tree
					// (16 h) planted late yesterday isn't shown as due before it can have grown.
					Long planted = plantedAt.get(key);
					if (planted != null)
					{
						long readyAt = planted + crop.getGrowthMinutes() * 60_000L;
						if (nowMillis < readyAt)
						{
							due = false;
							note = "Ready in " + formatDuration(readyAt - nowMillis);
						}
					}
				}

				tasks.add(new RunTask(location, i, crop, due, note));
			}
		}
		return tasks;
	}

	static String formatDuration(long millis)
	{
		long totalMinutes = Math.max(0, millis) / 60_000L;
		long hours = totalMinutes / 60;
		long minutes = totalMinutes % 60;
		if (hours >= 24)
		{
			return (hours / 24) + "d " + (hours % 24) + "h";
		}
		return hours + "h " + minutes + "m";
	}
}
