package com.treerunning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes what the tracker sees (Farming XP drops, patch changes, saplings leaving the
 * inventory, and what it decided) to RuneLite's own log, .runelite/logs/client.log, so a run
 * can be checked afterwards. Off unless turned on in settings. Every line starts with
 * "[Tree Running]" so it's easy to find.
 */
final class DebugLog
{
	private static final Logger LOG = LoggerFactory.getLogger("TreeRunning");

	private volatile boolean enabled;

	void setEnabled(boolean enabled)
	{
		boolean was = this.enabled;
		this.enabled = enabled;
		if (enabled && !was)
		{
			log(-1, "--- debug log on ---");
		}
	}

	boolean isEnabled()
	{
		return enabled;
	}

	/** @param tick the game tick, or -1 if not in game */
	void log(int tick, String message)
	{
		if (enabled)
		{
			LOG.info("[Tree Running] t{}  {}", tick, message);
		}
	}

	void shutDown()
	{
		enabled = false;
	}
}
