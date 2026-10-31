package com.treerunning.core;

import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Decides which checklist patch you just planted.
 *
 * In order of confidence:
 * 1. A patch of the right type whose varbit changed around the moment you planted, in the
 *    region you're standing in. This tells the three Fossil Island patches apart.
 * 2. A patch of the right type in the region you're standing in.
 * 3. The next unplanted patch of that type in route order (for anywhere the plugin has no
 *    region data, or if something unexpected happens).
 */
public final class PatchMatcher
{
	public enum Confidence
	{
		/** Matched by the patch's own state change. */
		PATCH,
		/** Matched by where you're standing. */
		REGION,
		/** Guessed from route order. */
		ROUTE
	}

	public static final class Match
	{
		private final RunTask task;
		private final Confidence confidence;

		Match(RunTask task, Confidence confidence)
		{
			this.task = task;
			this.confidence = confidence;
		}

		public RunTask getTask()
		{
			return task;
		}

		public Confidence getConfidence()
		{
			return confidence;
		}
	}

	private PatchMatcher()
	{
	}

	/**
	 * @param tasks          the run's checklist, in route order
	 * @param done           task keys already planted this run
	 * @param type           the patch type of what was planted
	 * @param region         the region ID you're standing in (-1 if unknown)
	 * @param changedPatches patches whose varbit changed just before/after planting
	 * @return the matched task, or null if every patch of that type is already ticked
	 */
	public static Match match(List<RunTask> tasks, Set<String> done, PatchType type, int region,
		Collection<PatchLocation> changedPatches)
	{
		for (RunTask task : tasks)
		{
			if (open(task, done, type) && changedPatches.contains(task.getLocation()) && task.getLocation().isInRegion(region))
			{
				return new Match(task, Confidence.PATCH);
			}
		}

		for (RunTask task : tasks)
		{
			if (open(task, done, type) && task.getLocation().isInRegion(region))
			{
				return new Match(task, Confidence.REGION);
			}
		}

		// You're at a stop that has this kind of patch, but it's already ticked (usually by
		// its check-health a moment ago). Don't guess another stop.
		for (RunTask task : tasks)
		{
			if (task.getLocation().getPatchType() == type && task.getLocation().isInRegion(region))
			{
				return null;
			}
		}

		// Planted somewhere the plugin can't place. Fall back to route order, but only
		// among due patches.
		for (RunTask task : tasks)
		{
			if (open(task, done, type) && task.isDue())
			{
				return new Match(task, Confidence.ROUTE);
			}
		}
		return null;
	}

	/** Not ticked yet and the right type. Not-due patches count too: you can only plant where it's ready. */
	private static boolean open(RunTask task, Set<String> done, PatchType type)
	{
		return !done.contains(task.getKey()) && task.getLocation().getPatchType() == type;
	}
}
