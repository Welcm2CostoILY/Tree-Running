package com.treerunning;

import com.treerunning.core.HerbTask;
import com.treerunning.core.RunTask;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.PatchKind;
import com.treerunning.data.PatchLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every patch the plugin reads in game: the tree patches, herb patches and allotments.
 * Each has a key (the same as its run task's), the varbit holding its state, how to read
 * that varbit, and where the varbit is sent.
 */
final class TrackedPatch
{
	enum Type
	{
		TREE,
		HERB,
		ALLOTMENT
	}

	final String key;
	final Type type;
	final PatchKind kind;
	final int varbit;
	/** The tree patch, or null. */
	final PatchLocation tree;
	/** The herb location (herb patch or allotment), or null. */
	final HerbLocation herb;
	/** Which allotment, or -1. */
	final int allotment;
	/** The gardener who protects it, or -1. */
	final int gardener;
	/** The option for this patch in the gardener's "which patch?" dialogue, or -1 if there's only one. */
	final int gardenerOption;

	private TrackedPatch(String key, Type type, PatchKind kind, int varbit, PatchLocation tree, HerbLocation herb, int allotment,
		int gardener, int gardenerOption)
	{
		this.key = key;
		this.type = type;
		this.kind = kind;
		this.varbit = varbit;
		this.tree = tree;
		this.herb = herb;
		this.allotment = allotment;
		this.gardener = gardener;
		this.gardenerOption = gardenerOption;
	}

	boolean isAt(int x, int y, int plane)
	{
		return tree != null ? tree.isAt(x, y, plane) : herb.isAt(x, y, plane);
	}

	String name()
	{
		if (tree != null)
		{
			return tree.getDisplayName();
		}
		return herb.getDisplayName() + (allotment < 0 ? " herb patch" : " " + herb.allotmentName(allotment).toLowerCase());
	}

	static final List<TrackedPatch> ALL;
	static final Map<String, TrackedPatch> BY_KEY;
	static final Map<Integer, List<TrackedPatch>> BY_VARBIT;

	static
	{
		List<TrackedPatch> all = new ArrayList<>();
		for (PatchLocation location : PatchLocation.values())
		{
			all.add(new TrackedPatch(RunTask.keyFor(location, 0), Type.TREE, location.getKind(), location.getVarbit(), location,
				null, -1, location.getGardener(), -1));
		}
		for (HerbLocation location : HerbLocation.values())
		{
			all.add(new TrackedPatch(HerbTask.keyFor(location, -1), Type.HERB, PatchKind.HERB, location.getHerbVarbit(), null,
				location, -1, -1, -1));
			int[] varbits = location.getAllotmentVarbits();
			for (int i = 0; i < varbits.length; i++)
			{
				all.add(new TrackedPatch(HerbTask.keyFor(location, i), Type.ALLOTMENT, PatchKind.ALLOTMENT, varbits[i], null,
					location, i, location.getAllotmentGardener(), varbits.length > 1 ? location.getAllotmentOption(i) : -1));
			}
		}
		Map<String, TrackedPatch> byKey = new HashMap<>();
		Map<Integer, List<TrackedPatch>> byVarbit = new HashMap<>();
		for (TrackedPatch p : all)
		{
			byKey.put(p.key, p);
			byVarbit.computeIfAbsent(p.varbit, v -> new ArrayList<>()).add(p);
		}
		ALL = Collections.unmodifiableList(all);
		BY_KEY = Collections.unmodifiableMap(byKey);
		BY_VARBIT = Collections.unmodifiableMap(byVarbit);
	}
}
