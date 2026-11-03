package com.treerunning;

import com.treerunning.data.AllotmentType;
import com.treerunning.data.Compost;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchLocation;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Plugin settings, shown in RuneLite's configuration panel. The route editor (stop order,
 * travel notes and bank stops) is in the side panel instead, and saves to the hidden keys
 * at the bottom. Key names are kept from earlier versions so saved choices carry over.
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

	enum KandarinDiary
	{
		NONE("None", 0),
		MEDIUM("Medium (+5%)", 0.05),
		HARD("Hard (+10%)", 0.10),
		ELITE("Elite (+15%)", 0.15);

		private final String label;
		private final double bonus;

		KandarinDiary(String label, double bonus)
		{
			this.label = label;
			this.bonus = bonus;
		}

		public double bonus()
		{
			return bonus;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Which run the bank filter shows. Changed from the side panel or the bank. */
	enum BankFilter
	{
		OFF,
		TREE,
		HERB
	}

	/** The settings key for each tree patch's on/off switch. */
	Map<PatchLocation, String> PATCH_KEYS = Keys.patches();
	/** Settings keys for each herb location: on/off, herb, allotments. */
	Map<HerbLocation, String> HERB_ENABLED_KEYS = Keys.herb("herbPatch");
	Map<HerbLocation, String> HERB_CHOICE_KEYS = Keys.herb("herb");
	Map<HerbLocation, String> ALLOTMENT_KEYS = Keys.herb("allotments");

	final class Keys
	{
		private Keys()
		{
		}

		static Map<PatchLocation, String> patches()
		{
			Map<PatchLocation, String> m = new EnumMap<>(PatchLocation.class);
			m.put(PatchLocation.TREE_FARMING_GUILD, "treeFarmingGuild");
			m.put(PatchLocation.TREE_GNOME_STRONGHOLD, "treeGnomeStronghold");
			m.put(PatchLocation.TREE_FALADOR, "treeFalador");
			m.put(PatchLocation.TREE_TAVERLEY, "treeTaverley");
			m.put(PatchLocation.TREE_VARROCK, "treeVarrock");
			m.put(PatchLocation.TREE_LUMBRIDGE, "treeLumbridge");
			m.put(PatchLocation.TREE_NEMUS_RETREAT, "treeNemusRetreat");
			m.put(PatchLocation.FRUIT_FARMING_GUILD, "fruitFarmingGuild");
			m.put(PatchLocation.FRUIT_GNOME_STRONGHOLD, "fruitGnomeStronghold");
			m.put(PatchLocation.FRUIT_TREE_GNOME_VILLAGE, "fruitTreeGnomeVillage");
			m.put(PatchLocation.FRUIT_CATHERBY, "fruitCatherby");
			m.put(PatchLocation.FRUIT_BRIMHAVEN, "fruitBrimhaven");
			m.put(PatchLocation.FRUIT_KASTORI, "fruitKastori");
			m.put(PatchLocation.FRUIT_LLETYA, "fruitLletya");
			m.put(PatchLocation.CALQUAT_TAI_BWO_WANNAI, "calquatTaiBwoWannai");
			m.put(PatchLocation.CALQUAT_KASTORI, "calquatKastori");
			m.put(PatchLocation.CALQUAT_SUMMER_SHORE, "calquatSummerShore");
			m.put(PatchLocation.CELASTRUS_FARMING_GUILD, "celastrusFarmingGuild");
			m.put(PatchLocation.REDWOOD_FARMING_GUILD, "redwoodFarmingGuild");
			m.put(PatchLocation.HESPORI_FARMING_GUILD, "hesporiFarmingGuild");
			m.put(PatchLocation.HARDWOOD_LOCUS_OASIS, "hardwoodLocusOasis");
			m.put(PatchLocation.HARDWOOD_ANGLERS_RETREAT, "hardwoodAnglersRetreat");
			m.put(PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST, "hardwoodFossilEast");
			m.put(PatchLocation.HARDWOOD_FOSSIL_ISLAND_MIDDLE, "hardwoodFossilMiddle");
			m.put(PatchLocation.HARDWOOD_FOSSIL_ISLAND_WEST, "hardwoodFossilWest");
			m.put(PatchLocation.CRYSTAL_PRIFDDINAS, "crystalPrifddinas");
			return m;
		}

		static Map<HerbLocation, String> herb(String prefix)
		{
			Map<HerbLocation, String> m = new EnumMap<>(HerbLocation.class);
			m.put(HerbLocation.FARMING_GUILD, prefix + "FarmingGuild");
			m.put(HerbLocation.HOSIDIUS, prefix + "Hosidius");
			m.put(HerbLocation.ORTUS_FARM, prefix + "OrtusFarm");
			m.put(HerbLocation.CATHERBY, prefix + "Catherby");
			m.put(HerbLocation.ARDOUGNE, prefix + "Ardougne");
			m.put(HerbLocation.FALADOR, prefix + "Falador");
			m.put(HerbLocation.PORT_PHASMATYS, prefix + "PortPhasmatys");
			m.put(HerbLocation.HARMONY_ISLAND, prefix + "HarmonyIsland");
			m.put(HerbLocation.TROLL_STRONGHOLD, prefix + "TrollStronghold");
			m.put(HerbLocation.WEISS, prefix + "Weiss");
			return m;
		}
	}

	@ConfigSection(name = "Goal", description = "Goal", position = 0)
	String goalSection = "goalSection";

	@ConfigSection(name = "Tree crops", description = "Tree crops", position = 1)
	String treeCropSection = "treeCropSection";

	@ConfigSection(name = "Tree patches", description = "Tree patches", position = 2, closedByDefault = true)
	String treePatchSection = "treePatchSection";

	@ConfigSection(name = "Herb runs", description = "Herb runs", position = 3)
	String herbSection = "herbSection";

	@ConfigSection(name = "Herb patches", description = "Herb patches", position = 4, closedByDefault = true)
	String herbPatchSection = "herbPatchSection";

	@ConfigSection(name = "Overlay and highlights", description = "Overlay and highlights", position = 5)
	String overlaySection = "overlaySection";

	@ConfigSection(name = "Debug", description = "Debug", position = 6, closedByDefault = true)
	String debugSection = "debugSection";

	@ConfigItem(keyName = "goalType", name = "Goal type", description = "Aim for a level or an XP total", section = goalSection, position = 1)
	default GoalType goalType()
	{
		return GoalType.LEVEL;
	}

	@Range(min = 2, max = 99)
	@ConfigItem(keyName = "goalLevel", name = "Goal level", description = "Used when the goal type is Level", section = goalSection, position = 2)
	default int goalLevel()
	{
		return 99;
	}

	@Range(min = 1, max = 200_000_000)
	@ConfigItem(keyName = "goalXp", name = "Goal XP", description = "Used when the goal type is XP (up to 200,000,000; 99 is 13,034,431)", section = goalSection, position = 3)
	default int goalXp()
	{
		return 13_034_431;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(keyName = "runsPerDay", name = "Tree runs per day", description = "How many tree runs you do a day", section = goalSection, position = 4)
	default int runsPerDay()
	{
		return 1;
	}

	@Range(min = 0, max = 18)
	@ConfigItem(keyName = "herbRunsPerDay", name = "Herb runs per day", description = "Counted toward the goal date. 0 leaves herbs out", section = goalSection, position = 5)
	default int herbRunsPerDay()
	{
		return 0;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(keyName = "averageOver", name = "Average over last N runs", description = "How many recent tree runs the XP per run averages", section = goalSection, position = 6)
	default int averageOver()
	{
		return 5;
	}

	@ConfigItem(keyName = "treeCrop", name = "Tree patches", description = "What you plant in tree patches", section = treeCropSection, position = 7)
	default CropChoices.Tree treeCrop()
	{
		return CropChoices.Tree.YEW;
	}

	@ConfigItem(keyName = "fruitCrop", name = "Fruit tree patches", description = "What you plant in fruit tree patches", section = treeCropSection, position = 8)
	default CropChoices.Fruit fruitCrop()
	{
		return CropChoices.Fruit.PAPAYA;
	}

	@ConfigItem(keyName = "hardwoodCrop", name = "Hardwood patches", description = "What you plant in hardwood patches", section = treeCropSection, position = 9)
	default CropChoices.Hardwood hardwoodCrop()
	{
		return CropChoices.Hardwood.IRONWOOD;
	}

	@ConfigItem(keyName = "treeFarmingGuild", name = "Tree: Farming Guild", description = "Include this patch in your tree runs", section = treePatchSection, position = 10)
	default boolean treeFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "treeGnomeStronghold", name = "Tree: Gnome Stronghold", description = "Include this patch in your tree runs", section = treePatchSection, position = 11)
	default boolean treeGnomeStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "treeFalador", name = "Tree: Falador Park", description = "Include this patch in your tree runs", section = treePatchSection, position = 12)
	default boolean treeFalador()
	{
		return true;
	}

	@ConfigItem(keyName = "treeTaverley", name = "Tree: Taverley", description = "Include this patch in your tree runs", section = treePatchSection, position = 13)
	default boolean treeTaverley()
	{
		return true;
	}

	@ConfigItem(keyName = "treeVarrock", name = "Tree: Varrock Castle", description = "Include this patch in your tree runs", section = treePatchSection, position = 14)
	default boolean treeVarrock()
	{
		return true;
	}

	@ConfigItem(keyName = "treeLumbridge", name = "Tree: Lumbridge", description = "Include this patch in your tree runs", section = treePatchSection, position = 15)
	default boolean treeLumbridge()
	{
		return true;
	}

	@ConfigItem(keyName = "treeNemusRetreat", name = "Tree: Nemus Retreat", description = "Include this patch in your tree runs", section = treePatchSection, position = 16)
	default boolean treeNemusRetreat()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitFarmingGuild", name = "Fruit: Farming Guild", description = "Include this patch in your tree runs", section = treePatchSection, position = 17)
	default boolean fruitFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitGnomeStronghold", name = "Fruit: Gnome Stronghold", description = "Include this patch in your tree runs", section = treePatchSection, position = 18)
	default boolean fruitGnomeStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitTreeGnomeVillage", name = "Fruit: Tree Gnome Village", description = "Include this patch in your tree runs", section = treePatchSection, position = 19)
	default boolean fruitTreeGnomeVillage()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitCatherby", name = "Fruit: Catherby", description = "Include this patch in your tree runs", section = treePatchSection, position = 20)
	default boolean fruitCatherby()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitBrimhaven", name = "Fruit: Brimhaven", description = "Include this patch in your tree runs", section = treePatchSection, position = 21)
	default boolean fruitBrimhaven()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitKastori", name = "Fruit: Kastori", description = "Include this patch in your tree runs", section = treePatchSection, position = 22)
	default boolean fruitKastori()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitLletya", name = "Fruit: Lletya", description = "Include this patch in your tree runs", section = treePatchSection, position = 23)
	default boolean fruitLletya()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatTaiBwoWannai", name = "Calquat: Tai Bwo Wannai", description = "Include this patch in your tree runs", section = treePatchSection, position = 24)
	default boolean calquatTaiBwoWannai()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatKastori", name = "Calquat: Kastori", description = "Include this patch in your tree runs", section = treePatchSection, position = 25)
	default boolean calquatKastori()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatSummerShore", name = "Calquat: The Summer Shore", description = "Include this patch in your tree runs", section = treePatchSection, position = 26)
	default boolean calquatSummerShore()
	{
		return true;
	}

	@ConfigItem(keyName = "celastrusFarmingGuild", name = "Celastrus: Farming Guild", description = "Include this patch in your tree runs", section = treePatchSection, position = 27)
	default boolean celastrusFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "redwoodFarmingGuild", name = "Redwood: Farming Guild", description = "Include this patch in your tree runs", section = treePatchSection, position = 28)
	default boolean redwoodFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "hesporiFarmingGuild", name = "Hespori: Farming Guild", description = "Include this patch in your tree runs", section = treePatchSection, position = 29)
	default boolean hesporiFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodLocusOasis", name = "Hardwood: Locus Oasis", description = "Include this patch in your tree runs", section = treePatchSection, position = 30)
	default boolean hardwoodLocusOasis()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodAnglersRetreat", name = "Hardwood: Anglers' Retreat", description = "Include this patch in your tree runs", section = treePatchSection, position = 31)
	default boolean hardwoodAnglersRetreat()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilEast", name = "Hardwood: Fossil Island East", description = "Include this patch in your tree runs", section = treePatchSection, position = 32)
	default boolean hardwoodFossilEast()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilMiddle", name = "Hardwood: Fossil Island Middle", description = "Include this patch in your tree runs", section = treePatchSection, position = 33)
	default boolean hardwoodFossilMiddle()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilWest", name = "Hardwood: Fossil Island West", description = "Include this patch in your tree runs", section = treePatchSection, position = 34)
	default boolean hardwoodFossilWest()
	{
		return true;
	}

	@ConfigItem(keyName = "crystalPrifddinas", name = "Crystal tree: Prifddinas", description = "Include this patch in your tree runs", section = treePatchSection, position = 35)
	default boolean crystalPrifddinas()
	{
		return false;
	}

	@ConfigItem(keyName = "compost", name = "Compost", description = "What you compost herb and allotment patches with", section = herbSection, position = 36)
	default Compost compost()
	{
		return Compost.BOTTOMLESS;
	}

	@ConfigItem(keyName = "ironmanSeeds", name = "Don't count seed costs (ironman)", description = "For accounts that can't buy seeds: profit counts the harvest only, minus compost and payments. Applies to herb, allotment and tree runs", section = herbSection, position = 45)
	default boolean ironmanSeeds()
	{
		return false;
	}

	@ConfigItem(keyName = "magicSecateurs", name = "Magic secateurs", description = "Count +10% herbs in estimates. The run warns you if they aren't with you", section = herbSection, position = 37)
	default boolean magicSecateurs()
	{
		return true;
	}

	@ConfigItem(keyName = "farmingCape", name = "Farming cape", description = "Count +5% herbs in estimates (worn)", section = herbSection, position = 38)
	default boolean farmingCape()
	{
		return false;
	}

	@ConfigItem(keyName = "kandarinDiary", name = "Kandarin diary", description = "Extra herbs at Catherby", section = herbSection, position = 39)
	default KandarinDiary kandarinDiary()
	{
		return KandarinDiary.NONE;
	}

	@ConfigItem(keyName = "kourendHard", name = "Hard Kourend & Kebos diary", description = "+5% herbs at Hosidius and the Farming Guild", section = herbSection, position = 40)
	default boolean kourendHard()
	{
		return false;
	}

	@ConfigItem(keyName = "allotmentCrop", name = "Allotment crop", description = "What you plant in allotments you turn on", section = herbSection, position = 41)
	default AllotmentType allotmentCrop()
	{
		return AllotmentType.SNAPE_GRASS;
	}

	@ConfigItem(keyName = "payForAllotments", name = "Pay to protect allotments", description = "Adds a pay step and the payment to bring", section = herbSection, position = 42)
	default boolean payForAllotments()
	{
		return false;
	}

	@ConfigItem(keyName = "herbNotify", name = "Notify when herbs are grown", description = "A RuneLite notification when all your herb patches should be grown", section = herbSection, position = 43)
	default boolean herbNotify()
	{
		return true;
	}

	@ConfigItem(keyName = "herbsOnTheWay", name = "Herbs on the way", description = "Add grown herb patches at Farming Guild, Catherby and Falador to tree-run stops", section = herbSection, position = 44)
	default boolean herbsOnTheWay()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchFarmingGuild", name = "Farming Guild: use", description = "Include the Farming Guild herb patch in herb runs", section = herbPatchSection, position = 45)
	default boolean herbPatchFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "herbFarmingGuild", name = "Farming Guild: herb", description = "The herb you plant at Farming Guild", section = herbPatchSection, position = 46)
	default HerbType herbFarmingGuild()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsFarmingGuild", name = "Farming Guild: allotments", description = "Also do the allotments at Farming Guild", section = herbPatchSection, position = 47)
	default boolean allotmentsFarmingGuild()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchHosidius", name = "Hosidius: use", description = "Include the Hosidius herb patch in herb runs", section = herbPatchSection, position = 48)
	default boolean herbPatchHosidius()
	{
		return true;
	}

	@ConfigItem(keyName = "herbHosidius", name = "Hosidius: herb", description = "The herb you plant at Hosidius", section = herbPatchSection, position = 49)
	default HerbType herbHosidius()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsHosidius", name = "Hosidius: allotments", description = "Also do the allotments at Hosidius", section = herbPatchSection, position = 50)
	default boolean allotmentsHosidius()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchOrtusFarm", name = "Ortus Farm: use", description = "Include the Ortus Farm herb patch in herb runs", section = herbPatchSection, position = 51)
	default boolean herbPatchOrtusFarm()
	{
		return true;
	}

	@ConfigItem(keyName = "herbOrtusFarm", name = "Ortus Farm: herb", description = "The herb you plant at Ortus Farm", section = herbPatchSection, position = 52)
	default HerbType herbOrtusFarm()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsOrtusFarm", name = "Ortus Farm: allotments", description = "Also do the allotments at Ortus Farm", section = herbPatchSection, position = 53)
	default boolean allotmentsOrtusFarm()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchCatherby", name = "Catherby: use", description = "Include the Catherby herb patch in herb runs", section = herbPatchSection, position = 54)
	default boolean herbPatchCatherby()
	{
		return true;
	}

	@ConfigItem(keyName = "herbCatherby", name = "Catherby: herb", description = "The herb you plant at Catherby", section = herbPatchSection, position = 55)
	default HerbType herbCatherby()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsCatherby", name = "Catherby: allotments", description = "Also do the allotments at Catherby", section = herbPatchSection, position = 56)
	default boolean allotmentsCatherby()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchArdougne", name = "Ardougne: use", description = "Include the Ardougne herb patch in herb runs", section = herbPatchSection, position = 57)
	default boolean herbPatchArdougne()
	{
		return true;
	}

	@ConfigItem(keyName = "herbArdougne", name = "Ardougne: herb", description = "The herb you plant at Ardougne", section = herbPatchSection, position = 58)
	default HerbType herbArdougne()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsArdougne", name = "Ardougne: allotments", description = "Also do the allotments at Ardougne", section = herbPatchSection, position = 59)
	default boolean allotmentsArdougne()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchFalador", name = "Falador: use", description = "Include the Falador herb patch in herb runs", section = herbPatchSection, position = 60)
	default boolean herbPatchFalador()
	{
		return true;
	}

	@ConfigItem(keyName = "herbFalador", name = "Falador: herb", description = "The herb you plant at Falador", section = herbPatchSection, position = 61)
	default HerbType herbFalador()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsFalador", name = "Falador: allotments", description = "Also do the allotments at Falador", section = herbPatchSection, position = 62)
	default boolean allotmentsFalador()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchPortPhasmatys", name = "Port Phasmatys: use", description = "Include the Port Phasmatys herb patch in herb runs", section = herbPatchSection, position = 63)
	default boolean herbPatchPortPhasmatys()
	{
		return true;
	}

	@ConfigItem(keyName = "herbPortPhasmatys", name = "Port Phasmatys: herb", description = "The herb you plant at Port Phasmatys", section = herbPatchSection, position = 64)
	default HerbType herbPortPhasmatys()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsPortPhasmatys", name = "Port Phasmatys: allotments", description = "Also do the allotments at Port Phasmatys", section = herbPatchSection, position = 65)
	default boolean allotmentsPortPhasmatys()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchHarmonyIsland", name = "Harmony Island: use", description = "Include the Harmony Island herb patch in herb runs", section = herbPatchSection, position = 66)
	default boolean herbPatchHarmonyIsland()
	{
		return false;
	}

	@ConfigItem(keyName = "herbHarmonyIsland", name = "Harmony Island: herb", description = "The herb you plant at Harmony Island", section = herbPatchSection, position = 67)
	default HerbType herbHarmonyIsland()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "allotmentsHarmonyIsland", name = "Harmony Island: allotments", description = "Also do the allotments at Harmony Island", section = herbPatchSection, position = 68)
	default boolean allotmentsHarmonyIsland()
	{
		return false;
	}

	@ConfigItem(keyName = "herbPatchTrollStronghold", name = "Troll Stronghold: use", description = "Include the Troll Stronghold herb patch in herb runs", section = herbPatchSection, position = 69)
	default boolean herbPatchTrollStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "herbTrollStronghold", name = "Troll Stronghold: herb", description = "The herb you plant at Troll Stronghold", section = herbPatchSection, position = 70)
	default HerbType herbTrollStronghold()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "herbPatchWeiss", name = "Weiss: use", description = "Include the Weiss herb patch in herb runs", section = herbPatchSection, position = 71)
	default boolean herbPatchWeiss()
	{
		return true;
	}

	@ConfigItem(keyName = "herbWeiss", name = "Weiss: herb", description = "The herb you plant at Weiss", section = herbPatchSection, position = 72)
	default HerbType herbWeiss()
	{
		return HerbType.RANARR;
	}

	@ConfigItem(keyName = "showOverlay", name = "Show checklist overlay", description = "The current stop and its patches, in game", section = overlaySection, position = 73)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(keyName = "overlayOnlyDuringRun", name = "Only during a run", description = "Hide the overlay between runs", section = overlaySection, position = 74)
	default boolean overlayOnlyDuringRun()
	{
		return false;
	}

	@ConfigItem(keyName = "highlightPatches", name = "Highlight patches", description = "Outline due patches in game. The next one is bright green", section = overlaySection, position = 75)
	default boolean highlightPatches()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightGardeners", name = "Highlight unpaid gardeners", description = "Outline the gardener of a patch you've planted but not paid for", section = overlaySection, position = 76)
	default boolean highlightGardeners()
	{
		return true;
	}

	@ConfigItem(keyName = "hintArrow", name = "Hint arrow to next patch", description = "Point the game's hint arrow at the next tree patch", section = overlaySection, position = 77)
	default boolean hintArrow()
	{
		return false;
	}

	@ConfigItem(keyName = "debugLog", name = "Debug log", description = "Write what the tracker sees to RuneLite's client.log ([Tree Running] lines)", section = debugSection, position = 78)
	default boolean debugLog()
	{
		return false;
	}

	@ConfigItem(keyName = "bankFilter", name = "bankFilter", description = "", hidden = true)
	default BankFilter bankFilter()
	{
		return BankFilter.OFF;
	}

	@ConfigItem(keyName = "stopOrder", name = "stopOrder", description = "", hidden = true)
	default String stopOrder()
	{
		return "[]";
	}

	@ConfigItem(keyName = "patchOrder", name = "patchOrder", description = "", hidden = true)
	default String patchOrder()
	{
		return "{}";
	}

	@ConfigItem(keyName = "travelText", name = "travelText", description = "", hidden = true)
	default String travelText()
	{
		return "{}";
	}

	@ConfigItem(keyName = "bankStops", name = "bankStops", description = "", hidden = true)
	default String bankStops()
	{
		return "[]";
	}
}
