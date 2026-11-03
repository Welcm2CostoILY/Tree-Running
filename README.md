# Tree Running

A RuneLite plugin for daily tree runs and herb runs toward a Farming goal: a level (99 by default) or any XP total up to 200M.

- **XP tab**: runs, days and a goal date (counting herb runs too, if you set how many a day), XP per run, lifetime and last-run profit, and the tree run history with run times and a delete button per run. The route editor is here too.
- **Trees tab**: the tree run checklist in route order with your bank stops. Each patch shows what it looked like when you last saw it (grown, check health, diseased, dead, stump), and whether you've paid its gardener. A "This run needs" box, an inventory-space warning with where to add a bank stop, and a bank filter.
- **Herbs tab**: every herb patch you've turned on (and its allotments, if you turned those on) with its state and time left, a herb chosen per patch, "This run needs", a checklist that ticks itself (harvest, compost, plant, pay), profit for the run, herb run history, and which herb pays best per patch at live prices.
- **Supplies tab**: seeds/saplings and protection payments needed for the tree runs you have left, against your bank, the Farming Guild seed vault and your inventory, with live OSRS Wiki prices.
- **Bank filter**: shows only what the run needs. Turn it on in the Trees or Herbs tab, or from the right-click menu on the bank's "Show worn items" button.
- **Overlays**: the current stop and its patches, unpaid gardeners, and herbs on the way. Due patches are outlined in game (the next tree patch in bright green), and gardeners you haven't paid are outlined in orange. An optional hint arrow points at the next patch.
- **Herbs on the way** (off by default): grown herb patches at Farming Guild, Catherby and Falador are added to those tree-run stops.
- **Notification** when all your herb patches should be grown.
- **Settings** are in RuneLite's configuration panel (the wrench, then Tree Running): goal, crops, tree patches (26, including an optional crystal tree), herb runs, each herb patch's herb and allotments, overlays and the debug log.

Built and compiled against RuneLite 1.13.4.

## Run it

1. Install **JDK 11 or newer** and **IntelliJ IDEA** (Community is fine).
2. In IntelliJ: **File → Open** and pick this folder. Let it import the Gradle project (it downloads RuneLite automatically).
3. Open `src/test/java/com/treerunning/TreeRunningPluginTest.java` and run its `main` method.
   - Edit the run configuration: **VM options** `-ea`, **Program arguments** `--developer-mode`.
4. RuneLite opens with the plugin loaded. Find **Tree Running** in the plugin list and the tree icon in the side bar.

From a terminal, `./gradlew runClient` (or `gradlew.bat runClient` on Windows) does the same thing.

## First-time setup in game

1. Open RuneLite's settings (the wrench), then **Tree Running**. Pick your goal and crops, untick tree patches you can't use, turn on your herb patches and pick a herb for each (your best seeds in the disease-free ones), and set your compost and diary bonuses. Back in the side panel, add bank stops in the **Route editor** at the bottom of the XP tab.
2. **Open your bank and the Farming Guild seed vault once.** That's how the supply tracker learns what you have. Each updates every time you open it.
3. Check your route under the **Route editor**. The travel notes assume a maxed house (jewellery box, portal nexus, spirit trees); type your own where yours differ.
4. Do your run. Checking health or planting ticks off that patch, and opening your bank ticks the next bank stop. Press **Finish run** at the end.

## How it tracks things

- **Patch states**: every tree, herb and allotment patch's game state (its varbit) is read while you're near it, using RuneLite's own Time Tracking data, so the plugin knows when it's grown, still growing, diseased, dead or a stump. A patch seen growing isn't due until it can have finished.
- **Tree patches**: Farming XP that arrives with a check-health or a planting at the patch you're standing at ticks it, in any order. Raking weeds doesn't. If that's ambiguous, a sapling leaving your inventory with Farming XP ticks it instead.
- **Payments**: when a gardener says "Leave it with me", that patch is marked paid (also the Falador elite diary's free protection). A planted patch that isn't paid is shown in orange and its gardener is outlined.
- **Herb runs**: planting, the last pick and curing or clearing tick themselves from the patch's state. Each pick before that counts from its Farming XP. Compost ticks from the "You treat the herb patch with..." message. Profit is herbs picked at live prices, minus the seeds, compost and allotment payments used.
- **Runs**: a run starts at the first tick and ends when you press **Finish run** (tree runs finish themselves 12 hours after the last tick, herb runs after 2 hours). Its time and profit go to the history.
- **Long-growing patches**: every tree patch is timed from when you ticked it on a finished run, and from what you last saw of it.
- **Prices**: the OSRS Wiki's live prices, which RuneLite downloads itself. Saplings aren't traded, so a planting costs its seed. Hespori seeds and crystal shards can't be traded.
- **Herb estimates**: expected herbs per patch use the OSRS Wiki's formula for your Farming level, compost, magic secateurs, farming cape and diary bonuses, with disease-free patches always surviving.
- **Saved data**: progress, patch states, run history and bank and seed vault counts are saved per account in your RuneLite profile. Settings are saved in your RuneLite settings.

## Debug log

If a patch doesn't tick when it should, turn on **Debug log** at the bottom of the XP tab's settings and do a few runs. It writes Farming XP drops, patch changes, saplings leaving your inventory and every tick decision to RuneLite's own log, `.runelite/logs/client.log`, on lines starting with `[Tree Running]`. Send that file along with a note of which patches it missed.

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
