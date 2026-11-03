package com.treerunning.core;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ties a Farming XP drop to the patch it came from: check-health, planting, harvesting and
 * so on. A patch counts when its varbit changed within one tick of the XP, while you were
 * standing where that patch's varbit is sent. If more than one patch changed (a growth
 * tick landing at the same moment), it's too ambiguous and nothing is returned.
 * Patches are identified by key (a tree task key, or a herb or allotment key).
 */
public final class ActionMatcher
{
	/** How many ticks either side of the XP drop a patch change still counts. */
	public static final int WINDOW_TICKS = 1;

	public static final class Change
	{
		final int tick;
		final String patch;

		public Change(int tick, String patch)
		{
			this.tick = tick;
			this.patch = patch;
		}

		public int tick()
		{
			return tick;
		}

		public String patch()
		{
			return patch;
		}
	}

	private ActionMatcher()
	{
	}

	/**
	 * @param xpTick  the tick the Farming XP arrived
	 * @param changes recent patch changes where you were standing
	 * @param skip    patches that shouldn't be ticked again (already done, or switched off)
	 * @return the one patch the XP came from, or null if none or several
	 */
	public static String find(int xpTick, Collection<Change> changes, Set<String> skip)
	{
		Set<String> candidates = new LinkedHashSet<>();
		for (Change change : changes)
		{
			if (Math.abs(change.tick - xpTick) <= WINDOW_TICKS && !skip.contains(change.patch))
			{
				candidates.add(change.patch);
			}
		}
		return candidates.size() == 1 ? candidates.iterator().next() : null;
	}
}
