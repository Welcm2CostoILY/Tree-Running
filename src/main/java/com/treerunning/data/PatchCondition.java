package com.treerunning.data;

/** What a patch looks like right now. The order matches tools/gen_patch_states.py. */
public enum PatchCondition
{
	UNKNOWN("Unknown"),
	EMPTY("Empty"),
	GROWING("Growing"),
	CHECK_HEALTH("Check health"),
	GROWN("Grown"),
	STUMP("Stump"),
	DISEASED("Diseased"),
	DEAD("Dead");

	private final String label;

	PatchCondition(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	/** Something to do here: it's ready, needs clearing or needs replanting. */
	public boolean needsVisit()
	{
		return this != GROWING && this != UNKNOWN;
	}
}
