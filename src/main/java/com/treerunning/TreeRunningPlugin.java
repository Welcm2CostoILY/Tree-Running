package com.treerunning;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import com.treerunning.core.ActionMatcher;
import com.treerunning.core.BankStop;
import com.treerunning.core.HerbPlanner;
import com.treerunning.core.HerbSettings;
import com.treerunning.core.HerbTask;
import com.treerunning.core.InventoryFit;
import com.treerunning.core.PatchMatcher;
import com.treerunning.core.PlanSettings;
import com.treerunning.core.Route;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RouteOrder;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunPlanner;
import com.treerunning.core.RunRecord;
import com.treerunning.core.RunTask;
import com.treerunning.core.SeenPatch;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.core.XpProjection;
import com.treerunning.core.XpTable;
import com.treerunning.data.AllotmentType;
import com.treerunning.data.Compost;
import com.treerunning.data.CropType;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.HerbType;
import com.treerunning.data.PatchCondition;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchReading;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModelType;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Tree Running",
	description = "XP countdown, tree and herb run checklists with bank stops, profit, and a supply tracker with GE prices",
	tags = {"farming", "tree", "herb", "run", "99", "checklist", "hespori", "allotment", "profit"}
)
@PluginDependency(BankTagsPlugin.class)
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
	/** A herb run with nothing done for this long is finished for you. */
	private static final long HERB_RUN_STALE_MS = 2 * 3_600_000L;
	/** A compost message ties to the patch you used the compost on this many ticks before. */
	private static final int COMPOST_MATCH_TICKS = 10;

	static final String TREE_TAG = "tree running: tree run";
	static final String HERB_TAG = "tree running: herb run";

	/** What a gardener says when you pay them (RuneLite's Time Tracking, PaymentTracker). */
	private static final Set<String> PAYMENT_TEXT = new HashSet<>(java.util.Arrays.asList(
		"That'll do nicely, sir. Leave it with me - I'll make sure<br>that patch grows for you.",
		"That'll do nicely, madam. Leave it with me - I'll make<br>sure that patch grows for you.",
		"That'll do nicely. Leave it with me - I'll make sure that<br>patch grows for you.",
		"That'll do nicely, iknami. Leave it with me - I'll make<br>sure that patch grows for you.",
		"Alright, leave it with me. I'll look after that nursery for<br>you."));
	private static final String FALADOR_DIARY_TEXT = "The gardener protects your tree for you, free of charge, as a token "
		+ "of gratitude for completing the Falador elite diary.";
	/** "You treat the herb patch with ultracompost." (RuneLite's Time Tracking, CompostTracker). */
	private static final Pattern COMPOST_USED = Pattern.compile("You treat the (.+) with (ultra|super|)compost\\.");

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
	private Notifier notifier;

	@Inject
	private BankTagsService bankTagsService;

	@Inject
	private TagManager tagManager;

	@Inject
	private Gson gson;

	private TreeRunningPanel panel;
	private NavigationButton navButton;
	private final DebugLog debug = new DebugLog();

	private RunState state = new RunState();
	private volatile List<BankStop> bankStops = Collections.emptyList();
	private volatile RouteOrder routeOrder = RouteOrder.defaults();
	private volatile Map<RouteStep, String> travelNotes = Collections.emptyMap();
	private String lastMatch;
	private String herbLastMatch;
	private volatile PanelData data = PanelData.EMPTY;

	private final Map<Integer, CropType> plantedItemToCrop = new HashMap<>();
	private final Set<Integer> trackedItems = new HashSet<>();
	private final Set<Integer> produceItems = new HashSet<>();

	private Map<Integer, Integer> inventory = new HashMap<>();
	private Map<Integer, Integer> equipment = new HashMap<>();
	private int lastXp = -1;
	private int lastXpGainTick = -100;
	private final Deque<int[]> recentXpGains = new ArrayDeque<>(); // {tick, delta}
	/** Farming XP drops still waiting to be matched: {tick, delta}. */
	private final List<int[]> pendingActions = new ArrayList<>();
	private final List<int[]> pendingPlants = new ArrayList<>(); // {tick, itemId}
	private final Deque<ActionMatcher.Change> recentPatchChanges = new ArrayDeque<>();
	/** The reading before and after each patch's latest change, for telling a plant from a rake. */
	private final Map<String, PatchReading[]> lastTransition = new HashMap<>();
	private final Map<String, PatchReading> currentReading = new HashMap<>();
	private final Map<String, Integer> lastValue = new HashMap<>();
	private int lastCompostTick = -100;
	private String compostTarget;
	private int compostTargetTick = -100;
	/** Which patch was picked in the gardener's dialogue (RuneLite's PaymentTracker), or -1. */
	private int lastSelectedOption = -1;

	/** Patch objects and gardeners in the loaded scene, for highlighting. */
	private final Map<String, GameObject> patchObjects = new HashMap<>();
	private final Map<Integer, NPC> gardeners = new HashMap<>();
	private Set<Integer> gardenerIds = Collections.emptySet();
	private WorldPoint ourHintArrow;

	private volatile Set<Integer> treeTagItems = Collections.emptySet();
	private volatile Set<Integer> herbTagItems = Collections.emptySet();

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
			if (crop.getProduce() != null)
			{
				produceItems.add(crop.getProduce().getId());
				trackedItems.add(crop.getProduce().getId());
			}
		}
		for (HerbType herb : HerbType.values())
		{
			trackedItems.add(herb.getSeedId());
		}
		for (AllotmentType crop : AllotmentType.values())
		{
			trackedItems.add(crop.getSeedId());
			trackedItems.add(crop.getPaymentId());
		}
		trackedItems.add(ItemID.BUCKET_ULTRACOMPOST);
		trackedItems.add(ItemID.BUCKET_SUPERCOMPOST);
		trackedItems.add(ItemID.BUCKET_COMPOST);
		trackedItems.add(ItemID.FAIRY_ENCHANTED_SECATEURS);

		Set<Integer> ids = new HashSet<>();
		for (TrackedPatch p : TrackedPatch.ALL)
		{
			if (p.gardener >= 0)
			{
				ids.add(p.gardener);
			}
		}
		gardenerIds = ids;

		debug.setEnabled(config.debugLog());
		loadBankStops();
		loadRouteOrder();
		loadTravelNotes();
		tagManager.registerTag(TREE_TAG, id -> treeTagItems.contains(id));
		tagManager.registerTag(HERB_TAG, id -> herbTagItems.contains(id));

		panel = new TreeRunningPanel(this);
		SwingUtilities.invokeLater(panel::loadRouteEditor);
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
		clientThread.invokeLater(() ->
		{
			clearOurHintArrow();
			String active = bankTagsService.getActiveTag();
			if (TREE_TAG.equals(active) || HERB_TAG.equals(active))
			{
				bankTagsService.closeBankTag();
			}
		});
		tagManager.unregisterTag(TREE_TAG);
		tagManager.unregisterTag(HERB_TAG);
		plantedItemToCrop.clear();
		trackedItems.clear();
		produceItems.clear();
		pendingPlants.clear();
		pendingActions.clear();
		recentXpGains.clear();
		recentPatchChanges.clear();
		lastTransition.clear();
		currentReading.clear();
		lastValue.clear();
		patchObjects.clear();
		gardeners.clear();
		inventory = new HashMap<>();
		equipment = new HashMap<>();
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

	/** Patch objects in the scene by patch key; read by the highlight overlay on the client thread. */
	Map<String, GameObject> getPatchObjects()
	{
		return patchObjects;
	}

	/** Gardener NPCs in the scene by NPC ID; read by the highlight overlay on the client thread. */
	Map<Integer, NPC> getGardeners()
	{
		return gardeners;
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
			gardeners.clear();
			currentReading.clear();
			lastValue.clear();
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
			pendingActions.add(new int[]{tick, delta});
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
				CropType crop = plantedItemToCrop.get(entry.getKey());
				if (crop == null)
				{
					continue;
				}
				int gone = entry.getValue() - now.getOrDefault(entry.getKey(), 0);
				for (int i = 0; i < gone; i++)
				{
					pendingPlants.add(new int[]{tick, entry.getKey()});
				}
				if (gone > 0)
				{
					debug.log(tick, gone + "x " + crop.getPlantedItem().getName() + " left the inventory at " + where());
				}
			}
			// Fruit and bark picked on a tree run count toward its profit.
			if (state.active && !isBankOpen())
			{
				boolean changed = false;
				for (int id : produceItems)
				{
					int gained = now.getOrDefault(id, 0) - inventory.getOrDefault(id, 0);
					if (gained > 0)
					{
						state.treeProduce.merge(id, gained, Integer::sum);
						changed = true;
					}
				}
				if (changed)
				{
					saveState();
				}
			}
			inventory = now;
			refresh();
		}
		else if (containerId == InventoryID.WORN)
		{
			equipment = countAll(event.getItemContainer());
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
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String message = event.getMessage();
		if (FALADOR_DIARY_TEXT.equals(message))
		{
			markPaid(RunTask.keyFor(PatchLocation.TREE_FALADOR, 0));
			return;
		}
		Matcher m = COMPOST_USED.matcher(message);
		if (m.find())
		{
			lastCompostTick = client.getTickCount();
			onComposted(m.group(1));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();
		// Compost used on a patch: remember which patch, for the "You treat..." message.
		if (action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
		{
			ObjectComposition def = client.getObjectDefinition(event.getId());
			WorldPoint p = playerLocation();
			if (def != null && p != null)
			{
				for (TrackedPatch patch : TrackedPatch.BY_VARBIT.getOrDefault(def.getVarbitId(), Collections.emptyList()))
				{
					if (patch.type != TrackedPatch.Type.TREE && patch.isAt(p.getX(), p.getY(), p.getPlane()))
					{
						compostTarget = patch.key;
						compostTargetTick = client.getTickCount();
					}
				}
			}
		}
		// Which patch was chosen when paying a gardener with more than one (RuneLite's PaymentTracker).
		if (action == MenuAction.WIDGET_CONTINUE)
		{
			Widget w = event.getWidget();
			if (w != null && w.getId() == InterfaceID.Chatmenu.OPTIONS && w.getIndex() > -1 && isPatchOption(w.getText()))
			{
				lastSelectedOption = w.getIndex() - 1;
			}
		}
		else if ((action == MenuAction.NPC_THIRD_OPTION || action == MenuAction.NPC_FOURTH_OPTION)
			&& event.getMenuOption().startsWith("Pay"))
		{
			lastSelectedOption = action == MenuAction.NPC_THIRD_OPTION ? 0 : 1;
		}
	}

	@Subscribe(priority = -1) // after the Bank Tags plugin
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_INIT && config.bankFilter() != TreeRunningConfig.BankFilter.OFF)
		{
			openBankFilter();
		}
		else if (event.getScriptId() == ScriptID.CHATBOX_KEYINPUT_MATCHED)
		{
			int[] stack = client.getIntStack();
			Widget parent = client.getWidget(stack[0]);
			Widget w = parent == null ? null : parent.getChild(stack[1]);
			if (stack[0] == InterfaceID.Chatmenu.OPTIONS && stack[1] > -1 && w != null && isPatchOption(w.getText()))
			{
				lastSelectedOption = stack[1] - 1;
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!"Show worn items".equals(event.getOption()) || !isBankOpen())
		{
			return;
		}
		TreeRunningConfig.BankFilter mode = config.bankFilter();
		client.getMenu().createMenuEntry(-1)
			.setOption(mode == TreeRunningConfig.BankFilter.HERB ? "Herb run filter off" : "Herb run filter")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> setBankFilter(mode == TreeRunningConfig.BankFilter.HERB ? TreeRunningConfig.BankFilter.OFF : TreeRunningConfig.BankFilter.HERB));
		client.getMenu().createMenuEntry(-1)
			.setOption(mode == TreeRunningConfig.BankFilter.TREE ? "Tree run filter off" : "Tree run filter")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> setBankFilter(mode == TreeRunningConfig.BankFilter.TREE ? TreeRunningConfig.BankFilter.OFF : TreeRunningConfig.BankFilter.TREE));
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		ObjectComposition def = client.getObjectDefinition(object.getId());
		WorldPoint at = object.getWorldLocation();
		if (def == null || at == null)
		{
			return;
		}
		for (TrackedPatch patch : TrackedPatch.BY_VARBIT.getOrDefault(def.getVarbitId(), Collections.emptyList()))
		{
			if (patch.isAt(at.getX(), at.getY(), at.getPlane()))
			{
				patchObjects.put(patch.key, object);
			}
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		patchObjects.values().removeIf(o -> o == event.getGameObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (gardenerIds.contains(event.getNpc().getId()))
		{
			gardeners.put(event.getNpc().getId(), event.getNpc());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		gardeners.values().removeIf(n -> n == event.getNpc());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();
		boolean changed = readPatchesHere();

		Iterator<int[]> actions = pendingActions.iterator();
		while (actions.hasNext())
		{
			int[] action = actions.next();
			if (tick <= action[0] + ActionMatcher.WINDOW_TICKS)
			{
				continue;
			}
			actions.remove();
			changed |= matchAction(action[0], action[1]);
		}

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
				it.remove();
			}
		}

		changed |= checkPayment();
		updateHintArrow();

		if (changed)
		{
			saveState();
			refresh();
		}
	}

	// ------------------------------------------------------------------ reading patches

	/**
	 * Reads every tracked patch whose varbit is sent where you're standing, once a tick, after
	 * the game has moved you (so a teleport's new varbits aren't read against the old spot).
	 * The first reading after arriving is just remembered; later differences are changes.
	 */
	private boolean readPatchesHere()
	{
		WorldPoint p = playerLocation();
		if (p == null)
		{
			return false;
		}
		int tick = client.getTickCount();
		boolean changed = false;
		Set<String> here = new HashSet<>();
		for (TrackedPatch patch : TrackedPatch.ALL)
		{
			if (!patch.isAt(p.getX(), p.getY(), p.getPlane()))
			{
				continue;
			}
			here.add(patch.key);
			int value = client.getVarbitValue(patch.varbit);
			Integer old = lastValue.put(patch.key, value);
			PatchReading reading = patch.kind.read(value);
			PatchReading before = currentReading.put(patch.key, reading);
			if (old == null)
			{
				changed |= remember(patch, reading);
				continue;
			}
			if (old != value)
			{
				lastTransition.put(patch.key, new PatchReading[]{before, reading});
				recentPatchChanges.addLast(new ActionMatcher.Change(tick, patch.key));
				changed |= remember(patch, reading);
				debug.log(tick, "Varbit " + patch.varbit + " " + old + " -> " + value + " at " + patch.name() + ": "
					+ (before == null ? "?" : describe(before)) + " -> " + describe(reading));
			}
		}
		// Leaving an area forgets its readings, so coming back starts fresh.
		lastValue.keySet().retainAll(here);
		currentReading.keySet().retainAll(here);
		while (!recentPatchChanges.isEmpty() && tick - recentPatchChanges.peekFirst().tick() > 2 * PATCH_MATCH_TICKS)
		{
			recentPatchChanges.removeFirst();
		}
		return changed;
	}

	/** Saves what a patch looks like now. */
	private boolean remember(TrackedPatch patch, PatchReading reading)
	{
		if (reading.getCondition() == PatchCondition.UNKNOWN)
		{
			return false;
		}
		SeenPatch old = state.seen.get(patch.key);
		long now = System.currentTimeMillis();
		state.seen.put(patch.key, new SeenPatch(reading.getCondition(), reading.getStage(), reading.getStages(),
			reading.getMinutesPerStage(), reading.getCrop(), now));
		return old == null || old.condition != reading.getCondition() || old.stage != reading.getStage();
	}

	private static String describe(PatchReading r)
	{
		return r.getCrop() + " " + r.getCondition().getLabel()
			+ (r.getCondition() == PatchCondition.GROWING ? " " + (r.getStage() + 1) + "/" + r.getStages() : "");
	}

	// ------------------------------------------------------------------ matching XP to patches

	private boolean matchAction(int xpTick, int delta)
	{
		Set<String> skip = new HashSet<>();
		for (PatchLocation location : PatchLocation.values())
		{
			if (state.done.contains(RunTask.keyFor(location, 0)) || !isPatchEnabled(location))
			{
				skip.add(RunTask.keyFor(location, 0));
			}
		}
		String key = ActionMatcher.find(xpTick, recentPatchChanges, skip);
		if (key == null)
		{
			return matchHerbPick(xpTick, delta);
		}
		TrackedPatch patch = TrackedPatch.BY_KEY.get(key);
		PatchReading[] t = lastTransition.getOrDefault(key, new PatchReading[]{null, PatchReading.UNKNOWN});
		PatchReading before = t[0];
		PatchReading after = t[1];
		if (patch.type == TrackedPatch.Type.TREE)
		{
			return matchTree(patch, before, after, xpTick);
		}
		return matchHerb(patch, before, after, xpTick, delta);
	}

	private boolean matchTree(TrackedPatch patch, PatchReading before, PatchReading after, int xpTick)
	{
		// Raking weeds also gives XP and changes the patch; that isn't the patch being done.
		if (after.isWeeds() || after.getCondition() == PatchCondition.EMPTY)
		{
			debug.log(xpTick, "  " + patch.name() + " was raked or cleared, not ticked");
			return false;
		}
		boolean checked = before != null && before.getCondition() == PatchCondition.CHECK_HEALTH
			&& after.getCondition() != PatchCondition.CHECK_HEALTH;
		boolean planted = after.getCondition() == PatchCondition.GROWING && after.getStage() == 0;
		if (!checked && !planted && patch.tree != PatchLocation.HESPORI_FARMING_GUILD && before != null)
		{
			debug.log(xpTick, "  " + patch.name() + " changed (" + describe(before) + " -> " + describe(after) + ") but isn't a check or plant");
			return false;
		}
		for (RunTask task : currentTasks(buildSettings()))
		{
			if (task.getKey().equals(patch.key))
			{
				startIfNeeded(xpTick);
				markDone(task);
				lastMatch = task.getLabel() + (checked ? " (checked)" : planted ? " (planted)" : "");
				debug.log(xpTick, "  ticked " + task.getKey() + (checked ? " (check-health)" : " (planted)"));
				return true;
			}
		}
		return false;
	}

	private boolean matchHerb(TrackedPatch patch, PatchReading before, PatchReading after, int xpTick, int delta)
	{
		String key = patch.key;
		PatchCondition was = before == null ? PatchCondition.UNKNOWN : before.getCondition();
		if (after.getCondition() == PatchCondition.GROWING && after.getStage() == 0 && !after.isWeeds())
		{
			startHerbRunIfNeeded();
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.PLANT));
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.HARVEST));
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.CLEAR));
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.CURE));
			state.herbCost += plantCost(patch, after.getCrop());
			state.herbXp += delta;
			state.herbLastTickAt = System.currentTimeMillis();
			herbLastMatch = "Planted " + after.getCrop() + " at " + patch.name();
			debug.log(xpTick, "  " + herbLastMatch);
			return true;
		}
		if (was == PatchCondition.GROWN && (after.isWeeds() || after.getCondition() == PatchCondition.EMPTY))
		{
			// The last pick empties the patch.
			startHerbRunIfNeeded();
			countPicks(patch, before.getCrop(), delta);
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.HARVEST));
			herbLastMatch = "Harvested " + patch.name();
			return true;
		}
		if (was == PatchCondition.DISEASED && after.getCondition() == PatchCondition.GROWING)
		{
			startHerbRunIfNeeded();
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.CURE));
			state.herbXp += delta;
			herbLastMatch = "Cured " + patch.name();
			return true;
		}
		if (was == PatchCondition.DEAD)
		{
			startHerbRunIfNeeded();
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.CLEAR));
			state.herbXp += delta;
			herbLastMatch = "Cleared " + patch.name();
			return true;
		}
		debug.log(xpTick, "  " + patch.name() + " changed with XP but isn't a plant or harvest");
		return false;
	}

	/** A herb or allotment pick: XP with no patch change, while standing at a grown patch. */
	private boolean matchHerbPick(int xpTick, int delta)
	{
		WorldPoint p = playerLocation();
		if (p == null)
		{
			return false;
		}
		// Compost gives XP too; its message tells it apart.
		if (Math.abs(lastCompostTick - xpTick) <= 2)
		{
			return false;
		}
		for (TrackedPatch patch : TrackedPatch.ALL)
		{
			if (patch.type == TrackedPatch.Type.TREE || !patch.isAt(p.getX(), p.getY(), p.getPlane()))
			{
				continue;
			}
			PatchReading r = currentReading.get(patch.key);
			if (r == null || r.getCondition() != PatchCondition.GROWN)
			{
				continue;
			}
			double each = harvestXp(patch, r.getCrop());
			if (each <= 0)
			{
				continue;
			}
			double n = delta / each;
			if (Math.abs(n - Math.round(n)) < 0.15 && Math.round(n) >= 1)
			{
				startHerbRunIfNeeded();
				countPicks(patch, r.getCrop(), delta);
				return true;
			}
		}
		debug.log(xpTick, "  no single patch matched this XP");
		return false;
	}

	private void countPicks(TrackedPatch patch, String crop, int delta)
	{
		double each = harvestXp(patch, crop);
		int n = each > 0 ? (int) Math.max(1, Math.round(delta / each)) : 1;
		int itemId = produceId(patch, crop);
		state.herbPicked += n;
		state.herbValue += (long) n * (itemId > 0 ? price(itemId) : 0);
		state.herbXp += delta;
		state.herbLastTickAt = System.currentTimeMillis();
		herbLastMatch = "Picked " + n + " " + crop + " at " + patch.name();
	}

	private static double harvestXp(TrackedPatch patch, String crop)
	{
		if (patch.type == TrackedPatch.Type.HERB)
		{
			HerbType h = HerbType.byCropName(crop);
			return h == null ? 0 : h.getHarvestXp();
		}
		AllotmentType a = AllotmentType.byCropName(crop);
		return a == null ? 0 : a.getHarvestXp();
	}

	private static int produceId(TrackedPatch patch, String crop)
	{
		if (patch.type == TrackedPatch.Type.HERB)
		{
			HerbType h = HerbType.byCropName(crop);
			return h == null ? -1 : h.getHerbId();
		}
		AllotmentType a = AllotmentType.byCropName(crop);
		return a == null ? -1 : a.getProduceId();
	}

	private long plantCost(TrackedPatch patch, String crop)
	{
		if (patch.type == TrackedPatch.Type.HERB)
		{
			HerbType h = HerbType.byCropName(crop);
			return h == null ? 0 : price(h.getSeedId());
		}
		AllotmentType a = AllotmentType.byCropName(crop);
		return a == null ? 0 : (long) AllotmentType.SEEDS_PER_PATCH * price(a.getSeedId());
	}

	/** "You treat the herb patch with ultracompost." */
	private void onComposted(String what)
	{
		int tick = client.getTickCount();
		WorldPoint p = playerLocation();
		String key = null;
		if (compostTarget != null && tick - compostTargetTick <= COMPOST_MATCH_TICKS)
		{
			key = compostTarget;
		}
		else if (p != null && what.contains("herb"))
		{
			for (TrackedPatch patch : TrackedPatch.ALL)
			{
				if (patch.type == TrackedPatch.Type.HERB && patch.isAt(p.getX(), p.getY(), p.getPlane()))
				{
					key = patch.key;
				}
			}
		}
		compostTarget = null;
		if (key == null)
		{
			return;
		}
		startHerbRunIfNeeded();
		state.herbDone.add(HerbTask.stepKey(key, HerbTask.COMPOST));
		Compost compost = config.compost();
		if (compost.getItemId() > 0)
		{
			state.herbCost += Math.round(price(compost.getItemId()) * compost.getBucketsPerUse());
		}
		state.herbLastTickAt = System.currentTimeMillis();
		herbLastMatch = "Composted " + TrackedPatch.BY_KEY.get(key).name();
		debug.log(tick, "  " + herbLastMatch);
		saveState();
		refresh();
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
			TrackedPatch patch = TrackedPatch.BY_KEY.get(change.patch());
			if (patch != null && patch.tree != null && Math.abs(change.tick() - plantTick) <= PATCH_MATCH_TICKS)
			{
				changed.add(patch.tree);
			}
		}
		PatchMatcher.Match match = PatchMatcher.match(tasks, state.done, crop.getPatchType(), currentRegion(), changed);
		if (match == null)
		{
			return false;
		}
		startIfNeeded(plantTick);
		markDone(match.getTask());
		lastMatch = match.getTask().getLabel()
			+ (match.getConfidence() == PatchMatcher.Confidence.ROUTE ? " (guessed from route order)" : "");
		debug.log(plantTick, "  planted " + crop + " -> ticked " + match.getTask().getKey() + " (" + match.getConfidence() + ")");
		return true;
	}

	// ------------------------------------------------------------------ gardener payments

	/** Watches the dialogue for a gardener accepting payment (as RuneLite's Time Tracking does). */
	private boolean checkPayment()
	{
		Widget text = client.getWidget(InterfaceID.ChatLeft.TEXT);
		if (text == null || !PAYMENT_TEXT.contains(text.getText()))
		{
			return false;
		}
		Widget head = client.getWidget(InterfaceID.ChatLeft.HEAD);
		if (head == null || head.getModelType() != WidgetModelType.NPC_CHATHEAD)
		{
			return false;
		}
		int npcId = head.getModelId();
		TrackedPatch match = null;
		for (TrackedPatch patch : TrackedPatch.ALL)
		{
			if (patch.gardener == npcId && (patch.gardenerOption < 0 || patch.gardenerOption == lastSelectedOption))
			{
				match = patch;
				break;
			}
		}
		return match != null && markPaid(match.key);
	}

	private boolean markPaid(String key)
	{
		if (!state.paid.add(key))
		{
			return false;
		}
		TrackedPatch patch = TrackedPatch.BY_KEY.get(key);
		if (patch != null && patch.type == TrackedPatch.Type.ALLOTMENT)
		{
			state.herbDone.add(HerbTask.stepKey(key, HerbTask.PAY));
			state.herbCost += allotmentPaymentCost();
		}
		debug.log(client.getTickCount(), "Paid the gardener for " + (patch != null ? patch.name() : key));
		saveState();
		refresh();
		return true;
	}

	private long allotmentPaymentCost()
	{
		AllotmentType crop = config.allotmentCrop();
		return (long) crop.getPaymentQuantity() * price(crop.getPaymentId());
	}

	private static boolean isPatchOption(String name)
	{
		return name != null && (name.contains("Patch") || name.contains("patch") || name.contains("allotment"));
	}

	// ------------------------------------------------------------------ tree run state

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

	private void markDone(RunTask task)
	{
		long now = System.currentTimeMillis();
		state.done.add(task.getKey());
		state.tickedAt.put(task.getKey(), now);
		state.lastTickAt = now;
		state.runLastXp = client.getSkillExperience(Skill.FARMING);
	}

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
		state.treeProduce.clear();
		state.paid.removeIf(k -> TrackedPatch.BY_KEY.containsKey(k) && TrackedPatch.BY_KEY.get(k).type == TrackedPatch.Type.TREE);
		debug.log(client.getTickCount(), "Run started at " + startXp + " XP");
	}

	private void finishIfStale()
	{
		long staleMs = (long) (Math.max(2, 12.0 / Math.max(1, config.runsPerDay())) * 3_600_000L);
		if (state.active && state.lastTickAt > 0 && System.currentTimeMillis() - state.lastTickAt >= staleMs)
		{
			endRun();
		}
		if (state.herbActive && state.herbLastTickAt > 0 && System.currentTimeMillis() - state.herbLastTickAt >= HERB_RUN_STALE_MS)
		{
			endHerbRun();
		}
	}

	/** Saves the run to history, with its time and profit, and starts the growth timers of what was ticked. */
	private void endRun()
	{
		if (!state.active)
		{
			return;
		}
		List<RunTask> tasks = currentTasks(buildSettings());
		int patches = 0;
		long cost = 0;
		for (String key : state.done)
		{
			if (key.startsWith("bank:"))
			{
				continue;
			}
			patches++;
			for (RunTask task : tasks)
			{
				if (task.getKey().equals(key))
				{
					state.plantedAt.put(key, state.tickedAt.getOrDefault(key, System.currentTimeMillis()));
					CropType crop = task.getCrop();
					// Saplings aren't traded, so a planting costs its seed.
					cost += price(crop.getSeed().getId());
					if (crop.getPayment() != null && state.paid.contains(key))
					{
						cost += (long) crop.getPaymentQuantity() * price(crop.getPayment().getId());
					}
				}
			}
		}
		long value = 0;
		for (Map.Entry<Integer, Integer> e : state.treeProduce.entrySet())
		{
			value += (long) e.getValue() * price(e.getKey());
		}

		int endXp = state.runLastXp > 0 ? state.runLastXp : client.getSkillExperience(Skill.FARMING);
		int gained = endXp - state.startXp;
		if (gained > 0 && patches > 0)
		{
			RunRecord r = new RunRecord(state.lastTickAt > 0 ? state.lastTickAt : System.currentTimeMillis(), gained, patches);
			r.startedAt = state.startedAt;
			r.cost = cost;
			r.value = value;
			r.profitTracked = true;
			state.runs.add(r);
		}
		debug.log(client.getTickCount(), "Run finished: " + gained + " XP, " + patches + " patches");
		state.active = false;
		state.done.clear();
		state.tickedAt.clear();
		state.treeProduce.clear();
		lastMatch = null;
	}

	// ------------------------------------------------------------------ herb run state

	private void startHerbRunIfNeeded()
	{
		finishIfStale();
		if (state.herbActive)
		{
			return;
		}
		beginHerbRun();
	}

	private void beginHerbRun()
	{
		state.herbActive = true;
		state.herbStartedAt = System.currentTimeMillis();
		state.herbLastTickAt = state.herbStartedAt;
		state.herbStartXp = client.getSkillExperience(Skill.FARMING);
		state.herbDone.clear();
		state.herbPicked = 0;
		state.herbValue = 0;
		state.herbCost = 0;
		state.herbXp = 0;
		state.paid.removeIf(k -> TrackedPatch.BY_KEY.containsKey(k) && TrackedPatch.BY_KEY.get(k).type == TrackedPatch.Type.ALLOTMENT);
	}

	private void endHerbRun()
	{
		if (!state.herbActive)
		{
			return;
		}
		int patches = 0;
		for (String step : state.herbDone)
		{
			if (step.endsWith("|" + HerbTask.PLANT))
			{
				patches++;
			}
		}
		if (patches > 0 || state.herbPicked > 0)
		{
			RunRecord r = new RunRecord(state.herbLastTickAt > 0 ? state.herbLastTickAt : System.currentTimeMillis(), state.herbXp, patches);
			r.startedAt = state.herbStartedAt;
			r.value = state.herbValue;
			r.cost = state.herbCost;
			r.picked = state.herbPicked;
			r.profitTracked = true;
			state.herbRuns.add(r);
		}
		state.herbActive = false;
		state.herbDone.clear();
		herbLastMatch = null;
	}

	// ------------------------------------------------------------------ bank filter

	/** Turns the bank filter on for one run, or off. Called from the panel or the bank's menu. */
	void setBankFilter(TreeRunningConfig.BankFilter mode)
	{
		setSetting("bankFilter", mode);
		clientThread.invokeLater(() ->
		{
			if (mode == TreeRunningConfig.BankFilter.OFF)
			{
				String active = bankTagsService.getActiveTag();
				if (TREE_TAG.equals(active) || HERB_TAG.equals(active))
				{
					bankTagsService.closeBankTag();
				}
			}
			else if (isBankOpen())
			{
				openBankFilter(mode);
			}
		});
	}

	private void openBankFilter()
	{
		openBankFilter(config.bankFilter());
	}

	private void openBankFilter(TreeRunningConfig.BankFilter mode)
	{
		if (mode == TreeRunningConfig.BankFilter.OFF)
		{
			return;
		}
		bankTagsService.openBankTag(mode == TreeRunningConfig.BankFilter.TREE ? TREE_TAG : HERB_TAG,
			BankTagsService.OPTION_HIDE_TAG_NAME | BankTagsService.OPTION_NO_LAYOUT);
	}

	private boolean isBankOpen()
	{
		Widget w = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		return w != null && !w.isHidden();
	}

	/** Item IDs (with their charged variants) for a stop's teleport item. */
	private static Collection<Integer> carryItemIds(String name)
	{
		List<Integer> ids = new ArrayList<>();
		if (name == null)
		{
			return ids;
		}
		switch (name)
		{
			case "Teleport crystal":
				ids.add(ItemID.MOURNING_TELEPORT_CRYSTAL_1);
				ids.add(ItemID.MOURNING_TELEPORT_CRYSTAL_2);
				ids.add(ItemID.MOURNING_TELEPORT_CRYSTAL_3);
				ids.add(ItemID.MOURNING_TELEPORT_CRYSTAL_4);
				ids.add(ItemID.MOURNING_TELEPORT_CRYSTAL_5);
				ids.add(ItemID.PRIF_TELEPORT_CRYSTAL);
				break;
			case "Digsite pendant":
				ids.add(ItemID.NECKLACE_OF_DIGSITE_1);
				ids.add(ItemID.NECKLACE_OF_DIGSITE_2);
				ids.add(ItemID.NECKLACE_OF_DIGSITE_3);
				ids.add(ItemID.NECKLACE_OF_DIGSITE_4);
				ids.add(ItemID.NECKLACE_OF_DIGSITE_5);
				break;
			case "Pendant of Ates":
				ids.add(ItemID.PENDANT_OF_ATES);
				break;
			default:
				break;
		}
		return ids;
	}

	private void updateTagItems(List<RouteItem> items, Set<String> done, List<HerbTask> herbTasks, Set<String> herbDone)
	{
		Set<Integer> tree = new HashSet<>();
		tree.add(ItemID.SPADE);
		tree.add(ItemID.COINS);
		for (RouteItem item : items)
		{
			if (item.isBank() || !item.isDue() || done.contains(item.getKey()))
			{
				continue;
			}
			CropType crop = item.getTask().getCrop();
			tree.add(crop.getPlantedItem().getId());
			if (crop.getPayment() != null)
			{
				tree.add(crop.getPayment().getId());
			}
			tree.addAll(carryItemIds(item.getStep().getCarryItem()));
		}

		Set<Integer> herb = new HashSet<>();
		herb.add(ItemID.SPADE);
		herb.add(ItemID.DIBBER);
		herb.add(ItemID.RAKE);
		herb.add(ItemID.FAIRY_ENCHANTED_SECATEURS);
		Compost compost = config.compost();
		if (compost.isBottomless())
		{
			herb.add(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED);
			herb.add(ItemID.BOTTOMLESS_COMPOST_BUCKET);
		}
		else if (compost.getItemId() > 0)
		{
			herb.add(compost.getItemId());
		}
		for (HerbTask task : herbTasks)
		{
			if (!task.isDue() || herbDone.contains(task.stepKey(HerbTask.PLANT)))
			{
				continue;
			}
			if (task.getHerb() != null)
			{
				herb.add(task.getHerb().getSeedId());
			}
			else
			{
				herb.add(task.getAllotmentCrop().getSeedId());
				if (config.payForAllotments())
				{
					herb.add(task.getAllotmentCrop().getPaymentId());
				}
			}
		}
		// Seeds and teleports have charged or noted variants; show every form.
		treeTagItems = withVariations(tree);
		herbTagItems = withVariations(herb);
	}

	private static Set<Integer> withVariations(Set<Integer> ids)
	{
		Set<Integer> all = new HashSet<>(ids);
		for (int id : ids)
		{
			all.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(id)));
		}
		return Collections.unmodifiableSet(all);
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
			GameObject object = patchObjects.get(next.getTask().getKey());
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

	/** Ticks or unticks one herb-run step by hand. */
	void toggleHerbStep(String stepKey)
	{
		clientThread.invokeLater(() ->
		{
			if (!state.herbDone.remove(stepKey))
			{
				startHerbRunIfNeeded();
				state.herbDone.add(stepKey);
				state.herbLastTickAt = System.currentTimeMillis();
			}
			saveState();
			refresh();
		});
	}

	void startNewHerbRun()
	{
		clientThread.invokeLater(() ->
		{
			endHerbRun();
			beginHerbRun();
			saveState();
			refresh();
		});
	}

	void finishHerbRun()
	{
		clientThread.invokeLater(() ->
		{
			endHerbRun();
			saveState();
			refresh();
		});
	}

	void deleteHerbRun(int index)
	{
		clientThread.invokeLater(() ->
		{
			if (index >= 0 && index < state.herbRuns.size())
			{
				state.herbRuns.remove(index);
				saveState();
				refresh();
			}
		});
	}

	void clearHerbHistory()
	{
		clientThread.invokeLater(() ->
		{
			state.herbRuns.clear();
			saveState();
			refresh();
		});
	}

	// ------------------------------------------------------------------ settings

	void setSetting(String key, Object value)
	{
		configManager.setConfiguration(TreeRunningConfig.GROUP, key, value);
	}

	boolean isPatchEnabled(PatchLocation location)
	{
		String key = TreeRunningConfig.PATCH_KEYS.get(location);
		String value = key == null ? null : configManager.getConfiguration(TreeRunningConfig.GROUP, key);
		return value == null ? !location.isOffByDefault() : Boolean.parseBoolean(value);
	}

	private boolean herbFlag(Map<HerbLocation, String> keys, HerbLocation location, boolean fallback)
	{
		String value = configManager.getConfiguration(TreeRunningConfig.GROUP, keys.get(location));
		return value == null ? fallback : Boolean.parseBoolean(value);
	}

	private HerbType herbChoice(HerbLocation location)
	{
		String value = configManager.getConfiguration(TreeRunningConfig.GROUP, TreeRunningConfig.HERB_CHOICE_KEYS.get(location));
		if (value != null)
		{
			try
			{
				return HerbType.valueOf(value);
			}
			catch (IllegalArgumentException e)
			{
				// fall through to the default
			}
		}
		return HerbType.RANARR;
	}

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

	String travelNote(RouteStep step)
	{
		String own = travelNotes.get(step);
		return own != null ? own : step.getTravel();
	}

	Map<RouteStep, String> getTravelNotes()
	{
		Map<RouteStep, String> copy = new EnumMap<>(RouteStep.class);
		copy.putAll(travelNotes);
		return copy;
	}

	void saveTravelNotes(Map<RouteStep, String> notes)
	{
		Map<RouteStep, String> copy = new EnumMap<>(RouteStep.class);
		copy.putAll(notes);
		travelNotes = Collections.unmodifiableMap(copy);
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
		crops.put(PatchType.CALQUAT, CropType.CALQUAT);
		crops.put(PatchType.CELASTRUS, CropType.CELASTRUS);
		crops.put(PatchType.REDWOOD, CropType.REDWOOD);
		crops.put(PatchType.HESPORI, CropType.HESPORI);
		crops.put(PatchType.CRYSTAL, CropType.CRYSTAL);

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

	private HerbSettings buildHerbSettings(int level)
	{
		Map<HerbLocation, HerbType> herbs = new EnumMap<>(HerbLocation.class);
		Set<HerbLocation> enabled = EnumSet.noneOf(HerbLocation.class);
		Set<HerbLocation> allotments = EnumSet.noneOf(HerbLocation.class);
		for (HerbLocation location : HerbLocation.values())
		{
			herbs.put(location, herbChoice(location));
			if (herbFlag(TreeRunningConfig.HERB_ENABLED_KEYS, location, location != HerbLocation.HARMONY_ISLAND))
			{
				enabled.add(location);
			}
			if (location.getAllotmentCount() > 0 && herbFlag(TreeRunningConfig.ALLOTMENT_KEYS, location, false))
			{
				allotments.add(location);
			}
		}
		double items = (config.magicSecateurs() ? 0.10 : 0) + (config.farmingCape() ? 0.05 : 0);
		return new HerbSettings(herbs, enabled, allotments, config.allotmentCrop(), config.payForAllotments(), config.compost(),
			level, items, config.kandarinDiary().bonus(), config.kourendHard());
	}

	private int goalXp()
	{
		return config.goalType() == TreeRunningConfig.GoalType.XP
			? Math.max(1, Math.min(200_000_000, config.goalXp()))
			: XpTable.xpForLevel(Math.max(2, Math.min(99, config.goalLevel())));
	}

	/** The run's patches in route order, with what you last saw of each. */
	private List<RunTask> currentTasks(PlanSettings settings)
	{
		long now = System.currentTimeMillis();
		List<RunTask> tasks = RunPlanner.buildTasks(settings, client.getRealSkillLevel(Skill.FARMING), state.plantedAt, now);
		return routeOrder.sort(RunPlanner.applySeen(tasks, state.seen, state.plantedAt, now));
	}

	private long price(int itemId)
	{
		return itemManager.getItemPriceWithSource(itemId, true);
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
		long now = System.currentTimeMillis();

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
			id -> itemManager.getItemPriceWithSource(id, true));

		List<RunTask> tasks = currentTasks(settings);
		HerbSettings herbSettings = buildHerbSettings(level);
		List<HerbTask> herbTasks = HerbPlanner.buildTasks(herbSettings, state.seen, now);

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
		d.runStartedAt = state.startedAt;
		d.runXpSoFar = state.active ? Math.max(0, xp - state.startXp) : 0;
		d.lastMatch = lastMatch;
		d.needs = RunNeeds.compute(tasks, d.done, id -> inv.getOrDefault(id, 0));
		d.slots = InventoryFit.slotsFor(d.items, d.done);
		d.overflow = InventoryFit.check(d.items, d.done);
		d.supplies = supplies;
		d.bankSeenAt = state.bankSeenAt;
		d.vaultSeenAt = state.vaultSeenAt;
		d.runs = new ArrayList<>(state.runs);
		d.seen = new HashMap<>(state.seen);
		d.paid = new HashSet<>(state.paid);
		Set<String> unpaid = new HashSet<>();
		for (RunTask task : tasks)
		{
			if (state.done.contains(task.getKey()) && task.getCrop().getPayment() != null
				&& task.getLocation().getGardener() >= 0 && !state.paid.contains(task.getKey()))
			{
				unpaid.add(task.getKey());
			}
		}
		d.unpaid = unpaid;
		Map<RouteStep, String> travel = new EnumMap<>(RouteStep.class);
		for (RouteStep step : RouteStep.values())
		{
			travel.put(step, travelNote(step));
		}
		d.travel = travel;
		d.bankFilter = config.bankFilter();
		d.highlightGardeners = config.highlightGardeners();

		// Herbs
		Set<String> herbDone = new HashSet<>(state.herbDone);
		Map<Integer, Integer> carried = new HashMap<>(inv);
		equipment.forEach((k, v) -> carried.merge(k, v, Integer::sum));
		d.herbTasks = herbTasks;
		d.herbDone = herbDone;
		d.herbActive = state.herbActive;
		d.herbStartedAt = state.herbStartedAt;
		d.herbNeeds = HerbPlanner.needs(herbTasks, herbDone, herbSettings, id -> carried.getOrDefault(id, 0));
		d.compost = herbSettings.compost;
		d.compostCarried = herbSettings.compost.isBottomless()
			? carried.getOrDefault(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, 0) > 0 : true;
		d.secateursExpected = config.magicSecateurs();
		d.secateursCarried = carried.getOrDefault(ItemID.FAIRY_ENCHANTED_SECATEURS, 0) > 0;
		d.herbPicked = state.herbPicked;
		d.herbValue = state.herbValue;
		d.herbCost = state.herbCost;
		d.herbXp = state.herbXp;
		d.herbRuns = new ArrayList<>(state.herbRuns);
		d.herbLastMatch = herbLastMatch;
		d.picker = HerbPlanner.picker(herbSettings, id -> itemManager.getItemPriceWithSource(id, true));
		Map<String, Long> perPatch = new HashMap<>();
		for (HerbTask task : herbTasks)
		{
			if (!task.isAllotment())
			{
				perPatch.put(task.getKey(), HerbPlanner.estimate(task.getHerb(), task.getLocation(), herbSettings,
					id -> itemManager.getItemPriceWithSource(id, true)).getProfit());
			}
		}
		d.herbEstimates = perPatch;
		d.herbXpPerRun = HerbPlanner.xpPerRun(herbSettings);
		d.herbRunsPerDay = config.herbRunsPerDay();
		if (config.herbsOnTheWay())
		{
			Map<RouteStep, List<HerbTask>> onTheWay = new EnumMap<>(RouteStep.class);
			for (HerbTask task : herbTasks)
			{
				RouteStep step = task.getLocation().getTreeStep();
				if (step != null && !task.isAllotment() && task.isDue()
					&& task.getCondition() != PatchCondition.UNKNOWN)
				{
					onTheWay.computeIfAbsent(step, s -> new ArrayList<>()).add(task);
				}
			}
			d.onTheWay = onTheWay;
		}

		// Profit
		d.lifetimeTreeProfit = RunRecord.lifetimeProfit(state.runs);
		d.lifetimeHerbProfit = RunRecord.lifetimeProfit(state.herbRuns);
		d.lastTreeRun = RunRecord.lastWithProfit(state.runs);
		d.lastHerbRun = RunRecord.lastWithProfit(state.herbRuns);

		updateTagItems(d.items, d.done, herbTasks, herbDone);
		notifyHerbsGrown(herbTasks);
		publish(d);
	}

	/** One notification each time all your herb patches have finished growing. */
	private void notifyHerbsGrown(List<HerbTask> herbTasks)
	{
		if (!config.herbNotify())
		{
			return;
		}
		long allReady = 0;
		int herbPatches = 0;
		for (HerbTask task : herbTasks)
		{
			if (task.isAllotment())
			{
				continue;
			}
			herbPatches++;
			if (!task.isDue())
			{
				return;
			}
			SeenPatch s = state.seen.get(task.getKey());
			if (s == null)
			{
				return;
			}
			allReady = Math.max(allReady, s.readyBy());
		}
		if (herbPatches > 0 && allReady > 0 && allReady > state.herbNotifiedFor)
		{
			state.herbNotifiedFor = allReady;
			saveState();
			notifier.notify("Your herb patches should be grown.");
		}
	}

	private void publish(PanelData snapshot)
	{
		snapshot.tagKey = treeTagItems.hashCode() * 31 + herbTagItems.hashCode();
		PanelData previous = data;
		data = snapshot;
		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.update(snapshot));
		}
		// Keep an open filter up to date as patches are done.
		if (previous != null && isBankOpen())
		{
			String active = bankTagsService.getActiveTag();
			if ((TREE_TAG.equals(active) || HERB_TAG.equals(active)) && previous.tagKey != snapshot.tagKey)
			{
				openBankFilter();
			}
		}
	}

	// ------------------------------------------------------------------ helpers

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

	private String where()
	{
		WorldPoint p = playerLocation();
		return p == null ? "unknown" : "region " + p.getRegionID() + " (" + p.getX() + "," + p.getY() + "," + p.getPlane() + ")";
	}

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

	private Map<Integer, Integer> countAll(ItemContainer container)
	{
		Map<Integer, Integer> counts = new HashMap<>();
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				if (item.getId() > 0)
				{
					counts.merge(item.getId(), item.getQuantity(), Integer::sum);
				}
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
