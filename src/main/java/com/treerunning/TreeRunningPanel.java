package com.treerunning;

import com.treerunning.core.BankStop;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RouteOrder;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunRecord;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.core.XpTable;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel with three tabs: XP (progress and all settings), Run and Supplies.
 */
class TreeRunningPanel extends PluginPanel
{
	private static final NumberFormat NUM = NumberFormat.getIntegerInstance();
	private static final Color GOOD = new Color(0x6C, 0xC2, 0x4A);
	private static final Color BAD = new Color(0xE0, 0x6C, 0x5A);
	private static final Color MUTED = ColorScheme.MEDIUM_GRAY_COLOR;
	private static final Color BANK = TreeRunningOverlay.BANK;
	private static final int TEXT_WIDTH = 165;

	/** Patch type colors for the "This run needs" box. */
	private static final Map<PatchType, Color> TYPE_COLORS = new EnumMap<>(PatchType.class);

	static
	{
		TYPE_COLORS.put(PatchType.TREE, new Color(0x7F, 0xD3, 0x5C));
		TYPE_COLORS.put(PatchType.FRUIT_TREE, new Color(0xFF, 0x9E, 0x7A));
		TYPE_COLORS.put(PatchType.HARDWOOD, new Color(0xD2, 0xA6, 0x79));
		TYPE_COLORS.put(PatchType.CALQUAT, new Color(0xF0, 0xD2, 0x64));
		TYPE_COLORS.put(PatchType.CELASTRUS, new Color(0xC3, 0xA6, 0xF2));
		TYPE_COLORS.put(PatchType.REDWOOD, new Color(0xEF, 0x7D, 0x6B));
		TYPE_COLORS.put(PatchType.HESPORI, new Color(0x6F, 0xD8, 0xC6));
	}

	private final TreeRunningPlugin plugin;
	private final JPanel statsPanel = column();
	private final JPanel settingsPanel = column();
	private final JPanel routePanel = column();
	private final JPanel runTab = column();
	private final JPanel suppliesTab = column();

	/** True while settings are being filled in, so their listeners don't save. */
	private boolean loading;
	private List<BankStop> bankStops = new ArrayList<>();
	private final Timer bankSaveTimer;
	private Predicate<PatchLocation> patchEnabled = location -> true;
	private RouteOrder routeOrder = RouteOrder.defaults();
	/** Travel notes the player typed, by stop. */
	private Map<RouteStep, String> travelNotes = new EnumMap<>(RouteStep.class);
	private final Timer travelSaveTimer;

	TreeRunningPanel(TreeRunningPlugin plugin)
	{
		super(false);
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		bankSaveTimer = new Timer(500, e -> saveBankStops());
		bankSaveTimer.setRepeats(false);
		travelSaveTimer = new Timer(600, e -> saveTravelNotes());
		travelSaveTimer.setRepeats(false);

		JPanel xpTab = column();
		xpTab.add(statsPanel);
		xpTab.add(settingsPanel);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("XP", scroll(xpTab));
		tabs.addTab("Run", scroll(runTab));
		tabs.addTab("Supplies", scroll(suppliesTab));
		add(tabs, BorderLayout.CENTER);

		update(PanelData.EMPTY);
	}

	/** Redraws the parts that change with the game. Settings are left alone so typing isn't interrupted. */
	void update(PanelData d)
	{
		buildStats(d);
		buildRun(d);
		buildSupplies(d);
		revalidate();
		repaint();
	}

	/** Run tab and overlay label: just the crop, plus East/Middle/West where a stop has several. */
	static String patchLabel(RunTask task)
	{
		String patch = task.getLocation().getPatchName();
		return task.getCrop().getDisplayName() + (patch != null ? " (" + patch + ")" : "");
	}

	// ------------------------------------------------------------------ XP tab: progress

