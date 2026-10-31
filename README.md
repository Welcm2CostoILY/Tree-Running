# Tree Running

A RuneLite plugin for daily tree runs toward a Farming goal: a level (99 by default) or any XP total up to 200M.

- **XP tab**: runs, days and a goal date. It starts from your chosen crops and switches to the average of your real runs once you finish one. Run history with a delete button per run. All settings live here too.
- **Run tab**: a "This run needs" box with the saplings and protection items still to use, against what's in your inventory (red when you're short), then the checklist in route order with your bank stops.
- **Supplies tab**: seeds/saplings and protection payments needed for the runs you have left, against your bank, the Farming Guild seed vault and your inventory, with the live OSRS Wiki GE price of each and the cost of what you're short.
- **Checklist overlay**: the current stop, how to get there, and its patches and bank stops in order.
- **Patch highlights**: due patches are outlined in the game world, the next one in bright green. An optional hint arrow points at the next patch.
- **Exact patch tracking**: checking health or planting ticks off the patch you're at, in any order. Opening your bank ticks the next bank stop.
- **Route & patches**: reorder stops and the patches at each stop, edit each stop's travel note, untick patches you don't use (25 in all, including Anglers' Retreat and each Fossil Island patch), and add bank stops before or after any stop's patches.
- **Debug log**: an optional log of everything the tracker sees, for fixing missed ticks.

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
3. Check your route under **Route & patches**. The travel notes assume a maxed house (jewellery box, portal nexus, spirit trees); type your own where yours differ.
4. Do your run. Checking health or planting ticks off that patch, and opening your bank ticks the next bank stop. Press **Finish run** at the end.

## How it tracks things

- **Which patch**: the plugin watches each patch's game state (its varbit) and the map region you're standing in, using the same patch data as RuneLite's built-in Time Tracking plugin. When Farming XP arrives (check-health, planting, harvesting a Hespori) within a tick of exactly one patch changing where you're standing, that patch is ticked. The three Fossil Island patches are told apart the same way.
- **Backup**: if that's ambiguous, a sapling leaving your inventory followed by Farming XP ticks the patch that changed. If it can't place you at all (somewhere it has no data for), it falls back to the next patch of that type in route order and says so. It never ticks a far-away patch when the patch where you're standing is already done.
- **Checking it**: the Run tab shows "Last ticked:" with the patch. You can tick or untick any box by hand, including Hespori and hardwood.
- **Runs**: a run starts at the first tick and includes the Farming XP from the few ticks before it. It's saved to your history, and growth timers start, when you press **Finish run**. If you forget, it finishes itself 12 hours after its last tick (less if you do more runs a day). **New run** saves the current run first.
- **Long-growing patches**: hardwood (85.3 h for ironwood), redwood (106.7 h) and Hespori (32 h) are timed from when you ticked them in a finished run. They show "Ready in…" until they're grown.
- **XP countdown**: it averages your last 5 runs by default (setting: *Average over last N runs*). Before you've finished a run, it uses the plan.
- **Bank stops**: opening your bank during a run ticks the next bank stop, but only when a bank stop is the next thing on the route. You can always tick it by hand.
- **Prices**: the OSRS Wiki's live prices, which RuneLite downloads and refreshes itself. Hespori seeds can't be traded, so they show "–".
- **Saved data**: progress, run history and the bank and seed vault counts are saved per account in your RuneLite profile. Settings and bank stops are saved in your RuneLite settings.

## Debug log

If a patch doesn't tick when it should, turn on **Debug log** at the bottom of the XP tab's settings and do a few runs. It writes Farming XP drops, patch changes, saplings leaving your inventory and every tick decision to `.runelite/tree-running/debug-YYYY-MM-DD.log` (the full path is shown under the setting). Send those files along with a note of which patches it missed.

## Preview

`preview/tree-running-preview.html` is a clickable mock of the panel and overlay. It uses the same crop data and the same planner math, ported to JavaScript.

## For development

- `./gradlew test`: unit tests for the planner (XP table, run projection, supplies and prices, route order with bank stops, stop and patch reordering, run needs with your inventory), patch matching and old-save migration.
- `./gradlew panelSnapshot`: renders the side panel tabs to `build/snapshots/` without starting the client.
- `./gradlew exportPreviewData`: regenerates the data the preview uses.

## Icons

- `src/main/resources/com/treerunning/panel_icon.png`: the 16×16 walking tree in the side bar.
- `icon.png`: the 48×72 Plugin Hub icon.

## Data sources

Levels, XP, payments and growth times come from the OSRS Wiki (Calculator:Farming/Trees and the patch location pages). Item IDs come from RuneLite's own item list. Patch regions and varbits come from RuneLite's Time Tracking plugin (FarmingWorld).
