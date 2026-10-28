# Tree Running

A RuneLite plugin for daily tree runs toward a Farming goal: a level (99 by default) or any XP total up to 200M.

- **XP tab**: runs, days and a goal date. It starts from your chosen crops and switches to the average of your real runs once you finish one. All settings live here too.
- **Run tab**: a "This run needs" box with the saplings and protection items for the patches due today, color-coded by patch type, then the checklist in route order with your bank stops.
- **Supplies tab**: seeds/saplings and protection payments needed for the runs you have left, against your bank, the Farming Guild seed vault and your inventory, with the live OSRS Wiki GE price of each and the cost of what you're short.
- **Checklist overlay**: the current stop, how to get there, and its patches and bank stops in order.
- **Exact patch tracking**: planting ticks off the patch you actually planted, in any order. Opening your bank ticks the next bank stop.
- **Route & patches**: a checkbox for every patch (25 in all, including Anglers' Retreat and each Fossil Island patch), and bank stops you can add at any stop, before or after its patches.

Built and compiled against RuneLite 1.13.4.

## Run it

1. Install **JDK 11 or newer** and **IntelliJ IDEA** (Community is fine).
2. In IntelliJ: **File → Open** and pick this folder. Let it import the Gradle project (it downloads RuneLite automatically).
3. Open `src/test/java/com/treerunning/TreeRunningPluginTest.java` and run its `main` method.
   - Edit the run configuration: **VM options** `-ea`, **Program arguments** `--developer-mode`.
4. RuneLite opens with the plugin loaded. Find **Tree Running** in the plugin list and the tree icon in the side bar.

From a terminal, `./gradlew runClient` (or `gradlew.bat runClient` on Windows) does the same thing.

## First-time setup in game

1. Open the **XP** tab and scroll to **Settings**. Pick your crops and your goal, untick any patches you can't use, and add bank stops where you restock.
2. **Open your bank and the Farming Guild seed vault once.** That's how the supply tracker learns what you have. Each updates every time you open it.
3. Do your run. Planting a sapling ticks off that patch, and opening your bank ticks the next bank stop.

## How it tracks things

- **Planting**: when a sapling (or Hespori seed) leaves your inventory and Farming XP arrives within two game ticks, it counts as planted.
- **Which patch**: the plugin watches each patch's game state (its varbit) and the map region you're standing in, using the same patch data as RuneLite's built-in Time Tracking plugin. The patch whose state changed as you planted gets ticked, so you can do the stops in any order. The three Fossil Island patches are told apart the same way. If it can't place you (somewhere it has no data for), it falls back to the next patch of that type in route order and says so in the Run tab.
- **Checking it**: the Run tab shows "Last planted:" with the patch it ticked. You can still tick or untick any box by hand.
- **Runs**: a run starts at your first plant and includes the check-health XP from the few minutes before it. It ends when every due patch is ticked, or when you press **Finish run**. Run XP is all Farming XP gained during the run.
- **Long-growing patches**: hardwood (85.3 h for ironwood), redwood (106.7 h) and Hespori (32 h) are timed from when you planted them. They show "Ready in…" until they're grown.
- **XP countdown**: it averages your last 5 runs by default (setting: *Average over last N runs*). Before you've finished a run, it uses the plan.
- **Bank stops**: opening your bank during a run ticks the next bank stop, but only when a bank stop is the next thing on the route. You can always tick it by hand.
- **Prices**: the OSRS Wiki's live prices, which RuneLite downloads and refreshes itself. Hespori seeds can't be traded, so they show "–".
- **Saved data**: progress, run history and the bank and seed vault counts are saved per account in your RuneLite profile. Settings and bank stops are saved in your RuneLite settings.

## Preview

`preview/tree-running-preview.html` is a clickable mock of the panel and overlay. It uses the same crop data and the same planner math, ported to JavaScript.

## For development

- `./gradlew test`: unit tests for the planner (XP table, run projection, supplies and prices, route order with bank stops, run needs) and patch matching.
- `./gradlew panelSnapshot`: renders the side panel tabs to `build/snapshots/` without starting the client.
- `./gradlew exportPreviewData`: regenerates the data the preview uses.

## Icons

- `src/main/resources/com/treerunning/panel_icon.png`: the 16×16 walking tree in the side bar.
- `icon.png`: the 48×72 Plugin Hub icon.

## Data sources

Levels, XP, payments and growth times come from the OSRS Wiki (Calculator:Farming/Trees and the patch location pages). Item IDs come from RuneLite's own item list. Patch regions and varbits come from RuneLite's Time Tracking plugin (FarmingWorld).