	private void buildStats(PanelData d)
	{
		statsPanel.removeAll();
		if (!d.loggedIn)
		{
			statsPanel.add(text("Log in to see your Farming progress.", MUTED));
			return;
		}

		boolean byLevel = d.goalType == TreeRunningConfig.GoalType.LEVEL;
		statsPanel.add(heading(byLevel ? "Level " + d.level + " → " + d.goalLevel : "Goal: " + NUM.format(d.goalXp) + " XP"));
		if (!byLevel)
		{
			statsPanel.add(row("Current XP", NUM.format(d.currentXp) + " (level " + d.level + ")"));
		}

		// Level goals fill the bar from the start of your current level, XP goals from zero.
		int base = byLevel ? XpTable.xpForLevel(Math.min(d.level, d.goalLevel)) : 0;
		double span = Math.max(1, d.goalXp - base);
		JProgressBar bar = new JProgressBar(0, 1000);
		bar.setValue((int) (1000 * Math.max(0, Math.min(1, (d.currentXp - base) / span))));
		bar.setForeground(GOOD);
		bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bar.setStringPainted(true);
		bar.setString(NUM.format(d.xpRemaining()) + " XP to go");
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		bar.setAlignmentX(LEFT_ALIGNMENT);
		statsPanel.add(bar);
		statsPanel.add(Box.createVerticalStrut(6));

		if (d.xpRemaining() == 0)
		{
			statsPanel.add(text("Goal reached." + (d.level >= 99 ? " Enjoy the cape!" : ""), GOOD));
			return;
		}

		if (d.runsLeft > 0)
		{
			int days = (int) Math.ceil(d.runsLeft / (double) d.runsPerDay);
			statsPanel.add(big(d.runsLeft + " runs left"));
			statsPanel.add(row("Days left", String.valueOf(days)));
			statsPanel.add(row("Goal date", LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
			statsPanel.add(text(d.runsLeftFromActual
				? "Based on your last " + d.actualRunsCounted + " run" + (d.actualRunsCounted == 1 ? "" : "s") + "."
				: "Planned estimate. Finish a run to switch to your real XP.", MUTED));
		}
		else
		{
			statsPanel.add(text("Your current plan can't reach this goal. Check your crops and patches.", BAD));
		}

		statsPanel.add(Box.createVerticalStrut(6));
		statsPanel.add(heading("XP per run"));
		statsPanel.add(row("Your average", d.actualXpPerRun > 0 ? NUM.format(Math.round(d.actualXpPerRun)) : "–"));
		statsPanel.add(row("Planned", NUM.format(Math.round(d.plannedXpPerRun))));
		statsPanel.add(row("Planned runs", d.plannedRuns > 0 ? String.valueOf(d.plannedRuns) : "–"));
		if (d.runActive)
		{
			statsPanel.add(row("This run so far", NUM.format(d.runXpSoFar)));
		}

		statsPanel.add(Box.createVerticalStrut(6));
		statsPanel.add(heading("Run history"));
		if (d.runs.isEmpty())
		{
			statsPanel.add(text("No finished runs yet. Press Finish run at the end of a run to save it here.", MUTED));
			return;
		}
		DateTimeFormatter when = DateTimeFormatter.ofPattern("MMM d, HH:mm");
		// Newest first; the index is the run's place in the saved list.
		int shown = 0;
		for (int i = d.runs.size() - 1; i >= 0 && shown < 30; i--, shown++)
		{
			RunRecord run = d.runs.get(i);
			int index = i;
			String date = run.endedAt > 0
				? Instant.ofEpochMilli(run.endedAt).atZone(ZoneId.systemDefault()).format(when)
				: "Earlier run";
			statsPanel.add(historyRow(date, NUM.format(run.xp) + " XP" + (run.patches > 0 ? " · " + run.patches : ""),
				() -> plugin.deleteRun(index)));
		}
		if (d.runs.size() > 30)
		{
			statsPanel.add(text((d.runs.size() - 30) + " older runs not shown.", MUTED));
		}
		JButton clear = button("Clear all history");
		clear.addActionListener(e -> plugin.clearHistory());
		statsPanel.add(Box.createVerticalStrut(4));
		statsPanel.add(clear);
	}

	private static JPanel historyRow(String date, String value, Runnable delete)
	{
		JPanel panel = new JPanel(new BorderLayout(4, 0));
		panel.setOpaque(false);
		panel.add(small(date, ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.WEST);
		panel.add(number(value), BorderLayout.CENTER);
		JButton remove = tinyButton("✕", "Delete this run");
		remove.addActionListener(e -> delete.run());
		panel.add(remove, BorderLayout.EAST);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	// ------------------------------------------------------------------ XP tab: settings

	/** Builds the settings from the saved values. Called once at startup, on the Swing thread. */
	void loadSettings()
	{
		loadSettings(plugin.getConfig(), plugin.getBankStops(), plugin.getRouteOrder(), plugin.getTravelNotes(),
			plugin::isPatchEnabled);
	}

	void loadSettings(TreeRunningConfig config, List<BankStop> stops, RouteOrder order, Map<RouteStep, String> travel,
		Predicate<PatchLocation> patchEnabled)
	{
		loading = true;
		try
		{
			this.patchEnabled = patchEnabled;
			this.routeOrder = order;
			this.travelNotes = new EnumMap<>(RouteStep.class);
			this.travelNotes.putAll(travel);
			buildSettings(config);
			bankStops = new ArrayList<>(stops);
			buildRoute();
		}
		finally
		{
			loading = false;
		}
		revalidate();
		repaint();
	}

	private void buildSettings(TreeRunningConfig config)
	{
		settingsPanel.removeAll();
		settingsPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(8, 0, 0, 0)));
		settingsPanel.add(Box.createVerticalStrut(10));
		settingsPanel.add(heading("Settings"));

		settingsPanel.add(section("Crops"));
		settingsPanel.add(field("Tree patches", combo(CropChoices.Tree.values(), config.treeCrop(), "treeCrop")));
		settingsPanel.add(field("Fruit tree patches", combo(CropChoices.Fruit.values(), config.fruitCrop(), "fruitCrop")));
		settingsPanel.add(field("Hardwood patches", combo(CropChoices.Hardwood.values(), config.hardwoodCrop(), "hardwoodCrop")));

		settingsPanel.add(section("Goal"));
		JComboBox<TreeRunningConfig.GoalType> goalType = new JComboBox<>(TreeRunningConfig.GoalType.values());
		goalType.setSelectedItem(config.goalType());
		JSpinner goalLevel = spinner(config.goalLevel(), 2, 99, "goalLevel");
		JTextField goalXp = new JTextField(NUM.format(config.goalXp()));
		styleInput(goalXp);
		JComponent levelRow = field("Goal level", goalLevel);
		JComponent xpRow = field("Goal XP", goalXp);
		JLabel xpHint = text("Any amount up to 200,000,000. 99 is 13,034,431.", MUTED);
		Runnable showGoalRows = () ->
		{
			boolean byXp = goalType.getSelectedItem() == TreeRunningConfig.GoalType.XP;
			levelRow.setVisible(!byXp);
			xpRow.setVisible(byXp);
			xpHint.setVisible(byXp);
			settingsPanel.revalidate();
		};
		goalType.addActionListener(e ->
		{
			showGoalRows.run();
			if (!loading)
			{
				plugin.setSetting("goalType", goalType.getSelectedItem());
			}
		});
		Runnable saveGoalXp = () ->
		{
			String digits = goalXp.getText().replaceAll("[^0-9]", "");
			if (digits.isEmpty())
			{
				goalXp.setText(NUM.format(plugin.getConfig().goalXp()));
				return;
			}
			long value = Math.max(1, Math.min(200_000_000L, Long.parseLong(digits.length() > 10 ? "9999999999" : digits)));
			goalXp.setText(NUM.format(value));
			if (!loading)
			{
				plugin.setSetting("goalXp", (int) value);
			}
		};
		goalXp.addActionListener(e -> saveGoalXp.run());
		goalXp.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				saveGoalXp.run();
			}
		});
		settingsPanel.add(field("Goal type", goalType));
		settingsPanel.add(levelRow);
		settingsPanel.add(xpRow);
		settingsPanel.add(xpHint);
		settingsPanel.add(field("Runs per day", spinner(config.runsPerDay(), 1, 4, "runsPerDay")));
		showGoalRows.run();

