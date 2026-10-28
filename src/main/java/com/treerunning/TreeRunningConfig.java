package com.treerunning;

import com.treerunning.data.PatchLocation;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

/**
 * Plugin settings. They're all changed from the Settings section of the XP tab, so they're
 * hidden from RuneLite's own settings list. Key names are kept from earlier versions so
 * saved choices carry over.
 */
@ConfigGroup(TreeRunningConfig.GROUP)
public interface TreeRunningConfig extends Config
{
	String GROUP = "treerunning";

	enum GoalType
	{
		LEVEL("Level"),
		XP("XP");

		private final String label;

		GoalType(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** The settings key for each patch's on/off switch. */
	Map<PatchLocation, String> PATCH_KEYS = Keys.build();

	final class Keys
	{
		private static final Map<PatchLocation, String> PATCH_KEYS = new EnumMap<>(PatchLocation.class);

		private Keys()
		{
		}

		static Map<PatchLocation, String> build()
		{
		PATCH_KEYS.put(PatchLocation.TREE_FARMING_GUILD, "treeFarmingGuild");
		PATCH_KEYS.put(PatchLocation.TREE_GNOME_STRONGHOLD, "treeGnomeStronghold");
		PATCH_KEYS.put(PatchLocation.TREE_FALADOR, "treeFalador");
		PATCH_KEYS.put(PatchLocation.TREE_TAVERLEY, "treeTaverley");
		PATCH_KEYS.put(PatchLocation.TREE_VARROCK, "treeVarrock");
		PATCH_KEYS.put(PatchLocation.TREE_LUMBRIDGE, "treeLumbridge");
		PATCH_KEYS.put(PatchLocation.TREE_NEMUS_RETREAT, "treeNemusRetreat");
		PATCH_KEYS.put(PatchLocation.FRUIT_FARMING_GUILD, "fruitFarmingGuild");
		PATCH_KEYS.put(PatchLocation.FRUIT_GNOME_STRONGHOLD, "fruitGnomeStronghold");
		PATCH_KEYS.put(PatchLocation.FRUIT_TREE_GNOME_VILLAGE, "fruitTreeGnomeVillage");
		PATCH_KEYS.put(PatchLocation.FRUIT_CATHERBY, "fruitCatherby");
		PATCH_KEYS.put(PatchLocation.FRUIT_BRIMHAVEN, "fruitBrimhaven");
		PATCH_KEYS.put(PatchLocation.FRUIT_KASTORI, "fruitKastori");
		PATCH_KEYS.put(PatchLocation.FRUIT_LLETYA, "fruitLletya");
		PATCH_KEYS.put(PatchLocation.CALQUAT_TAI_BWO_WANNAI, "calquatTaiBwoWannai");
		PATCH_KEYS.put(PatchLocation.CALQUAT_KASTORI, "calquatKastori");
		PATCH_KEYS.put(PatchLocation.CALQUAT_SUMMER_SHORE, "calquatSummerShore");
		PATCH_KEYS.put(PatchLocation.CELASTRUS_FARMING_GUILD, "celastrusFarmingGuild");
		PATCH_KEYS.put(PatchLocation.REDWOOD_FARMING_GUILD, "redwoodFarmingGuild");
		PATCH_KEYS.put(PatchLocation.HESPORI_FARMING_GUILD, "hesporiFarmingGuild");
		PATCH_KEYS.put(PatchLocation.HARDWOOD_LOCUS_OASIS, "hardwoodLocusOasis");
		PATCH_KEYS.put(PatchLocation.HARDWOOD_ANGLERS_RETREAT, "hardwoodAnglersRetreat");
		PATCH_KEYS.put(PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST, "hardwoodFossilEast");
		PATCH_KEYS.put(PatchLocation.HARDWOOD_FOSSIL_ISLAND_MIDDLE, "hardwoodFossilMiddle");
		PATCH_KEYS.put(PatchLocation.HARDWOOD_FOSSIL_ISLAND_WEST, "hardwoodFossilWest");
			return PATCH_KEYS;
		}
	}

	@ConfigItem(keyName = "treeCrop", name = "Tree patches", description = "", hidden = true)
	default CropChoices.Tree treeCrop()
	{
		return CropChoices.Tree.YEW;
	}

	@ConfigItem(keyName = "fruitCrop", name = "Fruit tree patches", description = "", hidden = true)
	default CropChoices.Fruit fruitCrop()
	{
		return CropChoices.Fruit.PAPAYA;
	}

	@ConfigItem(keyName = "hardwoodCrop", name = "Hardwood patches", description = "", hidden = true)
	default CropChoices.Hardwood hardwoodCrop()
	{
		return CropChoices.Hardwood.IRONWOOD;
	}

	@ConfigItem(keyName = "goalType", name = "Goal type", description = "", hidden = true)
	default GoalType goalType()
	{
		return GoalType.LEVEL;
	}

	@Range(min = 2, max = 99)

	@ConfigItem(keyName = "goalLevel", name = "Goal level", description = "", hidden = true)
	default int goalLevel()
	{
		return 99;
	}

	@Range(min = 1, max = 200_000_000)

	@ConfigItem(keyName = "goalXp", name = "Goal XP", description = "", hidden = true)
	default int goalXp()
	{
		return 13_034_431;
	}

	@Range(min = 1, max = 4)

	@ConfigItem(keyName = "runsPerDay", name = "Runs per day", description = "", hidden = true)
	default int runsPerDay()
	{
		return 1;
	}

	@Range(min = 1, max = 20)

	@ConfigItem(keyName = "averageOver", name = "Average over last N runs", description = "", hidden = true)
	default int averageOver()
	{
		return 5;
	}

	@ConfigItem(keyName = "showOverlay", name = "Show checklist overlay", description = "", hidden = true)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(keyName = "overlayOnlyDuringRun", name = "Only during a run", description = "", hidden = true)
	default boolean overlayOnlyDuringRun()
	{
		return false;
	}

	@ConfigItem(keyName = "bankStops", name = "Bank stops", description = "", hidden = true)
	default String bankStops()
	{
		return "[]";
	}

	@ConfigItem(keyName = "treeFarmingGuild", name = "treeFarmingGuild", description = "", hidden = true)
	default boolean treeFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "treeGnomeStronghold", name = "treeGnomeStronghold", description = "", hidden = true)
	default boolean treeGnomeStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "treeFalador", name = "treeFalador", description = "", hidden = true)
	default boolean treeFalador()
	{
		return true;
	}

