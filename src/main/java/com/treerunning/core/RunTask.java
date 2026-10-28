package com.treerunning.core;

import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.RouteStep;

/**
 * One patch to plant on this run.
 */
public final class RunTask
{
	private final PatchLocation location;
	private final int index;
	private final CropType crop;
	private final boolean due;
	private final String note;

	public RunTask(PatchLocation location, int index, CropType crop, boolean due, String note)
	{
		this.location = location;
		this.index = index;
		this.crop = crop;
		this.due = due;
		this.note = note;
	}

	/** Stable key for saving progress, e.g. "HARDWOOD_FOSSIL_ISLAND_EAST#0". */
	public static String keyFor(PatchLocation location, int index)
	{
		return location.name() + "#" + index;
	}

	public String getKey()
	{
		return keyFor(location, index);
	}

	public PatchLocation getLocation()
	{
		return location;
	}

	public RouteStep getStep()
	{
		return location.getStep();
	}

	public int getIndex()
	{
		return index;
	}

	public CropType getCrop()
	{
		return crop;
	}

	/** False when the patch is still growing or the crop's level isn't met yet. */
	public boolean isDue()
	{
		return due;
	}

	/** Why the task isn't due (or null). */
	public String getNote()
	{
		return note;
	}

	public String getLabel()
	{
		String name = crop.getDisplayName() + " – " + location.getStep().getDisplayName();
		if (location.getPatchName() != null)
		{
			name += " (" + location.getPatchName() + ")";
		}
		return name;
	}
}
