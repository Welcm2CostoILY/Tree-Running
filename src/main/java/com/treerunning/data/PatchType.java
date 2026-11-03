package com.treerunning.data;

/**
 * The kinds of patch the planner knows about.
 */
public enum PatchType
{
	TREE("Tree"),
	FRUIT_TREE("Fruit tree"),
	CALQUAT("Calquat"),
	CELASTRUS("Celastrus"),
	HARDWOOD("Hardwood"),
	REDWOOD("Redwood"),
	HESPORI("Hespori"),
	CRYSTAL("Crystal tree");

	private final String displayName;

	PatchType(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
