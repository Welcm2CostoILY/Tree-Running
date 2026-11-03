package com.treerunning;

import com.treerunning.core.RunRecord;
import com.treerunning.core.SeenPatch;
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
	/** More than any real run could reach; history entries beyond these are errors. */
	static final int MAX_HERB_RUN_PICKS = 1_000;
	static final int MAX_HERB_RUN_XP = 200_000;
	static final int MAX_TREE_RUN_XP = 1_000_000;

	/** True from the first patch of a run until you press Finish run. */
	boolean active;
	/** Farming XP at the start of the current run. */
	int startXp;
	long startedAt;
	/** Patches and bank stops ticked on the current run. */
	Set<String> done = new HashSet<>();
	/** When each patch was ticked on the current run. Growth timers start from these on Finish run. */
	Map<String, Long> tickedAt = new HashMap<>();
	/** When the last patch of the current run was ticked. */
	long lastTickAt;
	/** Farming XP shortly after the last patch (covers planting and payments after a check-health). */
	int runLastXp;
	/** When long-growing patches (hardwood, redwood, Hespori) were last planted, from finished runs. */
	Map<String, Long> plantedAt = new HashMap<>();
	/** Finished runs, oldest first. */
	List<RunRecord> runs = new ArrayList<>();
	/** XP of finished runs saved by older versions. Moved into runs on load. */
	List<Integer> runXp;
	/** Bank contents of tracked items (unnoted item ID → quantity) from the last time you opened it. */
	Map<Integer, Integer> bank = new HashMap<>();
	long bankSeenAt;
	/** Farming Guild seed vault contents, from the last time you opened it. */
	Map<Integer, Integer> vault = new HashMap<>();
	long vaultSeenAt;

	/** What each patch (tree, herb or allotment) looked like when last seen, by patch key. */
	Map<String, SeenPatch> seen = new HashMap<>();
	/** Patches whose gardener was paid on the current tree or herb run. */
	Set<String> paid = new HashSet<>();
	/** Produce picked on the current tree run (fruit, bark), by item ID. */
	Map<Integer, Integer> treeProduce = new HashMap<>();

	// Herb runs
	boolean herbActive;
	int herbStartXp;
	long herbStartedAt;
	long herbLastTickAt;
	/** Steps done on the current herb run ("herb:FALADOR|plant"). */
	Set<String> herbDone = new HashSet<>();
	int herbPicked;
	long herbValue;
	long herbCost;
	long herbSeedCost;
	long herbCompostCost;
	/** Seeds planted on the current herb run, by name. */
	Map<String, Integer> herbSeeds = new java.util.LinkedHashMap<>();
	int herbXp;
	List<RunRecord> herbRuns = new ArrayList<>();
	/** The "all grown" time already notified about, so it's only sent once. */
	long herbNotifiedFor;

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
		if (tickedAt == null)
		{
			tickedAt = new HashMap<>();
		}
		if (runs == null)
		{
			runs = new ArrayList<>();
		}
		if (runXp != null)
		{
			for (Integer xp : runXp)
			{
				if (xp != null)
				{
					runs.add(new RunRecord(0, xp, -1));
				}
			}
			runXp = null;
		}
		if (seen == null)
		{
			seen = new HashMap<>();
		}
		if (paid == null)
		{
			paid = new HashSet<>();
		}
		if (treeProduce == null)
		{
			treeProduce = new HashMap<>();
		}
		if (herbDone == null)
		{
			herbDone = new HashSet<>();
		}
		if (herbRuns == null)
		{
			herbRuns = new ArrayList<>();
		}
		if (herbSeeds == null)
		{
			herbSeeds = new java.util.LinkedHashMap<>();
		}
		// Runs saved by a version that read all your XP at login as one action: no run picks
		// thousands of herbs or gains this much XP.
		herbRuns.removeIf(r -> r == null || r.picked > MAX_HERB_RUN_PICKS || r.xp > MAX_HERB_RUN_XP);
		runs.removeIf(r -> r == null || r.xp > MAX_TREE_RUN_XP);
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