		settingsPanel.add(section("Overlay"));
		settingsPanel.add(checkField("Show checklist overlay", config.showOverlay(), "showOverlay"));
		settingsPanel.add(checkField("Only during a run", config.overlayOnlyDuringRun(), "overlayOnlyDuringRun"));
		settingsPanel.add(checkField("Highlight patches in game", config.highlightPatches(), "highlightPatches"));
		settingsPanel.add(checkField("Hint arrow to next patch", config.hintArrow(), "hintArrow"));
		settingsPanel.add(text("Highlights show once a patch is loaded nearby. The next patch is bright green.", MUTED));

		settingsPanel.add(section("Route & patches"));
		settingsPanel.add(text("▲▼ move a stop or a patch. The line under each stop is the travel note shown in the run; "
			+ "type your own. Untick patches you don't use, and add bank stops where you restock.", MUTED));
		settingsPanel.add(routePanel);

		settingsPanel.add(section("Debug"));
		settingsPanel.add(checkField("Debug log", config.debugLog(), "debugLog"));
		settingsPanel.add(text("Writes what the tracker sees to RuneLite's log, .runelite/logs/client.log, on lines "
			+ "starting with [Tree Running]. Turn it on for a few runs if ticking misses a patch, then send that file.", MUTED));
	}

	/** The route editor: every stop, its patches, and its bank stops before or after them. */
	private void buildRoute()
	{
		routePanel.removeAll();
		routePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		List<RouteStep> stops = routeOrder.getStops();
		for (int s = 0; s < stops.size(); s++)
		{
			RouteStep step = stops.get(s);
			List<PatchLocation> locations = routeOrder.getPatches(step);
			if (locations.isEmpty())
			{
				continue;
			}

			JLabel head = heading(step.getDisplayName());
			head.setForeground(ColorScheme.BRAND_ORANGE);
			routePanel.add(Box.createVerticalStrut(6));
			routePanel.add(withArrows(head, s > 0, s < stops.size() - 1,
				() -> moveRoute(routeOrder.moveStop(step, -1)), () -> moveRoute(routeOrder.moveStop(step, 1)),
				"stop"));
			routePanel.add(travelField(step));

			addBankEditors(step, true);
			for (int p = 0; p < locations.size(); p++)
			{
				PatchLocation location = locations.get(p);
				String label = location.getPatchType().getDisplayName()
					+ (location.getPatchName() != null ? " (" + location.getPatchName() + ")" : "");
				JCheckBox box = checkBox(label, patchEnabled.test(location));
				if (location.getRequirement() != null)
				{
					box.setToolTipText("Requires " + location.getRequirement());
				}
				box.addActionListener(e ->
				{
					if (!loading)
					{
						plugin.setPatchEnabled(location, box.isSelected());
					}
				});
				routePanel.add(locations.size() < 2 ? box : withArrows(box, p > 0, p < locations.size() - 1,
					() -> moveRoute(routeOrder.movePatch(location, -1)), () -> moveRoute(routeOrder.movePatch(location, 1)),
					"patch"));
			}
			addBankEditors(step, false);

			JButton add = button("+ Add bank stop");
			add.setToolTipText("Add a bank stop at " + step.getDisplayName());
			add.setForeground(MUTED);
			add.addActionListener(e ->
			{
				BankStop stop = TreeRunningPlugin.newBankStop(step);
				bankStops.add(stop);
				saveBankStops();
				buildRoute();
				routePanel.revalidate();
				routePanel.repaint();
			});
			routePanel.add(add);
		}
	}

	private void moveRoute(RouteOrder order)
	{
		routeOrder = order;
		plugin.saveRouteOrder(order);
		buildRoute();
		routePanel.revalidate();
		routePanel.repaint();
	}

	/** A row with ▲▼ buttons on the right. */
	private static JComponent withArrows(JComponent content, boolean canUp, boolean canDown, Runnable up, Runnable down,
		String what)
	{
		JPanel row = new JPanel(new BorderLayout(2, 0));
		row.setOpaque(false);
		content.setAlignmentX(LEFT_ALIGNMENT);
		row.add(content, BorderLayout.CENTER);
		JPanel arrows = new JPanel(new GridLayout(1, 2, 1, 0));
		arrows.setOpaque(false);
		JButton upButton = tinyButton("▲", "Move this " + what + " up");
		upButton.setEnabled(canUp);
		upButton.addActionListener(e -> up.run());
		JButton downButton = tinyButton("▼", "Move this " + what + " down");
		downButton.setEnabled(canDown);
		downButton.addActionListener(e -> down.run());
		arrows.add(upButton);
		arrows.add(downButton);
		row.add(arrows, BorderLayout.EAST);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(22, content.getPreferredSize().height)));
		row.setAlignmentX(LEFT_ALIGNMENT);
		return row;
	}

	/** The travel note for a stop, editable. Empty hides it. */
	private JComponent travelField(RouteStep step)
	{
		String current = travelNotes.containsKey(step) ? travelNotes.get(step) : step.getTravel();
		JTextField field = new JTextField(current);
		styleInput(field);
		field.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		field.setToolTipText("How you get here. Built-in: " + step.getTravel());
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			private void changed()
			{
				String text = field.getText();
				if (text.equals(step.getTravel()))
				{
					travelNotes.remove(step);
				}
				else
				{
					travelNotes.put(step, text);
				}
				if (!loading)
				{
					travelSaveTimer.restart();
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}
		});
		field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		field.setAlignmentX(LEFT_ALIGNMENT);
		return field;
	}

	private void saveTravelNotes()
	{
		if (!loading)
		{
			travelSaveTimer.stop();
			plugin.saveTravelNotes(travelNotes);
		}
	}

	private void addBankEditors(RouteStep step, boolean before)
	{
		for (BankStop stop : bankStops)
		{
			if (stop.getRouteStep() == step && stop.before == before)
			{
				routePanel.add(bankEditor(stop));
			}
		}
	}

	private JComponent bankEditor(BankStop stop)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(4, 4, 4, 4)));
		box.setAlignmentX(LEFT_ALIGNMENT);

		JPanel line = new JPanel(new BorderLayout(4, 0));
		line.setOpaque(false);
		JLabel label = new JLabel("Bank");
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(BANK);
		JTextField name = new JTextField(stop.name);
		styleInput(name);
		name.setToolTipText("Where you bank, e.g. Catherby bank");
		name.getDocument().addDocumentListener(new DocumentListener()
		{
			private void changed()
			{
				stop.name = name.getText();
				bankSaveTimer.restart();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}
		});
		JButton remove = tinyButton("✕", "Remove this bank stop");
		remove.addActionListener(e ->
		{
			bankStops.remove(stop);
			saveBankStops();
			buildRoute();
			routePanel.revalidate();
			routePanel.repaint();
		});
		line.add(label, BorderLayout.WEST);
		line.add(name, BorderLayout.CENTER);
		line.add(remove, BorderLayout.EAST);
		line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		line.setAlignmentX(LEFT_ALIGNMENT);

		JComboBox<String> when = new JComboBox<>(new String[]{"Before the patches here", "After the patches here"});
		when.setSelectedIndex(stop.before ? 0 : 1);
		when.setFont(FontManager.getRunescapeSmallFont());
		when.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		when.setAlignmentX(LEFT_ALIGNMENT);
		when.addActionListener(e ->
		{
			boolean before = when.getSelectedIndex() == 0;
			if (before != stop.before)
			{
				stop.before = before;
				saveBankStops();
				// Move it above or below the patches.
				javax.swing.SwingUtilities.invokeLater(() ->
				{
					buildRoute();
					routePanel.revalidate();
					routePanel.repaint();
				});
			}
		});

		box.add(line);
		box.add(Box.createVerticalStrut(4));
		box.add(when);
		box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
		return box;
	}

	private void saveBankStops()
	{
		if (!loading)
		{
			bankSaveTimer.stop();
			plugin.saveBankStops(bankStops);
		}
	}

	// ------------------------------------------------------------------ Run tab

	private void buildRun(PanelData d)
	{
		runTab.removeAll();
		if (!d.loggedIn)
		{
			runTab.add(text("Log in to see your run.", MUTED));
			return;
		}

		runTab.add(heading(d.runActive
			? "Run in progress: " + d.doneCount() + "/" + d.dueCount()
			: "Ready: " + d.dueCount() + " patches due"));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		JButton start = button("New run");
		start.addActionListener(e -> plugin.startNewRun());
		JButton finish = button("Finish run");
		finish.setEnabled(d.runActive);
		finish.addActionListener(e -> plugin.finishRun());
		buttons.add(start);
		buttons.add(finish);
		buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		runTab.add(buttons);
		runTab.add(text("Patches tick when you check health or plant, in any order. Opening your bank ticks the next "
			+ "bank stop. Growth timers and run XP are saved when you press Finish run.", MUTED));
		if (d.lastMatch != null)
		{
			runTab.add(text("Last ticked: " + d.lastMatch, GOOD));
		}
		if (d.runActive && d.dueCount() > 0 && d.nextItem() == null)
		{
			runTab.add(Box.createVerticalStrut(4));
			runTab.add(doneBox(d));
		}

		if (!d.needs.isEmpty())
		{
			runTab.add(Box.createVerticalStrut(4));
			runTab.add(needsBox(d.needs));
		}

		RouteStep currentStep = null;
		for (RouteItem item : d.items)
		{
			if (item.getStep() != currentStep)
			{
				currentStep = item.getStep();
				JLabel location = heading(currentStep.getDisplayName());
				location.setForeground(ColorScheme.BRAND_ORANGE);
				runTab.add(Box.createVerticalStrut(6));
				runTab.add(location);
				String travel = d.travelNote(currentStep);
				if (!travel.trim().isEmpty())
				{
					runTab.add(text(travel, MUTED));
				}
			}

			String key = item.getKey();
			JCheckBox box;
			if (item.isBank())
			{
				box = checkBox(item.getBank().getLabel(), d.done.contains(key));
				box.setForeground(BANK);
			}
			else
			{
				RunTask task = item.getTask();
				// Only "Ready in…" is shown; patches that need a higher level are just greyed out.
				String note = !task.isDue() && task.getNote() != null && task.getNote().startsWith("Ready")
					? " (" + task.getNote() + ")" : "";
				box = checkBox(patchLabel(task) + note, d.done.contains(key));
				box.setForeground(task.isDue() ? Color.WHITE : MUTED);
				// Due patches can always be ticked or unticked; a ticked one can always be unticked.
				box.setEnabled(task.isDue() || d.done.contains(key));
			}
			box.addActionListener(e -> plugin.toggleTask(key));
			runTab.add(box);
		}

		if (d.tasks.isEmpty())
		{
			runTab.add(text("No patches enabled. Turn some on under Settings in the XP tab.", MUTED));
		}
	}

	/** One line per crop: saplings and protection, colored by patch type. */
	private JComponent needsBox(List<RunNeeds.Line> needs)
	{
		JPanel box = new JPanel(new GridBagLayout());
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(5, 6, 5, 6)));
		box.setAlignmentX(LEFT_ALIGNMENT);

		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;

		JLabel title = new JLabel("This run needs");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		c.gridx = 0;
		c.gridwidth = 3;
		c.weightx = 1;
		box.add(title, c);
		int shortCount = RunNeeds.shortCount(needs);
		JLabel total = small(shortCount > 0 ? shortCount + " short" : RunNeeds.totalSaplings(needs) + " to plant",
			shortCount > 0 ? BAD : MUTED);
		total.setToolTipText("Have / need, counting your inventory");
		total.setHorizontalAlignment(SwingConstants.RIGHT);
		c.gridx = 3;
		c.gridwidth = 1;
		c.weightx = 0;
		box.add(total, c);

		for (RunNeeds.Line line : needs)
		{
			c.gridy++;
			CropType crop = line.getCrop();
			Color color = TYPE_COLORS.getOrDefault(crop.getPatchType(), Color.WHITE);
			String cropName = crop.getSapling() != null ? crop.getDisplayName() : crop.getSeed().getName();

			c.gridx = 0;
			c.weightx = 0;
			c.insets = new Insets(1, 0, 0, 5);
			box.add(count(line.getSaplingsCarried(), line.getSaplings()), c);
			c.gridx = 1;
			c.weightx = 0.45;
			box.add(small(cropName, color), c);
			c.gridx = 2;
			c.weightx = 0;
			if (crop.getPayment() != null)
			{
				box.add(count(line.getPaymentCarried(), line.getPayment()), c);
				c.gridx = 3;
				c.weightx = 0.55;
				c.insets = new Insets(1, 0, 0, 0);
				box.add(small(crop.getPayment().getName(), color), c);
			}
			else
			{
				box.add(new JLabel(""), c);
				c.gridx = 3;
				c.weightx = 0.55;
				c.insets = new Insets(1, 0, 0, 0);
				box.add(small("No payment", MUTED), c);
			}
		}
		box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height + 4));
		return box;
	}

	/** "have/need" in red when you're short, or just the number when you have them all. */
	private static JLabel count(int have, int need)
	{
		if (have >= need)
		{
			return number(NUM.format(need));
		}
		JLabel label = number(NUM.format(have) + "/" + NUM.format(need));
		label.setForeground(BAD);
		label.setToolTipText("You have " + have + " of " + need + " in your inventory");
		return label;
	}

	private static JComponent doneBox(PanelData d)
	{
		JPanel box = column();
		box.setBackground(new Color(0x23, 0x3A, 0x1F));
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(GOOD), new EmptyBorder(5, 6, 5, 6)));
		box.add(heading("All " + d.dueCount() + " patches done"));
		box.add(text("Press Finish run to save " + NUM.format(d.runXpSoFar) + " XP to your history and start the "
			+ "growth timers. Unticked something by mistake? Tick it again first.", Color.WHITE));
		box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height + 4));
		return box;
	}

	// ------------------------------------------------------------------ Supplies tab

	private void buildSupplies(PanelData d)
	{
		suppliesTab.removeAll();
		if (!d.loggedIn)
		{
			suppliesTab.add(text("Log in to see your supplies.", MUTED));
			return;
		}

		suppliesTab.add(heading(d.runsLeft > 0 ? "Needed for " + d.runsLeft + " runs" : "Supplies"));
		suppliesTab.add(text("Have counts your bank, the Farming Guild seed vault and your inventory, as of the last time "
			+ "you opened each. Crop rows count seeds, seedlings and saplings together.", MUTED));
		suppliesTab.add(row("Bank", seen(d.bankSeenAt)));
		suppliesTab.add(row("Seed vault", seen(d.vaultSeenAt)));
		suppliesTab.add(Box.createVerticalStrut(4));

		JPanel grid = supplyGrid();
		addSupplyRow(grid, "Item", "Have", "Need", "GE ea", ColorScheme.LIGHT_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR);
		boolean paymentsStarted = false;
		int shortItems = 0;
		for (SupplyCalculator.SupplyLine line : d.supplies)
		{
			if (line.isPayment() && !paymentsStarted)
			{
				paymentsStarted = true;
				suppliesTab.add(grid);
				suppliesTab.add(Box.createVerticalStrut(4));
				suppliesTab.add(heading("Payments"));
				grid = supplyGrid();
			}
			if (line.getShort() > 0)
			{
				shortItems++;
			}
			addSupplyRow(grid, line.getName(), NUM.format(line.getHave()), NUM.format(line.getNeed()),
				line.getPriceEach() > 0 ? compact(line.getPriceEach()) : "–",
				line.getShort() > 0 ? BAD : GOOD, Color.WHITE);
		}
		suppliesTab.add(grid);

		suppliesTab.add(Box.createVerticalStrut(6));
		suppliesTab.add(text(shortItems == 0 ? "You have everything you need." : shortItems + " items short (in red).",
			shortItems == 0 ? GOOD : BAD));
		long cost = SupplyCalculator.shortCost(d.supplies);
		if (cost > 0)
		{
			suppliesTab.add(row("Cost to buy what's short", compact(cost) + " gp"));
		}
		suppliesTab.add(text("Prices: OSRS Wiki, kept up to date by RuneLite. Hespori seeds can't be traded.", MUTED));
	}

	private static JPanel supplyGrid()
	{
		JPanel grid = new JPanel(new GridBagLayout());
		grid.setOpaque(false);
		grid.setAlignmentX(LEFT_ALIGNMENT);
		return grid;
	}

	private static void addSupplyRow(JPanel grid, String name, String have, String need, String price, Color haveColor, Color needColor)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = grid.getComponentCount() / 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(1, 0, 0, 4);
		c.gridx = 0;
		c.weightx = 1;
		grid.add(small(name, ColorScheme.LIGHT_GRAY_COLOR), c);
		c.weightx = 0;
		c.gridx = 1;
		grid.add(fixed(have, haveColor, 40), c);
		c.gridx = 2;
		grid.add(fixed(need, needColor, 42), c);
		c.gridx = 3;
		c.insets = new Insets(1, 0, 0, 0);
		grid.add(fixed(price, MUTED, 40), c);
		grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
	}

	// ------------------------------------------------------------------ formatting

	/** 947, 12.7k, 31k, 6.3M. */
	static String compact(long n)
	{
		if (n < 1_000)
		{
			return String.valueOf(n);
		}
		if (n < 1_000_000)
		{
			return n < 10_000 ? String.format("%.1fk", n / 1000.0) : (n / 1000) + "k";
		}
		return String.format("%.1fM", n / 1_000_000.0);
	}

	private static String seen(long at)
	{
		if (at <= 0)
		{
			return "open it once";
		}
		long minutes = Math.max(0, (System.currentTimeMillis() - at) / 60_000L);
		if (minutes < 1)
		{
			return "counted just now";
		}
		if (minutes < 60)
		{
			return "counted " + minutes + "m ago";
		}
		long hours = minutes / 60;
		return hours < 24 ? "counted " + hours + "h ago" : "counted " + (hours / 24) + "d ago";
	}

	// ------------------------------------------------------------------ small widgets

	private <E> JComboBox<E> combo(E[] values, E selected, String key)
	{
		JComboBox<E> box = new JComboBox<>(values);
		box.setSelectedItem(selected);
		box.addActionListener(e ->
		{
			if (!loading)
			{
				plugin.setSetting(key, box.getSelectedItem());
			}
		});
		return box;
	}

	private JSpinner spinner(int value, int min, int max, String key)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, value)), min, max, 1));
		spinner.addChangeListener(e ->
		{
			if (!loading)
			{
				plugin.setSetting(key, spinner.getValue());
			}
		});
		return spinner;
	}

	private JComponent checkField(String label, boolean value, String key)
	{
		JCheckBox box = checkBox(label, value);
		box.addActionListener(e ->
		{
			if (!loading)
			{
				plugin.setSetting(key, box.isSelected());
			}
		});
		return box;
	}

	private static JComponent field(String label, JComponent control)
	{
		JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setOpaque(false);
		JLabel l = small(label, ColorScheme.LIGHT_GRAY_COLOR);
		control.setPreferredSize(new Dimension(100, 24));
		control.setFont(FontManager.getRunescapeSmallFont());
		panel.add(l, BorderLayout.CENTER);
		panel.add(control, BorderLayout.EAST);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		panel.setBorder(new EmptyBorder(1, 0, 1, 0));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static void styleInput(JTextField field)
	{
		field.setFont(FontManager.getRunescapeSmallFont());
		field.setBackground(ColorScheme.DARK_GRAY_COLOR);
		field.setForeground(Color.WHITE);
		field.setCaretColor(Color.WHITE);
	}

	private static JPanel column()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static JComponent scroll(JPanel content)
	{
		content.setBorder(new EmptyBorder(8, 8, 8, 8));
		JPanel wrapper = new WidthTrackingPanel();
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(content, BorderLayout.NORTH);
		JScrollPane pane = new JScrollPane(wrapper);
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.getVerticalScrollBar().setUnitIncrement(16);
		pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return pane;
	}

	private static JLabel section(String s)
	{
		JLabel label = heading(s);
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setBorder(new EmptyBorder(8, 0, 2, 0));
		return label;
	}

	private static JLabel heading(String s)
	{
		JLabel label = new JLabel("<html><body style='width:" + TEXT_WIDTH + "px'>" + escape(s) + "</body></html>");
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(2, 0, 2, 0));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JLabel big(String s)
	{
		JLabel label = new JLabel(s);
		label.setAlignmentX(LEFT_ALIGNMENT);
		label.setFont(FontManager.getRunescapeBoldFont().deriveFont(20f));
		label.setForeground(GOOD);
		return label;
	}

	private static JLabel text(String s, Color color)
	{
		JLabel label = new JLabel("<html><body style='width:" + TEXT_WIDTH + "px'>" + escape(s) + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JLabel small(String s, Color color)
	{
		JLabel label = new JLabel(s);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	private static JLabel number(String s)
	{
		JLabel label = small(s, Color.WHITE);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}

	private static JLabel fixed(String s, Color color, int width)
	{
		JLabel label = small(s, color);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setPreferredSize(new Dimension(width, 15));
		return label;
	}

	private static JPanel row(String left, String right)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.add(small(left, ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.WEST);
		panel.add(small(right, Color.WHITE), BorderLayout.EAST);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static JCheckBox checkBox(String label, boolean selected)
	{
		JCheckBox box = new JCheckBox("<html><body style='width:150px'>" + escape(label) + "</body></html>", selected);
		box.setOpaque(false);
		box.setForeground(Color.WHITE);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setAlignmentX(LEFT_ALIGNMENT);
		return box;
	}

	private static JButton button(String s)
	{
		JButton b = new JButton(s);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setFocusPainted(false);
		b.setAlignmentX(LEFT_ALIGNMENT);
		return b;
	}

	private static JButton tinyButton(String s, String tooltip)
	{
		JButton b = new JButton(s);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setFocusPainted(false);
		b.setMargin(new Insets(0, 3, 0, 3));
		b.setToolTipText(tooltip);
		return b;
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Scrollable content that always matches the panel width, so nothing runs off the right edge. */
	private static final class WidthTrackingPanel extends JPanel implements Scrollable
	{
		WidthTrackingPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return 64;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
