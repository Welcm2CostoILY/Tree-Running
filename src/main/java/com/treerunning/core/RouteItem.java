package com.treerunning.core;

import com.treerunning.data.RouteStep;

/**
 * One line of the run, in order: a patch to plant or a bank stop.
 */
public final class RouteItem
{
	private final RouteStep step;
	private final RunTask task;
	private final BankStop bank;

	private RouteItem(RouteStep step, RunTask task, BankStop bank)
	{
		this.step = step;
		this.task = task;
		this.bank = bank;
	}

	static RouteItem patch(RunTask task)
	{
		return new RouteItem(task.getStep(), task, null);
	}

	static RouteItem bank(RouteStep step, BankStop bank)
	{
		return new RouteItem(step, null, bank);
	}

	public RouteStep getStep()
	{
		return step;
	}

	public boolean isBank()
	{
		return bank != null;
	}

	/** The patch, or null for a bank stop. */
	public RunTask getTask()
	{
		return task;
	}

	/** The bank stop, or null for a patch. */
	public BankStop getBank()
	{
		return bank;
	}

	public String getKey()
	{
		return isBank() ? bank.getKey() : task.getKey();
	}

	/** Patches that aren't ready yet can't be done; bank stops always can. */
	public boolean isDue()
	{
		return isBank() || task.isDue();
	}
}
