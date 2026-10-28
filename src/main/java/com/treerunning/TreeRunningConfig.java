package com.treerunning;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(TreeRunningConfig.GROUP)
public interface TreeRunningConfig extends Config
{
	String GROUP = "treerunning";

	// ---------------------------------------------------------------- Crops

	@ConfigSection(name = "Crops", description = "What to plant in each patch type", position = 0)
	String crops = "crops";

	@ConfigItem(keyName = "treeCrop", name = "Tree patches", description = "Tree planted in all tree patches",
		position = 0, section = crops)
	default CropChoices.Tree treeCrop()
	{
		return CropChoices.Tree.YEW;
	}

	@ConfigItem(keyName = "fruitCrop", name = "Fruit tree patches", description = "Fruit tree planted in all fruit tree patches",
		position = 1, section = crops)
	default CropChoices.Fruit fruitCrop()
	{
		return CropChoices.Fruit.PAPAYA;
	}

	@ConfigItem(keyName = "hardwoodCrop", name = "Hardwood patches", description = "Hardwood tree planted in hardwood patches",
		position = 2, section = crops)
	default CropChoices.Hardwood hardwoodCrop()
	{
		return CropChoices.Hardwood.IRONWOOD;
	}





	// ---------------------------------------------------------------- Goal

	@ConfigSection(name = "Goal", description = "Your Farming goal", position = 1)
	String goal = "goal";

	@Range(min = 2, max = 99)
	@ConfigItem(keyName = "goalLevel", name = "Goal level", description = "Farming level you're working toward", position = 0, section = goal)
	default int goalLevel()
	{
		return 99;
	}

	@Range(min = 1, max = 4)
	@ConfigItem(keyName = "runsPerDay", name = "Runs per day", description = "How many tree runs you do a day (used for ETA and growth timing)",
		position = 1, section = goal)
	default int runsPerDay()
	{
		return 1;
	}

	@Range(min = 1, max = 20)
	@ConfigItem(keyName = "averageOver", name = "Average over last N runs",
		description = "How many recent runs the XP countdown averages", position = 2, section = goal)
	default int averageOver()
	{
		return 5;
	}

	// ---------------------------------------------------------------- Inventory

	@ConfigSection(name = "Inventory", description = "Inventory plan settings", position = 2)
	String inventory = "inventory";

	@Range(min = 0, max = 20)
	@ConfigItem(keyName = "reservedSlots", name = "Always-carried slots",
		description = "Slots for your own items on every run (spade, coins, house teleport, etc.). Teleport items the route needs are added automatically.",
		position = 0, section = inventory)
	default int reservedSlots()
	{
		return 3;
	}

	@ConfigItem(keyName = "pickProduce", name = "Pick fruit",
		description = "Plan space for picking fruit and noting it at the leprechaun (the leprechaun notes, it doesn't bank)",
		position = 1, section = inventory)
	default boolean pickProduce()
	{
		return true;
	}

	// ---------------------------------------------------------------- Overlay

	@ConfigSection(name = "Overlay", description = "In-game checklist overlay", position = 3)
	String overlay = "overlay";

	@ConfigItem(keyName = "showOverlay", name = "Show checklist overlay", description = "Show the current stop and its patches in game",
		position = 0, section = overlay)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(keyName = "overlayOnlyDuringRun", name = "Only during a run", description = "Hide the overlay when no run is in progress",
		position = 1, section = overlay)
	default boolean overlayOnlyDuringRun()
	{
		return false;
	}

	// ---------------------------------------------------------------- Patches

	@ConfigSection(name = "Patches", description = "Every patch on the run. Untick any you can't or don't want to use.", position = 4)
	String patches = "patches";

