package com.treerunning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import org.junit.Test;

public class RunStateTest
{
	@Test
	public void oldRunXpListMovesIntoHistory()
	{
		String old = "{\"active\":false,\"runXp\":[90000,95000],\"done\":[],\"plantedAt\":{}}";
		RunState state = new Gson().fromJson(old, RunState.class);
		state.fillDefaults();
		assertNull(state.runXp);
		assertEquals(2, state.runs.size());
		assertEquals(95000, state.runs.get(1).xp);
		assertEquals(-1, state.runs.get(1).patches);
		assertNotNull(state.tickedAt);

		// Saving and loading again doesn't add them twice.
		RunState again = new Gson().fromJson(new Gson().toJson(state), RunState.class);
		again.fillDefaults();
		assertEquals(2, again.runs.size());
	}

	@Test
	public void impossibleRunsFromTheLoginBugAreRemoved()
	{
		String bad = "{\"herbRuns\":[{\"endedAt\":1,\"xp\":5300000,\"picked\":173770,\"value\":274000000,\"profitTracked\":true},"
			+ "{\"endedAt\":2,\"xp\":6000,\"picked\":63,\"value\":500000,\"profitTracked\":true}],"
			+ "\"runs\":[{\"endedAt\":3,\"xp\":5400000,\"patches\":3},{\"endedAt\":4,\"xp\":182000,\"patches\":19}]}";
		RunState state = new Gson().fromJson(bad, RunState.class);
		state.fillDefaults();
		assertEquals(1, state.herbRuns.size());
		assertEquals(63, state.herbRuns.get(0).picked);
		assertEquals(1, state.runs.size());
		assertEquals(182000, state.runs.get(0).xp);
	}
}
