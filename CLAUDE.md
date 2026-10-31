# Tree Running: RuneLite plugin

A RuneLite plugin for daily OSRS Farming tree runs toward a goal, such as 99 Farming. It's live on the Plugin Hub as "Tree Running", by Welcm2CostoILY. See README.md for what users see.

## Build and run

- Java 11+, Gradle. Built against RuneLite `latest.release`, currently 1.13.4, with Lombok.
- `./gradlew build`: compiles and runs the unit tests in `src/test/java/.../core`, plus `RunStateTest`.
- `./gradlew runClient`: starts RuneLite with the plugin loaded in developer mode. Use this, not the test `main`, from IntelliJ.
- `./gradlew panelSnapshot`: renders the side-panel tabs to `build/snapshots/tab-N.png` with sample data, no client needed. Use it to check UI changes visually.
- `./gradlew exportPreviewData`: regenerates data for `preview/tree-running-preview.html`. That file is a clickable JS mock of the panel.

## Layout

- `data/`: static game data.
  - `CropType`: items, XP, growth and payment.
  - `PatchLocation`: 25 patches, each with a varbit and region IDs copied from RuneLite's FarmingWorld.
  - `RouteStep`: stops and default travel notes.
  - `PatchType`.
- `core/`: pure logic, no RuneLite API, and unit-tested.
  - `RunPlanner`, `XpProjection`, `XpTable`, `SupplyCalculator`.
  - `Route` and `RouteItem`: the checklist with bank stops.
  - `RouteOrder`: the user's stop and patch order.
  - `RunNeeds`: this run's items against the inventory.
  - `ActionMatcher`: XP drop plus one patch varbit change in the player's region ticks that patch.
  - `PatchMatcher`: sapling-removal fallback.
  - `RunRecord`, `BankStop`.
- Top level:
  - `TreeRunningPlugin`: events, run state and settings persistence.
  - `TreeRunningPanel`: Swing UI with XP, Run and Supplies tabs. All settings are edited in the XP tab, and the config items are `hidden = true`.
  - `TreeRunningOverlay`: the checklist overlay.
  - `PatchHighlightOverlay`: patch outlines.
  - `PanelData`: an immutable snapshot that the client thread publishes to Swing and the overlays.
  - `RunState`: per-account state in RS profile config, key `state`, JSON.
  - `DebugLog`: when enabled, logs `[Tree Running]` lines to RuneLite's `client.log`. No file I/O of its own: Plugin Hub flags java.io/java.nio for extra review, and bans process execution such as `LinkBrowser.open`.

## Rules that matter

- A patch is identified by varbit **and** region, because varbits are reused, e.g. 4771.
- Ticking only marks a patch done. Growth timers (`plantedAt`) and run XP are committed in `endRun()` when the user presses Finish run, or by the 12-hour stale auto-finish. Don't set `plantedAt` on tick, or the checkbox can't be unticked.
- `PatchMatcher` must not fall back to route order when the player's region has a patch of that type that's already done.
- Settings saved as JSON strings in config: `bankStops`, `stopOrder`, `patchOrder`, `travelText`.
- Prices come from `itemManager.getItemPriceWithSource(id, true)`, the Wiki live price.
- Owner preference: for UI changes, show a clickable preview (update `preview/`) before changing the plugin.

## Releasing an update

Plugin Hub PR #19586, the first version, is merged. Each update is a **new** PR to runelite/plugin-hub:

1. Push here, then run `git rev-parse HEAD`.
2. In the fork of runelite/plugin-hub, sync with upstream.
3. Edit `plugins/tree-running`:
   - Set `commit=` to the new hash.
   - Set `repository=` too, if this repo's URL changed.
4. Open a PR titled "Update Tree Running".

## Open work

- Fix tracking against the owner's debug logs (`[Tree Running]` lines in `.runelite/logs/client.log`) from real runs.
- Optional: read patch state from RuneLite Time Tracking. Its RS profile config group is `timetracking`, with key `region.varbit` and value `varbitValue:epoch`. It was skipped as fragile.
