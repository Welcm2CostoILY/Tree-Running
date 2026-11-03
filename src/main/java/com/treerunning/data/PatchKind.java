package com.treerunning.data;

/** How a patch's varbit is read. Each kind has its own table of states. */
public enum PatchKind
{
	TREE(PatchStateTable.TREE),
	FRUIT_TREE(PatchStateTable.FRUIT_TREE),
	HARDWOOD_TREE(PatchStateTable.HARDWOOD_TREE),
	REDWOOD(PatchStateTable.REDWOOD),
	CALQUAT(PatchStateTable.CALQUAT),
	CELASTRUS(PatchStateTable.CELASTRUS),
	CRYSTAL_TREE(PatchStateTable.CRYSTAL_TREE),
	HESPORI(PatchStateTable.HESPORI),
	HERB(PatchStateTable.HERB),
	ALLOTMENT(PatchStateTable.ALLOTMENT);

	private final int[] table;

	PatchKind(int[] table)
	{
		this.table = table;
	}

	/** What a varbit value means for this kind of patch. */
	public PatchReading read(int value)
	{
		if (value < 0 || value >= table.length || table[value] == 0)
		{
			return PatchReading.UNKNOWN;
		}
		int packed = table[value];
		PatchCondition condition = PatchCondition.values()[packed & 0xF];
		int stage = (packed >> 4) & 0x1F;
		int stages = (packed >> 9) & 0x1F;
		int minutes = (packed >> 14) & 0x3FF;
		String crop = PatchStateTable.NAMES[(packed >>> 24) & 0xFF];
		return new PatchReading(condition, stage, stages, minutes, crop);
	}
}