	@ConfigItem(keyName = "treeFarmingGuild", name = "Tree: Farming Guild", description = "Needs 65 Farming", position = 0, section = patches)
	default boolean treeFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "treeGnomeStronghold", name = "Tree: Gnome Stronghold", description = "", position = 1, section = patches)
	default boolean treeGnomeStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "treeFalador", name = "Tree: Falador Park", description = "", position = 2, section = patches)
	default boolean treeFalador()
	{
		return true;
	}

	@ConfigItem(keyName = "treeTaverley", name = "Tree: Taverley", description = "66 Agility for the Falador shortcut", position = 3, section = patches)
	default boolean treeTaverley()
	{
		return true;
	}

	@ConfigItem(keyName = "treeVarrock", name = "Tree: Varrock", description = "", position = 4, section = patches)
	default boolean treeVarrock()
	{
		return true;
	}

	@ConfigItem(keyName = "treeLumbridge", name = "Tree: Lumbridge", description = "", position = 5, section = patches)
	default boolean treeLumbridge()
	{
		return true;
	}

	@ConfigItem(keyName = "treeNemusRetreat", name = "Tree: Nemus Retreat", description = "Children of the Sun", position = 6, section = patches)
	default boolean treeNemusRetreat()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitFarmingGuild", name = "Fruit: Farming Guild", description = "85 Farming", position = 7, section = patches)
	default boolean fruitFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitGnomeStronghold", name = "Fruit: Gnome Stronghold", description = "", position = 8, section = patches)
	default boolean fruitGnomeStronghold()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitTreeGnomeVillage", name = "Fruit: Tree Gnome Village", description = "", position = 9, section = patches)
	default boolean fruitTreeGnomeVillage()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitCatherby", name = "Fruit: Catherby", description = "", position = 10, section = patches)
	default boolean fruitCatherby()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitBrimhaven", name = "Fruit: Brimhaven", description = "", position = 11, section = patches)
	default boolean fruitBrimhaven()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitKastori", name = "Fruit: Kastori", description = "Children of the Sun", position = 12, section = patches)
	default boolean fruitKastori()
	{
		return true;
	}

	@ConfigItem(keyName = "fruitLletya", name = "Fruit: Lletya", description = "Mourning's End Part I (partial)", position = 13, section = patches)
	default boolean fruitLletya()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatTaiBwoWannai", name = "Calquat: Tai Bwo Wannai", description = "", position = 14, section = patches)
	default boolean calquatTaiBwoWannai()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatKastori", name = "Calquat: Kastori", description = "Children of the Sun", position = 15, section = patches)
	default boolean calquatKastori()
	{
		return true;
	}

	@ConfigItem(keyName = "calquatSummerShore", name = "Calquat: Summer Shore", description = "Troubled Tortugans (partial)", position = 16, section = patches)
	default boolean calquatSummerShore()
	{
		return true;
	}

	@ConfigItem(keyName = "celastrusFarmingGuild", name = "Celastrus: Farming Guild", description = "85 Farming", position = 17, section = patches)
	default boolean celastrusFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "redwoodFarmingGuild", name = "Redwood: Farming Guild", description = "90 Farming", position = 18, section = patches)
	default boolean redwoodFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "hesporiFarmingGuild", name = "Hespori: Farming Guild", description = "65 Farming; seeds come from seed packs",
		position = 19, section = patches)
	default boolean hesporiFarmingGuild()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodLocusOasis", name = "Hardwood: Locus Oasis",
		description = "The Ribbiting Tale of a Lily Pad Labour Dispute", position = 20, section = patches)
	default boolean hardwoodLocusOasis()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodAnglersRetreat", name = "Hardwood: Anglers' Retreat", description = "51 Sailing",
		position = 21, section = patches)
	default boolean hardwoodAnglersRetreat()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilEast", name = "Hardwood: Fossil Island east", description = "Bone Voyage", position = 22, section = patches)
	default boolean hardwoodFossilEast()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilMiddle", name = "Hardwood: Fossil Island middle", description = "Bone Voyage", position = 23, section = patches)
	default boolean hardwoodFossilMiddle()
	{
		return true;
	}

	@ConfigItem(keyName = "hardwoodFossilWest", name = "Hardwood: Fossil Island west", description = "Bone Voyage", position = 24, section = patches)
	default boolean hardwoodFossilWest()
	{
		return true;
	}
}
