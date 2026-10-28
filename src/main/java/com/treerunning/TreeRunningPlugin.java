package com.treerunning;

import com.treerunning.core.BankStop;
import com.treerunning.core.PatchMatcher;
import com.treerunning.core.PlanSettings;
import com.treerunning.core.Route;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunPlanner;
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
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
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
	/** XP gained within this window before the first plant counts toward the run (check-health XP). */
	private static final int RUN_START_LOOKBACK_TICKS = 300;
	/** A sapling leaving the inventory counts as planted if Farming XP arrives within this many ticks. */
	private static final int PLANT_MATCH_TICKS = 2;
	/** A patch's varbit changing within this many ticks of a planting ties the planting to that patch. */
	private static final int PATCH_MATCH_TICKS = 3;

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
	private Gson gson;

	private TreeRunningPanel panel;
	private NavigationButton navButton;

	private RunState state = new RunState();
	/** Bank stops the player added to their route (saved in the plugin settings). */
	private volatile List<BankStop> bankStops = Collections.emptyList();
	/** What the last planting ticked, shown in the Run tab so you can check it. */
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
	private final List<int[]> pendingPlants = new ArrayList<>(); // {tick, itemId}
	/** Varbit ID → patches that use it (the same varbit is reused in different regions). */
	private final Map<Integer, List<PatchLocation>> patchesByVarbit = new HashMap<>();
	/** Patches whose state changed recently while you were standing by them. */
	private final Deque<PatchChange> recentPatchChanges = new ArrayDeque<>();

	private static final class PatchChange
	{
		final int tick;
		final PatchLocation location;

		PatchChange(int tick, PatchLocation location)
		{
			this.tick = tick;
			this.location = location;
		}
	}

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

		loadBankStops();
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
		clientToolbar.removeNavigation(navButton);
		plantedItemToCrop.clear();
		trackedItems.clear();
		pendingPlants.clear();
		recentXpGains.clear();
		patchesByVarbit.clear();
		recentPatchChanges.clear();
		inventory = new HashMap<>();
		data = PanelData.EMPTY;
	}

	PanelData getData()
	{
		return data;
	}

	TreeRunningConfig getConfig()
	{
		return config;
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
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			lastXp = client.getSkillExperience(Skill.FARMING);
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TreeRunningConfig.GROUP.equals(event.getGroup()))
		{
			if ("bankStops".equals(event.getKey()))
			{
				loadBankStops();
			}
			clientThread.invokeLater(this::refresh);
		}
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
			lastXpGainTick = tick;
			recentXpGains.addLast(new int[]{tick, xp - lastXp});
			while (!recentXpGains.isEmpty() && tick - recentXpGains.peekFirst()[0] > RUN_START_LOOKBACK_TICKS)
			{
				recentXpGains.removeFirst();
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
				if (!plantedItemToCrop.containsKey(id))
				{
					continue;
				}
				int before = entry.getValue();
				int after = now.getOrDefault(id, 0);
				for (int i = 0; i < before - after; i++)
				{
					pendingPlants.add(new int[]{tick, id});
				}
			}
			inventory = now;
			refresh();
		}
		else if (containerId == InventoryID.BANK)
		{
			state.bank = countTracked(event.getItemContainer());
			state.bankSeenAt = System.currentTimeMillis();
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
		for (PatchLocation location : patches)
		{
			if (location.isInRegion(region))
			{
				recentPatchChanges.addLast(new PatchChange(tick, location));
			}
		}
		while (!recentPatchChanges.isEmpty() && tick - recentPatchChanges.peekFirst().tick > 2 * PATCH_MATCH_TICKS)
		{
			recentPatchChanges.removeFirst();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (pendingPlants.isEmpty())
		{
			return;
		}

		int tick = client.getTickCount();
		boolean changed = false;
		Iterator<int[]> it = pendingPlants.iterator();
		while (it.hasNext())
		{
			int[] pending = it.next();
			// Wait one tick so the patch's own state change has arrived too.
			if (tick > pending[0] && Math.abs(lastXpGainTick - pending[0]) <= PLANT_MATCH_TICKS)
			{
				onPlanted(plantedItemToCrop.get(pending[1]), pending[0]);
				it.remove();
				changed = true;
			}
			else if (tick - pending[0] > PLANT_MATCH_TICKS)
			{
				// No XP followed, so it was banked, dropped or used elsewhere.
				it.remove();
			}
		}

		if (changed)
		{
			saveState();
			refresh();
		}
	}

	// ------------------------------------------------------------------ run tracking

	private void onPlanted(CropType crop, int plantTick)
	{
		if (crop == null)
		{
			return;
		}

		PlanSettings settings = buildSettings();
		List<RunTask> tasks = currentTasks(settings);

		if (!state.active)
		{
			int lookback = 0;
			for (int[] gain : recentXpGains)
			{
				lookback += gain[1];
			}
			beginRun(client.getSkillExperience(Skill.FARMING) - lookback);
		}

		Set<PatchLocation> changed = new LinkedHashSet<>();
		for (PatchChange change : recentPatchChanges)
		{
			if (Math.abs(change.tick - plantTick) <= PATCH_MATCH_TICKS)
			{
				changed.add(change.location);
			}
		}

		PatchMatcher.Match match = PatchMatcher.match(tasks, state.done, crop.getPatchType(), currentRegion(), changed);
		if (match != null)
		{
			markDone(match.getTask());
			lastMatch = match.getTask().getLabel()
				+ (match.getConfidence() == PatchMatcher.Confidence.ROUTE ? " (guessed from route order)" : "");
			log.debug("Planted {} → {} ({})", crop, match.getTask().getKey(), match.getConfidence());
		}

		finishIfComplete(tasks);
	}

	/** Opening your bank during a run ticks the bank stop, if a bank stop is what's next. */
	private void tickBankStopIfNext()
	{
		if (!state.active)
		{
			return;
		}
		List<RouteItem> items = Route.build(currentTasks(buildSettings()), bankStops);
		RouteItem next = Route.next(items, state.done);
		if (next != null && next.isBank())
		{
			state.done.add(next.getKey());
		}
	}

	private void markDone(RunTask task)
	{
		state.done.add(task.getKey());
		if (task.getCrop().getGrowthMinutes() > 1440.0 / config.runsPerDay())
		{
			state.plantedAt.put(task.getKey(), System.currentTimeMillis());
		}
	}

	private void beginRun(int startXp)
	{
		state.active = true;
		state.startXp = startXp;
		state.startedAt = System.currentTimeMillis();
		state.done.clear();
	}

	private void finishIfComplete(List<RunTask> tasks)
	{
		for (RunTask task : tasks)
		{
			if (task.isDue() && !state.done.contains(task.getKey()))
			{
				return;
			}
		}
		endRun();
	}

	private void endRun()
	{
		if (!state.active)
		{
			return;
		}
		int gained = client.getSkillExperience(Skill.FARMING) - state.startXp;
		if (gained > 0)
		{
			state.runXp.add(gained);
		}
		state.active = false;
	}

	// ------------------------------------------------------------------ panel actions (called from Swing)

	void toggleTask(String key)
	{
		clientThread.invokeLater(() ->
		{
			if (state.done.remove(key))
			{
				saveState();
				refresh();
				return;
			}

			PlanSettings settings = buildSettings();
			List<RunTask> tasks = currentTasks(settings);
			if (!state.active)
			{
				beginRun(client.getSkillExperience(Skill.FARMING));
			}
			if (key.startsWith("bank:"))
			{
				state.done.add(key);
				saveState();
				refresh();
				return;
			}
			for (RunTask task : tasks)
			{
				if (task.getKey().equals(key))
				{
					markDone(task);
					break;
				}
			}
			finishIfComplete(tasks);
			saveState();
			refresh();
		});
	}

	void startNewRun()
	{
		clientThread.invokeLater(() ->
		{
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

	void clearHistory()
	{
		clientThread.invokeLater(() ->
		{
			state.runXp.clear();
			saveState();
			refresh();
		});
	}

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

	// ------------------------------------------------------------------ planning

	private PlanSettings buildSettings()
	{
		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, config.treeCrop().crop());
		crops.put(PatchType.FRUIT_TREE, config.fruitCrop().crop());
		crops.put(PatchType.HARDWOOD, config.hardwoodCrop().crop());
		// These patch types only take one crop; each patch is switched on or off in the Patches section.
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

	private List<RunTask> currentTasks(PlanSettings settings)
	{
		return RunPlanner.buildTasks(settings, client.getRealSkillLevel(Skill.FARMING), state.plantedAt,
			System.currentTimeMillis());
	}

	/** Recomputes everything shown in the panel and overlay. Must run on the client thread. */
	private void refresh()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			publish(PanelData.EMPTY);
			return;
		}

		PlanSettings settings = buildSettings();
		int xp = client.getSkillExperience(Skill.FARMING);
		int level = client.getRealSkillLevel(Skill.FARMING);
		int goalXp = settings.getGoalXp();

		XpProjection planned = XpProjection.toGoal(xp, settings);
		int counted = Math.min(config.averageOver(), state.runXp.size());
		double actual = XpProjection.averageOfRecent(state.runXp, config.averageOver());

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
		d.items = Route.build(tasks, bankStops);
		d.done = new HashSet<>(state.done);
		d.runActive = state.active;
		d.runXpSoFar = state.active ? Math.max(0, xp - state.startXp) : 0;
		d.lastMatch = lastMatch;
		d.needs = RunNeeds.compute(tasks);
		d.supplies = supplies;
		d.bankSeenAt = state.bankSeenAt;
		d.vaultSeenAt = state.vaultSeenAt;
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
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return -1;
		}
		WorldPoint point = client.getTopLevelWorldView().isInstance()
			? WorldPoint.fromLocalInstance(client, player.getLocalLocation())
			: player.getWorldLocation();
		return point == null ? -1 : point.getRegionID();
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
			catch (JsonSyntaxException e)
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
