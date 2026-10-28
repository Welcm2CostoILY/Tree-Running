# Tree Running

A RuneLite plugin for daily tree runs toward a Farming goal (99 by default).

- **XP countdown**: runs, days and a goal date. It starts from your chosen crops and switches to the average of your real runs once you finish one.
- **Checklist overlay**: the next stop, how to get there, its patches, and where to bank first.
- **Supply tracker**: seeds/saplings and protection payments needed for the runs you have left, against what's in your bank and inventory.
- **Inventory plan**: what to carry for the whole run, what to withdraw for each leg, and where the bank stops go. It allows for the tool leprechaun only noting produce (not banking it).
- **Exact patch tracking**: planting ticks off the patch you actually planted, in any order.
- **Crop choices**: tree, fruit tree and hardwood dropdowns, and a checkbox for every patch (25 in all, including Anglers' Retreat and each Fossil Island patch).

Built and compiled against RuneLite 1.13.4.

## Run it

1. Install **JDK 11 or newer** and **IntelliJ IDEA** (Community is fine).
2. In IntelliJ: **File → Open** and pick this folder. Let it import the Gradle project (it downloads RuneLite automatically).
3. Open `src/test/java/com/treerunning/TreeRunningPluginTest.java` and run its `main` method.
   - Edit the run configuration: **VM options** `-ea`, **Program arguments** `--developer-mode`.
4. RuneLite opens with the plugin loaded. Find **Tree Running** in the plugin list and the tree icon in the side bar.

From a terminal, `./gradlew runClient` (or `gradlew.bat runClient` on Windows) does the same thing.

## First-time setup in game

1. Open the plugin settings and pick your crops. Untick any patches you can't use.
2. **Open your bank once.** That's how the supply tracker learns what you have. It updates every time you open the bank.
3. Do your run. Planting a sapling ticks off the next patch of that type automatically.

## How it tracks things

- **Planting**: when a sapling (or Hespori seed) leaves your inventory and Farming XP arrives within two game ticks, it counts as planted.
- **Which patch**: the plugin watches each patch's game state (its varbit) and the map region you're standing in, using the same patch data as RuneLite's built-in Time Tracking plugin. The patch whose state changed as you planted gets ticked, so you can do the stops in any order. The three Fossil Island patches are told apart the same way. If it can't place you (somewhere it has no data for), it falls back to the next patch of that type in route order and says so in the Run tab.
- **Checking it**: the Run tab shows "Last planted:" with the patch it ticked. You can still tick or untick any box by hand.
- **Runs**: a run starts at your first plant and includes the check-health XP from the few minutes before it. It ends when every due patch is ticked, or when you press **Finish run**. Run XP is all Farming XP gained during the run.
- **Long-growing patches**: hardwood (85.3 h for ironwood), redwood (106.7 h) and Hespori (32 h) are timed from when you planted them. They show "Ready in…" until they're grown.
- **XP countdown**: it averages your last 5 runs by default (setting: *Average over last N runs*). Before you've finished a run, it uses the plan.
- **Saved data**: progress, run history and the bank count are saved per account in your RuneLite profile.

## Inventory plan

- Payments are planned **noted** and carried for the whole run. Gardeners accept noted payments.
- Teleport items the route needs (Pendant of Ates, teleport crystal, digsite pendant) are added automatically. The *Always-carried slots* setting covers your own items: spade, coins, house teleport, and so on.
- With **Pick fruit** on, 6 slots stay free to pick a tree, plus one slot per kind of noted produce. The leprechaun notes produce but doesn't bank it, so the noted stacks stay in your inventory.
- Saplings take one slot each. When the next stop won't fit, the plan adds a bank stop. It uses the nearest bank to that stop, or Ring of dueling → Castle Wars from your house.

## Preview

`preview/tree-running-preview.html` is a clickable mock of the panel and overlay. It uses the same crop data and the same planner math, ported to JavaScript.

## For development

- `./gradlew test`: unit tests for the planner (XP table, run projection, supplies, inventory plan) and patch matching.
- `./gradlew panelSnapshot`: renders the side panel tabs to `build/snapshots/` without starting the client.
- `./gradlew exportPreviewData`: regenerates the data the preview uses.

## Icons

- `src/main/resources/com/treerunning/panel_icon.png`: the 16×16 walking tree in the side bar.
- `icon.png`: the 48×72 Plugin Hub icon.

## Data sources

Levels, XP, payments and growth times come from the OSRS Wiki (Calculator:Farming/Trees and the patch location pages). Item IDs come from RuneLite's own item list. Patch regions and varbits come from RuneLite's Time Tracking plugin (FarmingWorld).
