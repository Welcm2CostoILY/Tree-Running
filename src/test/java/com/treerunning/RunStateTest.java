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
}
