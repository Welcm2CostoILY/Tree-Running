package com.treerunning.core;

import com.treerunning.data.RouteStep;

/**
 * A bank stop the player added to their route, before or after one stop's patches.
 * Saved as JSON in the plugin settings, so fields stay simple.
 */
public final class BankStop
{
	/** Stable ID, so a stop keeps its tick if it's renamed. */
	public String id;
	/** The RouteStep this bank stop belongs to (its enum name). */
	public String step;
	/** True to bank before planting the patches at this stop, false for after. */
	public boolean before = true;
	/** Where the player banks, as they typed it. */
	public String name = "";

	public BankStop()
	{
	}

	public BankStop(String id, RouteStep step, boolean before, String name)
	{
		this.id = id;
		this.step = step.name();
		this.before = before;
		this.name = name;
	}

	/** The stop this bank belongs to, or null if the saved step no longer exists. */
	public RouteStep getRouteStep()
	{
		for (RouteStep s : RouteStep.values())
		{
			if (s.name().equals(step))
			{
				return s;
			}
		}
		return null;
	}

	/** The key used to tick it off on a run. */
	public String getKey()
	{
		return "bank:" + id;
	}

	public String getLabel()
	{
		return "Bank: " + (name == null || name.trim().isEmpty() ? "bank stop" : name.trim());
	}
}
