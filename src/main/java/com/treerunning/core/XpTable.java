package com.treerunning.core;

/**
 * The standard OSRS experience table.
 */
public final class XpTable
{
	private static final int[] XP_FOR_LEVEL = new int[127];

	static
	{
		double points = 0;
		for (int level = 1; level < XP_FOR_LEVEL.length; level++)
		{
			XP_FOR_LEVEL[level] = (int) Math.floor(points / 4);
			points += Math.floor(level + 300 * Math.pow(2, level / 7.0));
		}
	}

	private XpTable()
	{
	}

	/** Total XP needed to reach a level (1–126). */
	public static int xpForLevel(int level)
	{
		int clamped = Math.max(1, Math.min(126, level));
		return XP_FOR_LEVEL[clamped];
	}

	/** The level a given amount of XP is worth (capped at 99). */
	public static int levelForXp(int xp)
	{
		int level = 1;
		while (level < 99 && xp >= XP_FOR_LEVEL[level + 1])
		{
			level++;
		}
		return level;
	}
}
