package com.treerunning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the plugin remembers per account. Saved as JSON in the RuneScape profile
 * config, so each of your accounts has its own progress.
 */
class RunState
{
	/** True between the first plant of a run and finishing it. */
	boolean active;
	/** Farming XP at the start of the current run. */
	int startXp;
	long startedAt;
	/** Task keys planted on the current run. */
	Set<String> done = new HashSet<>();
	/** When long-growing patches (hardwood, redwood, Hespori) were last planted. */
	Map<String, Long> plantedAt = new HashMap<>();
	/** XP gained on each completed run, oldest first. */
	List<Integer> runXp = new ArrayList<>();
	/** Bank contents of tracked items (unnoted item ID → quantity) from the last time you opened it. */
	Map<Integer, Integer> bank = new HashMap<>();
	long bankSeenAt;
	/** Farming Guild seed vault contents, from the last time you opened it. */
	Map<Integer, Integer> vault = new HashMap<>();
	long vaultSeenAt;

	/** Gson leaves missing fields null when loading older saves. */
	void fillDefaults()
	{
		if (done == null)
		{
			done = new HashSet<>();
		}
		if (plantedAt == null)
		{
			plantedAt = new HashMap<>();
		}
		if (runXp == null)
		{
			runXp = new ArrayList<>();
		}
		if (bank == null)
		{
			bank = new HashMap<>();
		}
		if (vault == null)
		{
			vault = new HashMap<>();
		}
	}
}