	@ConfigItem(keyName = "treeTaverley", name = "treeTaverley", description = "", hidden = true)
	default boolean treeTaverley()
	{
		return true;
	}

	@ConfigItem(keyName = "treeVarrock", name = "treeVarrock", description = "", hidden = true)
	default boolean treeVarrock()
	{
		return true;
	}

	@ConfigItem(keyName = "treeLumbridge", name = "treeLumbridge", description = "", hidden = true)
	default boolean treeLumbridge()
	{
		return true;
	}

	@ConfigItem(keyName = "treeNemusRetreat", name = "treeNemusRetreat", description = "", hidden = true)
	default boolean treeNemusRetreat()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitFarmingGuild", name = "fruitFarmingGuild", description = "", hidden = true)
	default boolean fruitFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitGnomeStronghold", name = "fruitGnomeStronghold", description = "", hidden = true)
	default boolean fruitGnomeStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitTreeGnomeVillage", name = "fruitTreeGnomeVillage", description = "", hidden = true)
	default boolean fruitTreeGnomeVillage()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitCatherby", name = "fruitCatherby", description = "", hidden = true)
	default boolean fruitCatherby()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitBrimhaven", name = "fruitBrimhaven", description = "", hidden = true)
	default boolean fruitBrimhaven()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitKastori", name = "fruitKastori", description = "", hidden = true)
	default boolean fruitKastori()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitLletya", name = "fruitLletya", description = "", hidden = true)
	default boolean fruitLletya()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatTaiBwoWannai", name = "calquatTaiBwoWannai", description = "", hidden = true)
	default boolean calquatTaiBwoWannai()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatKastori", name = "calquatKastori", description = "", hidden = true)
	default boolean calquatKastori()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatSummerShore", name = "calquatSummerShore", description = "", hidden = true)
	default boolean calquatSummerShore()
	{
		return true;
	}

	@ConfigItem(keyName = "celastrusFarmingGuild", name = "celastrusFarmingGuild", description = "", hidden = true)
	default boolean celastrusFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "redwoodFarmingGuild", name = "redwoodFarmingGuild", description = "", hidden = true)
	default boolean redwoodFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "hesporiFarmingGuild", name = "hesporiFarmingGuild", description = "", hidden = true)
	default boolean hesporiFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodLocusOasis", name = "hardwoodLocusOasis", description = "", hidden = true)
	default boolean hardwoodLocusOasis()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodAnglersRetreat", name = "hardwoodAnglersRetreat", description = "", hidden = true)
	default boolean hardwoodAnglersRetreat()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilEast", name = "hardwoodFossilEast", description = "", hidden = true)
	default boolean hardwoodFossilEast()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilMiddle", name = "hardwoodFossilMiddle", description = "", hidden = true)
	default boolean hardwoodFossilMiddle()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilWest", name = "hardwoodFossilWest", description = "", hidden = true)
	default boolean hardwoodFossilWest()
	{
		return true;
	}
}
