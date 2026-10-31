package com.treerunning;

import com.treerunning.core.ActionMatcher;
import com.treerunning.core.BankStop;
import com.treerunning.core.PatchMatcher;
import com.treerunning.core.PlanSettings;
import com.treerunning.core.Route;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RouteOrder;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunPlanner;
import com.treerunning.core.RunRecord;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.core.XpProjection;
import com.treerunning.core.XpTable;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Tree Running",
	description = "XP countdown, run checklist with bank stops, and a supply tracker with GE prices for daily tree runs",
	tags = {"farming", "tree", "run", "99", "checklist", "hespori"}
)
public class TreeRunningPlugin extends Plugin
{
	private static final String STATE_KEY = "state";
	/** XP this many ticks before the first patch counts toward the run (a check-health just before planting). */
	private static final int RUN_START_LOOKBACK_TICKS = 20;
	/** A sapling leaving the inventory counts as planted if Farming XP arrives within this many ticks. */
	private static final int PLANT_MATCH_TICKS = 2;
	/** A patch's varbit changing within this many ticks of a planting ties the planting to that patch. */
	private static final int PATCH_MATCH_TICKS = 3;
	/** Farming XP up to this long after the last patch still counts toward the run (planting, paying). */
	private static final long RUN_XP_TAIL_MS = 5 * 60_000L;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private TreeRunningConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TreeRunningOverlay overlay;

	@Inject
	private PatchHighlightOverlay highlightOverlay;

	@Inject
	private Gson gson;

	private TreeRunningPanel panel;
	private NavigationButton navButton;
	private final DebugLog debug = new DebugLog();

	private RunState state = new RunState();
	/** Bank stops the player added to their route (saved in the plugin settings). */
	private volatile List<BankStop> bankStops = Collections.emptyList();
	private volatile RouteOrder routeOrder = RouteOrder.defaults();
	/** Travel notes the player edited, by stop. Stops not in here use the built-in note. */
	private volatile Map<RouteStep, String> travelNotes = Collections.emptyMap();
	/** What the last tick was, shown in the Run tab so you can check it. */
	private String lastMatch;
	private volatile PanelData data = PanelData.EMPTY;

	/** Planted item ID → crop, for every crop. */
	private final Map<Integer, CropType> plantedItemToCrop = new HashMap<>();
	/** Every item ID the supply tracker counts. */
	private final Set<Integer> trackedItems = new HashSet<>();

	private Map<Integer, Integer> inventory = new HashMap<>();
	private int lastXp = -1;
	private int lastXpGainTick = -100;
	private final Deque<int[]> recentXpGains = new ArrayDeque<>(); // {tick, delta}
	/** Farming XP drops still waiting to be matched to a patch change. */
	private final List<Integer> pendingActions = new ArrayList<>();
	private final List<int[]> pendingPlants = new ArrayList<>(); // {tick, itemId}
	/** Varbit ID → patches that use it (the same varbit is reused in different regions). */
	private final Map<Integer, List<PatchLocation>> patchesByVarbit = new HashMap<>();
	/** Patch changes in your region over the last few ticks. */
	private final Deque<ActionMatcher.Change> recentPatchChanges = new ArrayDeque<>();
	private final Map<Integer, Integer> lastVarbitValues = new HashMap<>();
	/** Patch objects in the loaded scene, for highlighting. */
	private final Map<PatchLocation, GameObject> patchObjects = new EnumMap<>(PatchLocation.class);
	/** Where this plugin last put the hint arrow, so it only clears its own. */
	private WorldPoint ourHintArrow;

