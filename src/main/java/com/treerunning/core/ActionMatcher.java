package com.treerunning.core;

import com.treerunning.data.PatchLocation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ties a Farming XP drop to the patch it came from: check-health, planting, harvesting a
 * Hespori, and so on. A patch counts when its varbit changed within one tick of the XP,
 * while you were standing in that patch's region. If more than one patch changed (a
 * growth tick landing at the same moment), it's too ambiguous and nothing is returned.
 */
public final class ActionMatcher
{
	/** How many ticks either side of the XP drop a patch change still counts. */
	public static final int WINDOW_TICKS = 1;

	public static final class Change
	{
		final int tick;
		final PatchLocation location;

		public Change(int tick, PatchLocation location)
		{
			this.tick = tick;
			this.location = location;
		}

		public int tick()
		{
			return tick;
		}

		public PatchLocation location()
		{
			return location;
		}
	}

	private ActionMatcher()
	{
	}

	/**
	 * @param xpTick  the tick the Farming XP arrived
	 * @param changes recent patch changes in your region
	 * @param skip    patches that shouldn't be ticked again (already done, or switched off)
	 * @return the one patch the XP came from, or null if none or several
	 */
	public static PatchLocation find(int xpTick, Collection<Change> changes, Set<PatchLocation> skip)
	{
		Set<PatchLocation> candidates = new LinkedHashSet<>();
		for (Change change : changes)
		{
			if (Math.abs(change.tick - xpTick) <= WINDOW_TICKS && !skip.contains(change.location))
			{
				candidates.add(change.location);
			}
		}
		return candidates.size() == 1 ? candidates.iterator().next() : null;
	}
}