	@Provides
	TreeRunningConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TreeRunningConfig.class);
	}

	@Override
	protected void startUp()
	{
		for (CropType crop : CropType.values())
		{
			plantedItemToCrop.put(crop.getPlantedItem().getId(), crop);
			for (int id : crop.getStockItemIds())
			{
				if (id >= 0)
				{
					trackedItems.add(id);
				}
			}
			if (crop.getPayment() != null)
			{
				trackedItems.add(crop.getPayment().getId());
			}
		}

		for (PatchLocation location : PatchLocation.values())
		{
			patchesByVarbit.computeIfAbsent(location.getVarbit(), v -> new ArrayList<>()).add(location);
		}

		debug.setEnabled(config.debugLog());
		loadBankStops();
		loadRouteOrder();
		loadTravelNotes();
		panel = new TreeRunningPanel(this);
		SwingUtilities.invokeLater(panel::loadSettings);
		navButton = NavigationButton.builder()
			.tooltip("Tree Running")
			.icon(ImageUtil.loadImageResource(TreeRunningPlugin.class, "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
		overlayManager.add(highlightOverlay);

		clientThread.invokeLater(() ->
		{
			loadState();
			lastXp = client.getGameState() == GameState.LOGGED_IN ? client.getSkillExperience(Skill.FARMING) : -1;
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(highlightOverlay);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(this::clearOurHintArrow);
		plantedItemToCrop.clear();
		trackedItems.clear();
		pendingPlants.clear();
		pendingActions.clear();
		recentXpGains.clear();
		patchesByVarbit.clear();
		recentPatchChanges.clear();
		lastVarbitValues.clear();
		patchObjects.clear();
		inventory = new HashMap<>();
		data = PanelData.EMPTY;
		debug.shutDown();
	}

	PanelData getData()
	{
		return data;
	}

	TreeRunningConfig getConfig()
	{
		return config;
	}

	/** Patch objects in the scene; read by the highlight overlay on the client thread. */
	Map<PatchLocation, GameObject> getPatchObjects()
	{
		return patchObjects;
	}

	// ------------------------------------------------------------------ events

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadState();
		refresh();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			patchObjects.clear();
		}
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			lastXp = client.getSkillExperience(Skill.FARMING);
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!TreeRunningConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		switch (event.getKey())
		{
			case "bankStops":
				loadBankStops();
				break;
			case "stopOrder":
			case "patchOrder":
				loadRouteOrder();
				break;
			case "travelText":
				loadTravelNotes();
				break;
			case "debugLog":
				debug.setEnabled(config.debugLog());
				break;
			case "hintArrow":
				if (!config.hintArrow())
				{
					clientThread.invokeLater(this::clearOurHintArrow);
				}
				break;
			default:
				break;
		}
		clientThread.invokeLater(this::refresh);
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.FARMING)
		{
			return;
		}

		int xp = event.getXp();
		if (lastXp >= 0 && xp > lastXp)
		{
			int tick = client.getTickCount();
			int delta = xp - lastXp;
			lastXpGainTick = tick;
			recentXpGains.addLast(new int[]{tick, delta});
			while (!recentXpGains.isEmpty() && tick - recentXpGains.peekFirst()[0] > RUN_START_LOOKBACK_TICKS)
			{
				recentXpGains.removeFirst();
			}
			pendingActions.add(tick);
			debug.log(tick, "XP +" + delta + " (total " + xp + ") at " + where());

			if (state.active && System.currentTimeMillis() - state.lastTickAt < RUN_XP_TAIL_MS)
			{
				state.runLastXp = xp;
				saveState();
			}
		}
		lastXp = xp;
		refresh();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV)
		{
			Map<Integer, Integer> now = countTracked(event.getItemContainer());
			int tick = client.getTickCount();
			for (Map.Entry<Integer, Integer> entry : inventory.entrySet())
			{
				int id = entry.getKey();
				CropType crop = plantedItemToCrop.get(id);
				if (crop == null)
				{
					continue;
				}
				int gone = entry.getValue() - now.getOrDefault(id, 0);
				for (int i = 0; i < gone; i++)
				{
					pendingPlants.add(new int[]{tick, id});
				}
				if (gone > 0)
				{
					debug.log(tick, gone + "x " + crop.getPlantedItem().getName() + " left the inventory at " + where());
				}
			}
			inventory = now;
			refresh();
		}
		else if (containerId == InventoryID.BANK)
		{
			state.bank = countTracked(event.getItemContainer());
			state.bankSeenAt = System.currentTimeMillis();
			debug.log(client.getTickCount(), "Bank opened at " + where());
			tickBankStopIfNext();
			saveState();
			refresh();
		}
		else if (containerId == InventoryID.SEED_VAULT)
		{
			state.vault = countTracked(event.getItemContainer());
			state.vaultSeenAt = System.currentTimeMillis();
			saveState();
			refresh();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		List<PatchLocation> patches = patchesByVarbit.get(event.getVarbitId());
		if (patches == null)
		{
			return;
		}

		int region = currentRegion();
		int tick = client.getTickCount();
		Integer old = lastVarbitValues.put(event.getVarbitId(), event.getValue());
		List<String> here = new ArrayList<>();
		for (PatchLocation location : patches)
		{
			if (location.isInRegion(region))
			{
				recentPatchChanges.addLast(new ActionMatcher.Change(tick, location));
				here.add(location.name());
			}
		}
		while (!recentPatchChanges.isEmpty() && tick - recentPatchChanges.peekFirst().tick() > 2 * PATCH_MATCH_TICKS)
		{
			recentPatchChanges.removeFirst();
		}
		debug.log(tick, "Varbit " + event.getVarbitId() + ": " + old + " -> " + event.getValue()
			+ " in region " + region + (here.isEmpty() ? " (not a tracked patch here)" : " = " + here));
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		ObjectComposition def = client.getObjectDefinition(object.getId());
		if (def == null)
		{
			return;
		}
		List<PatchLocation> patches = patchesByVarbit.get(def.getVarbitId());
		if (patches == null || object.getWorldLocation() == null)
		{
			return;
		}
		int region = object.getWorldLocation().getRegionID();
		for (PatchLocation location : patches)
		{
			if (location.isInRegion(region))
			{
				patchObjects.put(location, object);
			}
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		patchObjects.values().removeIf(o -> o == event.getGameObject());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();
		boolean changed = false;

		// Farming XP that arrived with a patch change where you're standing: check-health,
		// planting, harvesting a Hespori. Wait until the patch change has had time to arrive.
		Iterator<Integer> actions = pendingActions.iterator();
		while (actions.hasNext())
		{
			int xpTick = actions.next();
			if (tick <= xpTick + ActionMatcher.WINDOW_TICKS)
			{
				continue;
			}
			actions.remove();
			changed |= matchAction(xpTick);
		}

		// Backup: a sapling left the inventory and Farming XP followed.
		Iterator<int[]> it = pendingPlants.iterator();
		while (it.hasNext())
		{
			int[] pending = it.next();
			if (tick > pending[0] && Math.abs(lastXpGainTick - pending[0]) <= PLANT_MATCH_TICKS)
			{
				changed |= onPlanted(plantedItemToCrop.get(pending[1]), pending[0]);
				it.remove();
			}
			else if (tick - pending[0] > PLANT_MATCH_TICKS)
			{
				// No XP followed, so it was banked, dropped or used elsewhere.
				it.remove();
			}
		}

		updateHintArrow();

		if (changed)
		{
			saveState();
			refresh();
		}
	}

	// ------------------------------------------------------------------ run tracking

	private boolean matchAction(int xpTick)
	{
		Set<PatchLocation> skip = EnumSet.noneOf(PatchLocation.class);
		for (PatchLocation location : PatchLocation.values())
		{
			if (state.done.contains(RunTask.keyFor(location, 0)) || !isPatchEnabled(location))
			{
				skip.add(location);
			}
		}
		PatchLocation location = ActionMatcher.find(xpTick, recentPatchChanges, skip);
		if (location == null)
		{
			debug.log(xpTick, "  no single patch matched this XP");
			return false;
		}

		List<RunTask> tasks = currentTasks(buildSettings());
		for (RunTask task : tasks)
		{
			if (task.getLocation() == location)
			{
				startIfNeeded(xpTick);
				markDone(task);
				lastMatch = task.getLabel();
				debug.log(xpTick, "  ticked " + task.getKey() + " (XP + patch change)");
				return true;
			}
		}
		debug.log(xpTick, "  matched " + location + " but it isn't on the run (check its crop and level)");
		return false;
	}

	private boolean onPlanted(CropType crop, int plantTick)
	{
		if (crop == null)
		{
			return false;
		}

		List<RunTask> tasks = currentTasks(buildSettings());
		Set<PatchLocation> changed = new LinkedHashSet<>();
		for (ActionMatcher.Change change : recentPatchChanges)
		{
			if (Math.abs(change.tick() - plantTick) <= PATCH_MATCH_TICKS)
			{
				changed.add(change.location());
			}
		}

		PatchMatcher.Match match = PatchMatcher.match(tasks, state.done, crop.getPatchType(), currentRegion(), changed);
		if (match == null)
		{
			debug.log(plantTick, "  planted " + crop + ": already ticked or nothing to match");
			return false;
		}
		startIfNeeded(plantTick);
		markDone(match.getTask());
		lastMatch = match.getTask().getLabel()
			+ (match.getConfidence() == PatchMatcher.Confidence.ROUTE ? " (guessed from route order)" : "");
		debug.log(plantTick, "  planted " + crop + " -> ticked " + match.getTask().getKey() + " (" + match.getConfidence() + ")");
		return true;
	}

	/** Opening your bank during a run ticks the bank stop, if a bank stop is what's next. */
	private void tickBankStopIfNext()
	{
		if (!state.active)
		{
			return;
		}
		List<RouteItem> items = Route.build(currentTasks(buildSettings()), bankStops, routeOrder);
		RouteItem next = Route.next(items, state.done);
		if (next != null && next.isBank())
		{
			state.done.add(next.getKey());
			debug.log(client.getTickCount(), "  ticked " + next.getKey());
		}
	}

	/** Ticking only marks the patch; timers and run XP are saved on Finish run. */
	private void markDone(RunTask task)
	{
		long now = System.currentTimeMillis();
		state.done.add(task.getKey());
		state.tickedAt.put(task.getKey(), now);
		state.lastTickAt = now;
		state.runLastXp = client.getSkillExperience(Skill.FARMING);
	}

	/** Starts a run if none is going, after finishing one that was left open too long. */
	private void startIfNeeded(int triggerTick)
	{
		finishIfStale();
		if (state.active)
		{
			return;
		}
		int lookback = 0;
		for (int[] gain : recentXpGains)
		{
			if (gain[0] >= triggerTick - RUN_START_LOOKBACK_TICKS)
			{
				lookback += gain[1];
			}
		}
		beginRun(client.getSkillExperience(Skill.FARMING) - lookback);
	}

	private void beginRun(int startXp)
	{
		state.active = true;
		state.startXp = startXp;
		state.startedAt = System.currentTimeMillis();
		state.lastTickAt = state.startedAt;
		state.runLastXp = 0;
		state.done.clear();
		state.tickedAt.clear();
		debug.log(client.getTickCount(), "Run started at " + startXp + " XP");
	}

	/** If you forget to press Finish run, it finishes 12 hours after its last patch (less with more runs a day). */
	private void finishIfStale()
	{
		long staleMs = (long) (Math.max(2, 12.0 / Math.max(1, config.runsPerDay())) * 3_600_000L);
		if (state.active && state.lastTickAt > 0 && System.currentTimeMillis() - state.lastTickAt >= staleMs)
		{
			debug.log(client.getTickCount(), "Run left open; finishing it");
			endRun();
		}
	}

	/** Saves the run to history and starts the growth timers of what was ticked. */
	private void endRun()
	{
		if (!state.active)
		{
			return;
		}
		List<RunTask> tasks = currentTasks(buildSettings());
		double minutesBetweenRuns = 1440.0 / Math.max(1, config.runsPerDay());
		int patches = 0;
		for (String key : state.done)
		{
			if (key.startsWith("bank:"))
			{
				continue;
			}
			patches++;
			for (RunTask task : tasks)
			{
				if (task.getKey().equals(key) && task.getCrop().getGrowthMinutes() > minutesBetweenRuns)
				{
					state.plantedAt.put(key, state.tickedAt.getOrDefault(key, System.currentTimeMillis()));
				}
			}
		}

		int endXp = state.runLastXp > 0 ? state.runLastXp : client.getSkillExperience(Skill.FARMING);
		int gained = endXp - state.startXp;
		if (gained > 0 && patches > 0)
		{
			state.runs.add(new RunRecord(System.currentTimeMillis(), gained, patches));
		}
		debug.log(client.getTickCount(), "Run finished: " + gained + " XP, " + patches + " patches");
		state.active = false;
		state.done.clear();
		state.tickedAt.clear();
		lastMatch = null;
	}

	// ------------------------------------------------------------------ hint arrow

	private void updateHintArrow()
	{
		if (!config.hintArrow())
		{
			return;
		}
		WorldPoint target = null;
		RouteItem next = data.nextItem();
		if (next != null && !next.isBank())
		{
			GameObject object = patchObjects.get(next.getTask().getLocation());
			if (object != null)
			{
				target = object.getWorldLocation();
			}
		}
		if (target == null)
		{
			clearOurHintArrow();
		}
		else if (!target.equals(ourHintArrow))
		{
			client.setHintArrow(target);
			ourHintArrow = target;
		}
	}

	private void clearOurHintArrow()
	{
		if (ourHintArrow != null && client.hasHintArrow() && ourHintArrow.equals(client.getHintArrowPoint()))
		{
			client.clearHintArrow();
		}
		ourHintArrow = null;
	}

	// ------------------------------------------------------------------ panel actions (called from Swing)

	void toggleTask(String key)
	{
		clientThread.invokeLater(() ->
		{
			if (state.done.remove(key))
			{
				state.tickedAt.remove(key);
				debug.log(client.getTickCount(), "Unticked " + key + " by hand");
			}
			else
			{
				startIfNeeded(client.getTickCount());
				if (key.startsWith("bank:"))
				{
					state.done.add(key);
				}
				else
				{
					for (RunTask task : currentTasks(buildSettings()))
					{
						if (task.getKey().equals(key))
						{
							markDone(task);
							break;
						}
					}
				}
				debug.log(client.getTickCount(), "Ticked " + key + " by hand");
			}
			saveState();
			refresh();
		});
	}

	void startNewRun()
	{
		clientThread.invokeLater(() ->
		{
			endRun();
			beginRun(client.getSkillExperience(Skill.FARMING));
			saveState();
			refresh();
		});
	}

	void finishRun()
	{
		clientThread.invokeLater(() ->
		{
			endRun();
			saveState();
			refresh();
		});
	}

	void deleteRun(int index)
	{
		clientThread.invokeLater(() ->
		{
			if (index >= 0 && index < state.runs.size())
			{
				state.runs.remove(index);
				saveState();
				refresh();
			}
		});
	}

	void clearHistory()
	{
		clientThread.invokeLater(() ->
		{
			state.runs.clear();
			saveState();
			refresh();
		});
	}

	// ------------------------------------------------------------------ settings (called from Swing)

	/** Saves one setting from the XP tab. ConfigManager is thread-safe. */
	void setSetting(String key, Object value)
	{
		configManager.setConfiguration(TreeRunningConfig.GROUP, key, value);
	}

	boolean isPatchEnabled(PatchLocation location)
	{
		String key = TreeRunningConfig.PATCH_KEYS.get(location);
		String value = key == null ? null : configManager.getConfiguration(TreeRunningConfig.GROUP, key);
		return value == null || Boolean.parseBoolean(value);
	}

	void setPatchEnabled(PatchLocation location, boolean enabled)
	{
		String key = TreeRunningConfig.PATCH_KEYS.get(location);
		if (key != null)
		{
			setSetting(key, enabled);
		}
	}

	/** A copy of the bank stops, for the route editor. */
	List<BankStop> getBankStops()
	{
		List<BankStop> copy = new ArrayList<>();
		for (BankStop b : bankStops)
		{
			copy.add(new BankStop(b.id, b.getRouteStep(), b.before, b.name));
		}
		return copy;
	}

	void saveBankStops(List<BankStop> stops)
	{
		bankStops = Collections.unmodifiableList(new ArrayList<>(stops));
		setSetting("bankStops", gson.toJson(stops));
	}

	static BankStop newBankStop(RouteStep step)
	{
		return new BankStop(UUID.randomUUID().toString().substring(0, 8), step, true, "");
	}

	RouteOrder getRouteOrder()
	{
		return routeOrder;
	}

	void saveRouteOrder(RouteOrder order)
	{
		routeOrder = order;
		setSetting("stopOrder", gson.toJson(order.stopNames()));
		setSetting("patchOrder", gson.toJson(order.patchNames()));
	}

	/** The travel note for a stop: the player's own, or the built-in one. */
	String travelNote(RouteStep step)
	{
		String own = travelNotes.get(step);
		return own != null ? own : step.getTravel();
	}

	Map<RouteStep, String> getTravelNotes()
	{
		return new EnumMap<>(travelNotes.isEmpty() ? new EnumMap<>(RouteStep.class) : travelNotes);
	}

	void saveTravelNotes(Map<RouteStep, String> notes)
	{
		travelNotes = Collections.unmodifiableMap(new EnumMap<>(notes.isEmpty() ? new EnumMap<>(RouteStep.class) : notes));
		Map<String, String> byName = new HashMap<>();
		for (Map.Entry<RouteStep, String> e : notes.entrySet())
		{
			byName.put(e.getKey().name(), e.getValue());
		}
		setSetting("travelText", gson.toJson(byName));
	}

	private void loadBankStops()
	{
		List<BankStop> loaded = new ArrayList<>();
		try
		{
			List<BankStop> parsed = gson.fromJson(config.bankStops(), new TypeToken<List<BankStop>>()
			{
			}.getType());
			if (parsed != null)
			{
				for (BankStop b : parsed)
				{
					if (b != null && b.id != null && b.getRouteStep() != null)
					{
						loaded.add(b);
					}
				}
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Couldn't read saved bank stops", e);
		}
		bankStops = Collections.unmodifiableList(loaded);
	}

	private void loadRouteOrder()
	{
		try
		{
			List<String> stops = gson.fromJson(config.stopOrder(), new TypeToken<List<String>>()
			{
			}.getType());
			Map<String, List<String>> patches = gson.fromJson(config.patchOrder(), new TypeToken<Map<String, List<String>>>()
			{
			}.getType());
			routeOrder = RouteOrder.fromNames(stops, patches);
		}
		catch (JsonParseException e)
		{
			log.warn("Couldn't read saved route order", e);
			routeOrder = RouteOrder.defaults();
		}
	}

	private void loadTravelNotes()
	{
		Map<RouteStep, String> notes = new EnumMap<>(RouteStep.class);
		try
		{
			Map<String, String> byName = gson.fromJson(config.travelText(), new TypeToken<Map<String, String>>()
			{
			}.getType());
			if (byName != null)
			{
				for (RouteStep step : RouteStep.values())
				{
					String note = byName.get(step.name());
					if (note != null)
					{
						notes.put(step, note);
					}
				}
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Couldn't read saved travel notes", e);
		}
		travelNotes = Collections.unmodifiableMap(notes);
	}

	// ------------------------------------------------------------------ planning

	private PlanSettings buildSettings()
	{
		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, config.treeCrop().crop());
		crops.put(PatchType.FRUIT_TREE, config.fruitCrop().crop());
		crops.put(PatchType.HARDWOOD, config.hardwoodCrop().crop());
		// These patch types only take one crop; each patch is switched on or off in Route & patches.
		crops.put(PatchType.CALQUAT, CropType.CALQUAT);
		crops.put(PatchType.CELASTRUS, CropType.CELASTRUS);
		crops.put(PatchType.REDWOOD, CropType.REDWOOD);
		crops.put(PatchType.HESPORI, CropType.HESPORI);

		Set<PatchLocation> enabled = EnumSet.noneOf(PatchLocation.class);
		for (PatchLocation location : PatchLocation.values())
		{
			if (isPatchEnabled(location))
			{
				enabled.add(location);
			}
		}

		return new PlanSettings(crops, enabled, goalXp(), config.runsPerDay());
	}

	/** The goal as an XP total, whichever way it was set. */
	private int goalXp()
	{
		return config.goalType() == TreeRunningConfig.GoalType.XP
			? Math.max(1, Math.min(200_000_000, config.goalXp()))
			: XpTable.xpForLevel(Math.max(2, Math.min(99, config.goalLevel())));
	}

	/** The run's patches in the player's route order. */
	private List<RunTask> currentTasks(PlanSettings settings)
	{
		return routeOrder.sort(RunPlanner.buildTasks(settings, client.getRealSkillLevel(Skill.FARMING), state.plantedAt,
			System.currentTimeMillis()));
	}

	/** Recomputes everything shown in the panel and overlays. Must run on the client thread. */
	private void refresh()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			publish(PanelData.EMPTY);
			return;
		}

		finishIfStale();

		PlanSettings settings = buildSettings();
		int xp = client.getSkillExperience(Skill.FARMING);
		int level = client.getRealSkillLevel(Skill.FARMING);
		int goalXp = settings.getGoalXp();

		List<Integer> runXp = new ArrayList<>();
		for (RunRecord r : state.runs)
		{
			runXp.add(r.xp);
		}
		XpProjection planned = XpProjection.toGoal(xp, settings);
		int counted = Math.min(config.averageOver(), runXp.size());
		double actual = XpProjection.averageOfRecent(runXp, config.averageOver());

		boolean fromActual = actual > 0;
		int runsLeft = fromActual ? XpProjection.runsAtRate(xp, goalXp, actual) : planned.getRuns();
		if (runsLeft < 0 || (!fromActual && !planned.reachesGoal()))
		{
			runsLeft = -1;
		}

		Map<CropType, Integer> plantings = runsLeft > 0
			? XpProjection.forRuns(xp, runsLeft, settings).getPlantings()
			: Collections.emptyMap();
		Map<Integer, Integer> bank = state.bank;
		Map<Integer, Integer> vault = state.vault;
		Map<Integer, Integer> inv = inventory;
		List<SupplyCalculator.SupplyLine> supplies = SupplyCalculator.compute(plantings,
			id -> bank.getOrDefault(id, 0) + vault.getOrDefault(id, 0) + inv.getOrDefault(id, 0),
			// The OSRS Wiki's live price, which RuneLite downloads and keeps up to date.
			id -> itemManager.getItemPriceWithSource(id, true));

		List<RunTask> tasks = currentTasks(settings);

		PanelData d = new PanelData();
		d.loggedIn = true;
		d.currentXp = xp;
		d.level = level;
		d.goalType = config.goalType();
		d.goalLevel = config.goalLevel();
		d.goalXp = goalXp;
		d.plannedXpPerRun = planned.getAverageXpPerRun();
		d.plannedRuns = planned.reachesGoal() ? planned.getRuns() : -1;
		d.actualXpPerRun = actual;
		d.actualRunsCounted = counted;
		d.runsLeft = runsLeft;
		d.runsLeftFromActual = fromActual;
		d.runsPerDay = settings.getRunsPerDay();
		d.tasks = tasks;
		d.items = Route.build(tasks, bankStops, routeOrder);
		d.done = new HashSet<>(state.done);
		d.runActive = state.active;
		d.runXpSoFar = state.active ? Math.max(0, xp - state.startXp) : 0;
		d.lastMatch = lastMatch;
		d.needs = RunNeeds.compute(tasks, d.done, id -> inv.getOrDefault(id, 0));
		d.supplies = supplies;
		d.bankSeenAt = state.bankSeenAt;
		d.vaultSeenAt = state.vaultSeenAt;
		d.runs = new ArrayList<>(state.runs);
		Map<RouteStep, String> travel = new EnumMap<>(RouteStep.class);
		for (RouteStep step : RouteStep.values())
		{
			travel.put(step, travelNote(step));
		}
		d.travel = travel;
		publish(d);
	}

	private void publish(PanelData snapshot)
	{
		data = snapshot;
		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.update(snapshot));
		}
	}

	// ------------------------------------------------------------------ helpers

	/** The map region you're standing in, translated out of instances. -1 if unknown. */
	private int currentRegion()
	{
		WorldPoint point = playerLocation();
		return point == null ? -1 : point.getRegionID();
	}

	private WorldPoint playerLocation()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		return client.getTopLevelWorldView().isInstance()
			? WorldPoint.fromLocalInstance(client, player.getLocalLocation())
			: player.getWorldLocation();
	}

	/** For the debug log: region and tile. */
	private String where()
	{
		WorldPoint p = playerLocation();
		return p == null ? "unknown" : "region " + p.getRegionID() + " (" + p.getX() + "," + p.getY() + "," + p.getPlane() + ")";
	}

	/** Counts tracked items, folding noted copies into their unnoted ID. */
	private Map<Integer, Integer> countTracked(ItemContainer container)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		if (container == null)
		{
			return counts;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			int id = itemManager.canonicalize(item.getId());
			if (trackedItems.contains(id))
			{
				counts.merge(id, item.getQuantity(), Integer::sum);
			}
		}
		return counts;
	}

	private void loadState()
	{
		String json = configManager.getRSProfileConfiguration(TreeRunningConfig.GROUP, STATE_KEY);
		RunState loaded = null;
		if (json != null && !json.isEmpty())
		{
			try
			{
				loaded = gson.fromJson(json, RunState.class);
			}
			catch (JsonParseException e)
			{
				log.warn("Couldn't read saved farming run state, starting fresh", e);
			}
		}
		state = loaded != null ? loaded : new RunState();
		state.fillDefaults();
	}

	private void saveState()
	{
		configManager.setRSProfileConfiguration(TreeRunningConfig.GROUP, STATE_KEY, gson.toJson(state));
	}
}
