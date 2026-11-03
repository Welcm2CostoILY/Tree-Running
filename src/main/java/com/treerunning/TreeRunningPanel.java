package com.treerunning;

import com.treerunning.core.BankStop;
import com.treerunning.core.HerbPlanner;
import com.treerunning.core.HerbTask;
import com.treerunning.core.RouteItem;
import com.treerunning.core.RouteOrder;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.RunRecord;
import com.treerunning.core.RunTask;
import com.treerunning.core.SeenPatch;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.core.XpTable;
import com.treerunning.data.CropType;
import com.treerunning.data.HerbLocation;
import com.treerunning.data.PatchCondition;
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
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel with four tabs: XP (progress, profit, history and the route editor), Trees,
 * Herbs and Supplies. Settings are in RuneLite's own configuration panel.
 */
class TreeRunningPanel extends PluginPanel
{
	private static final NumberFormat NUM = NumberFormat.getIntegerInstance();
	private static final Color GOOD = new Color(0x6C, 0xC2, 0x4A);
	private static final Color BAD = new Color(0xE0, 0x6C, 0x5A);
	private static final Color WARN = new Color(0xE8, 0xC3, 0x4A);
	private static final Color MUTED = ColorScheme.MEDIUM_GRAY_COLOR;
	private static final Color BANK = TreeRunningOverlay.BANK;
	private static final Color HERB = TreeRunningOverlay.HERB;
	private static final Color PAY = TreeRunningOverlay.PAY;
	private static final Color DF = new Color(0x7C, 0xC4, 0xFF);
	private static final int TEXT_WIDTH = 165;
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, HH:mm");
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

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
		TYPE_COLORS.put(PatchType.CRYSTAL, new Color(0x8F, 0xE3, 0xE8));
	}

	private final TreeRunningPlugin plugin;
	private final JPanel statsPanel = column();
	private final JPanel settingsPanel = column();
	private final JPanel routePanel = column();
	private final JPanel runTab = column();
	private final JPanel herbTab = column();
	private final JPanel suppliesTab = column();

	/** True while the route editor is being filled in, so its listeners don't save. */
	private boolean loading;
	private List<BankStop> bankStops = new ArrayList<>();
	private final Timer bankSaveTimer;
	private Predicate<PatchLocation> patchEnabled = location -> true;
	private RouteOrder routeOrder = RouteOrder.defaults();
	private Map<RouteStep, String> travelNotes = new EnumMap<>(RouteStep.class);
	private Map<HerbLocation, String> herbTravelNotes = new EnumMap<>(HerbLocation.class);
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
		tabs.addTab("Trees", scroll(runTab));
		tabs.addTab("Herbs", scroll(herbTab));
		tabs.addTab("Supplies", scroll(suppliesTab));
		add(tabs, BorderLayout.CENTER);

		update(PanelData.EMPTY);
	}

	/** Redraws the parts that change with the game. The route editor is left alone so typing isn't interrupted. */
	void update(PanelData d)
	{
		buildStats(d);
		buildRun(d);
		buildHerbs(d);
		buildSupplies(d);
		revalidate();
		repaint();
	}

	/** Trees tab and overlay label: just the crop, plus East/Middle/West where a stop has several. */
	static String patchLabel(RunTask task)
	{
		String patch = task.getLocation().getPatchName();
		return task.getCrop().getDisplayName() + (patch != null ? " (" + patch + ")" : "");
	}

	// ------------------------------------------------------------------ XP tab: progress, profit, history

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
		}
		else if (d.herbRunsPerDay > 0 && d.daysLeft() > 0)
		{
			int days = d.daysLeft();
			statsPanel.add(big(days + " days left"));
			statsPanel.add(text("With " + d.runsPerDay + " tree run" + (d.runsPerDay == 1 ? "" : "s") + " and "
				+ d.herbRunsPerDay + " herb run" + (d.herbRunsPerDay == 1 ? "" : "s") + " a day.", MUTED));
			statsPanel.add(row("Goal date", LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
			if (d.runsLeft > 0)
			{
				statsPanel.add(row("Trees only", (int) Math.ceil(d.runsLeft / (double) d.runsPerDay) + " days"));
			}
		}
		else if (d.runsLeft > 0)
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
			statsPanel.add(text("Your current plan can't reach this goal. Check your crops and patches in the settings.", BAD));
		}

		statsPanel.add(Box.createVerticalStrut(6));
		statsPanel.add(heading("XP per run"));
		statsPanel.add(row("Tree run: your average", d.actualXpPerRun > 0 ? NUM.format(Math.round(d.actualXpPerRun)) : "–"));
		statsPanel.add(row("Tree run: planned", NUM.format(Math.round(d.plannedXpPerRun))));
		statsPanel.add(row("Herb run: estimate", d.herbXpPerRun > 0 ? NUM.format(Math.round(d.herbXpPerRun)) : "–"));
		if (d.runActive)
		{
			statsPanel.add(row("This tree run so far", NUM.format(d.runXpSoFar)));
		}

		statsPanel.add(Box.createVerticalStrut(6));
		statsPanel.add(heading("Profit"));
		long lifetime = d.lifetimeTreeProfit + d.lifetimeHerbProfit;
		statsPanel.add(gpRow("Lifetime", lifetime));
		statsPanel.add(gpRow("  Herb runs", d.lifetimeHerbProfit));
		statsPanel.add(gpRow("  Tree runs", d.lifetimeTreeProfit));
		statsPanel.add(d.lastHerbRun != null ? gpRow("Last herb run", d.lastHerbRun.profit()) : row("Last herb run", "–"));
		statsPanel.add(d.lastTreeRun != null ? gpRow("Last tree run", d.lastTreeRun.profit()) : row("Last tree run", "–"));
		statsPanel.add(text("Harvests at live prices, minus seeds, compost and payments. Tree runs count fruit and bark "
			+ "picked, against each sapling's seed and the payments you made. Runs from before this version aren't counted.", MUTED));

		statsPanel.add(Box.createVerticalStrut(6));
		statsPanel.add(heading("Tree run history"));
		addHistory(statsPanel, d.runs, i -> plugin.deleteRun(i), () -> plugin.clearHistory(), false);
	}

	private void addHistory(JPanel panel, List<RunRecord> runs, java.util.function.IntConsumer delete, Runnable clearAll, boolean herbs)
	{
		if (runs.isEmpty())
		{
			panel.add(text("No finished runs yet. Press Finish run at the end of a run to save it here.", MUTED));
			return;
		}
		int shown = 0;
		for (int i = runs.size() - 1; i >= 0 && shown < 30; i--, shown++)
		{
			RunRecord run = runs.get(i);
			int index = i;
			String date = run.endedAt > 0 ? Instant.ofEpochMilli(run.endedAt).atZone(ZoneId.systemDefault()).format(WHEN) : "Earlier run";
			StringBuilder value = new StringBuilder(NUM.format(run.xp) + " XP");
			if (run.patches > 0)
			{
				value.append(" · ").append(run.patches);
			}
			long ms = run.durationMs();
			long best = RunRecord.bestTime(runs, run.patches);
			StringBuilder second = new StringBuilder();
			if (ms > 0)
			{
				second.append(duration(ms)).append(ms == best ? " (best)" : "");
			}
			if (run.profitTracked)
			{
				second.append(second.length() > 0 ? " · " : "").append(gp(run.profit()));
				if (herbs && run.picked > 0)
				{
					second.append(" · ").append(run.picked).append(" picked");
				}
				if (herbs && run.seedCost > 0)
				{
					second.append(" · seeds −").append(compact(run.seedCost));
				}
			}
			panel.add(historyRow(date, value.toString(), second.toString(), () -> delete.accept(index)));
		}
		if (runs.size() > 30)
		{
			panel.add(text((runs.size() - 30) + " older runs not shown.", MUTED));
		}
		JButton clear = button("Clear all history");
		clear.addActionListener(e -> clearAll.run());
		panel.add(Box.createVerticalStrut(4));
		panel.add(clear);
	}

	private static JPanel historyRow(String date, String value, String second, Runnable delete)
	{
		JPanel outer = new JPanel(new BorderLayout(4, 0));
		outer.setOpaque(false);
		JPanel lines = new JPanel(new GridLayout(second.isEmpty() ? 1 : 2, 1));
		lines.setOpaque(false);
		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(small(date, ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.WEST);
		top.add(number(value), BorderLayout.CENTER);
		lines.add(top);
		if (!second.isEmpty())
		{
			JLabel l = small(second, MUTED);
			l.setHorizontalAlignment(SwingConstants.RIGHT);
			lines.add(l);
		}
		outer.add(lines, BorderLayout.CENTER);
		JButton remove = tinyButton("✕", "Delete this run");
		remove.addActionListener(e -> delete.run());
		outer.add(remove, BorderLayout.EAST);
		outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, second.isEmpty() ? 20 : 34));
		outer.setAlignmentX(LEFT_ALIGNMENT);
		return outer;
	}

	// ------------------------------------------------------------------ XP tab: settings note and route editor

	/** Builds the route editor from the saved values. Called once at startup, on the Swing thread. */
	void loadRouteEditor()
	{
		loadRouteEditor(plugin.getBankStops(), plugin.getRouteOrder(), plugin.getTravelNotes(), plugin.getHerbTravelNotes(),
			plugin::isPatchEnabled, plugin::isHerbEnabled);
	}

	void loadRouteEditor(List<BankStop> stops, RouteOrder order, Map<RouteStep, String> travel, Map<HerbLocation, String> herbTravel,
		Predicate<PatchLocation> patchEnabled, Predicate<HerbLocation> herbEnabled)
	{
		loading = true;
		try
		{
			this.patchEnabled = patchEnabled;
			this.routeOrder = order;
			this.travelNotes = new EnumMap<>(RouteStep.class);
			this.travelNotes.putAll(travel);
			this.herbTravelNotes = new EnumMap<>(HerbLocation.class);
			this.herbTravelNotes.putAll(herbTravel);
			settingsPanel.removeAll();
			settingsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(8, 0, 0, 0)));
			settingsPanel.add(Box.createVerticalStrut(10));
			settingsPanel.add(heading("Settings have moved"));
			settingsPanel.add(text("Crops, goal, patches, herbs, overlays and the debug log are now in RuneLite's settings: "
				+ "click the wrench (Configuration) at the top of the side bar, then Tree Running. The route editor stays here.", WARN));
			settingsPanel.add(section("Route editor"));
			settingsPanel.add(text("▲▼ move a stop or a patch. The line under each stop is the travel note shown in the run; "
				+ "type your own. Add bank stops where you restock. Turn patches on or off in the settings.", MUTED));
			bankStops = new ArrayList<>(stops);
			buildRoute();
			settingsPanel.add(routePanel);

			settingsPanel.add(section("Herb run travel"));
			settingsPanel.add(text("The travel note shown for each herb patch. Type your own; empty hides it. Herb patches "
				+ "that are off in the settings are greyed out.", MUTED));
			for (HerbLocation location : HerbLocation.values())
			{
				boolean on = herbEnabled.test(location);
				JLabel name = small(location.getDisplayName() + (on ? "" : " (off in settings)"), on ? ColorScheme.BRAND_ORANGE : MUTED);
				name.setBorder(new EmptyBorder(4, 0, 1, 0));
				name.setAlignmentX(LEFT_ALIGNMENT);
				settingsPanel.add(name);
				settingsPanel.add(travelField(location.getTravel(),
					herbTravelNotes.containsKey(location) ? herbTravelNotes.get(location) : location.getTravel(),
					text ->
					{
						if (text.equals(location.getTravel()))
						{
							herbTravelNotes.remove(location);
						}
						else
						{
							herbTravelNotes.put(location, text);
						}
					}));
			}
		}
		finally
		{
			loading = false;
		}
		revalidate();
		repaint();
	}

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
			routePanel.add(travelField(step.getTravel(), travelNotes.containsKey(step) ? travelNotes.get(step) : step.getTravel(),
				text ->
				{
					if (text.equals(step.getTravel()))
					{
						travelNotes.remove(step);
					}
					else
					{
						travelNotes.put(step, text);
					}
				}));

			addBankEditors(step, true);
			for (int p = 0; p < locations.size(); p++)
			{
				PatchLocation location = locations.get(p);
				String label = location.getPatchType().getDisplayName()
					+ (location.getPatchName() != null ? " (" + location.getPatchName() + ")" : "");
				boolean on = patchEnabled.test(location);
				JLabel box = small("  " + label + (on ? "" : " (off in settings)"), on ? Color.WHITE : MUTED);
				if (location.getRequirement() != null)
				{
					box.setToolTipText("Requires " + location.getRequirement());
				}
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

	/** An editable travel note. Empty hides it; typing the built-in text back restores the default. */
	private JComponent travelField(String builtIn, String current, java.util.function.Consumer<String> onChange)
	{
		JTextField field = new JTextField(current);
		styleInput(field);
		field.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		field.setToolTipText("How you get here. Built-in: " + builtIn);
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			private void changed()
			{
				onChange.accept(field.getText());
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
			plugin.saveTravelNotes(travelNotes, herbTravelNotes);
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


	// ------------------------------------------------------------------ Trees tab

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
		if (d.runActive && d.runStartedAt > 0)
		{
			runTab.add(text("Started " + Instant.ofEpochMilli(d.runStartedAt).atZone(ZoneId.systemDefault()).format(CLOCK)
				+ " · " + duration(System.currentTimeMillis() - d.runStartedAt) + " so far", MUTED));
		}

		runTab.add(runButtons(() -> plugin.startNewRun(), () -> plugin.finishRun(), d.runActive));
		runTab.add(text("Patches tick when you check health or plant, in any order. Opening your bank ticks the next "
			+ "bank stop. Growth timers, run time and profit are saved when you press Finish run.", MUTED));
		if (d.lastMatch != null)
		{
			runTab.add(text("Last ticked: " + d.lastMatch, GOOD));
		}
		if (d.runActive && d.dueCount() > 0 && d.nextItem() == null)
		{
			runTab.add(Box.createVerticalStrut(4));
			runTab.add(doneBox(d));
		}
		runTab.add(bankFilterBox(d, TreeRunningConfig.BankFilter.TREE));

		if (!d.unpaid.isEmpty())
		{
			StringBuilder names = new StringBuilder();
			for (RunTask task : d.tasks)
			{
				if (d.unpaid.contains(task.getKey()))
				{
					names.append(names.length() > 0 ? ", " : "").append(patchLabel(task)).append(" (").append(task.getStep().getDisplayName()).append(")");
				}
			}
			runTab.add(text("Not paid yet: " + names + ". Pay the gardener to protect it; they're outlined in game.", PAY));
		}
		if (d.overflow != null)
		{
			runTab.add(text("This run needs about " + d.overflow.getSlots() + " inventory slots between bank stops"
				+ (d.overflow.getBankBefore() != null ? ". Add a bank stop before " + d.overflow.getBankBefore().getDisplayName() : "")
				+ ", or wear your teleport items.", BAD));
		}

		if (!d.needs.isEmpty())
		{
			runTab.add(Box.createVerticalStrut(4));
			runTab.add(needsBox(d.needs, d.slots));
		}

		long now = System.currentTimeMillis();
		RouteStep currentStep = null;
		for (RouteItem item : d.items)
		{
			if (item.getStep() != currentStep)
			{
				addOnTheWay(d, currentStep);
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
				String note = !task.isDue() && task.getNote() != null && task.getNote().startsWith("Ready")
					? " (" + task.getNote() + ")" : "";
				SeenPatch seenPatch = d.seen.get(key);
				Long planted = d.plantedAt.get(key);
				String state = seenPatch != null && (planted == null || seenPatch.seenAt >= planted) ? stateLabel(seenPatch, now) : "";
				String pay = d.done.contains(key) && task.getCrop().getPayment() != null && task.getLocation().getGardener() >= 0
					? (d.paid.contains(key) ? " · paid" : " · not paid") : "";
				box = checkBox(patchLabel(task) + note + state + pay, d.done.contains(key));
				SeenPatch s = d.seen.get(key);
				boolean bad = s != null && (s.condition == PatchCondition.DISEASED || s.condition == PatchCondition.DEAD);
				box.setForeground(bad ? BAD : d.unpaid.contains(key) ? PAY : task.isDue() ? Color.WHITE : MUTED);
				box.setEnabled(task.isDue() || d.done.contains(key));
			}
			box.addActionListener(e -> plugin.toggleTask(key));
			runTab.add(box);
		}
		addOnTheWay(d, currentStep);

		if (d.tasks.isEmpty())
		{
			runTab.add(text("No patches on. Turn some on in the settings (Tree patches).", MUTED));
		}
	}

	/** "Herbs on the way": grown herb patches at a tree-run stop. */
	private void addOnTheWay(PanelData d, RouteStep step)
	{
		if (step == null)
		{
			return;
		}
		for (HerbTask herb : d.onTheWay.getOrDefault(step, Collections.emptyList()))
		{
			List<String> plantKeys = herb.stepKeys(HerbTask.PLANT);
			JCheckBox box = checkBox("Herb: " + herb.getPlantName() + " (" + herb.getCondition().getLabel().toLowerCase() + ")",
				herb.isDone(d.herbDone, HerbTask.PLANT));
			box.setForeground(HERB);
			box.setToolTipText("Herbs on the way. Ticks itself when you plant; it's saved with your herb runs.");
			box.addActionListener(e -> plugin.toggleHerbStep(plantKeys));
			runTab.add(box);
		}
	}

	/** " – grown", " – diseased" and so on, from when the patch was last seen. */
	private static String stateLabel(SeenPatch s, long now)
	{
		if (s == null || s.condition == PatchCondition.UNKNOWN)
		{
			return "";
		}
		PatchCondition c = s.predict(now);
		switch (c)
		{
			case GROWING:
				return "";
			case GROWN:
				return s.condition == PatchCondition.GROWING ? " – should be grown" : " – grown";
			default:
				return " – " + c.getLabel().toLowerCase();
		}
	}

	private JComponent bankFilterBox(PanelData d, TreeRunningConfig.BankFilter mode)
	{
		JCheckBox box = checkBox("Filter bank to this run", d.bankFilter == mode);
		box.setToolTipText("Shows only what this run needs when you open the bank. Also on the bank's \"Show worn items\" right-click menu.");
		box.addActionListener(e -> plugin.setBankFilter(box.isSelected() ? mode : TreeRunningConfig.BankFilter.OFF));
		return box;
	}

	private static JPanel runButtons(Runnable start, Runnable finish, boolean active)
	{
		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		JButton startButton = button("New run");
		startButton.addActionListener(e -> start.run());
		JButton finishButton = button("Finish run");
		finishButton.setEnabled(active);
		finishButton.addActionListener(e -> finish.run());
		buttons.add(startButton);
		buttons.add(finishButton);
		buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		return buttons;
	}

	// ------------------------------------------------------------------ Herbs tab

	private void buildHerbs(PanelData d)
	{
		herbTab.removeAll();
		if (!d.loggedIn)
		{
			herbTab.add(text("Log in to see your herb patches.", MUTED));
			return;
		}
		if (d.herbTasks.isEmpty())
		{
			herbTab.add(heading("No herb patches on"));
			herbTab.add(text("Turn herb patches on in RuneLite's settings: the wrench, then Tree Running, then Herb patches.", MUTED));
			return;
		}

		long now = System.currentTimeMillis();
		int herbPatches = 0;
		int ready = 0;
		int unseen = 0;
		long nextGrown = 0;
		String nextName = null;
		long allGrown = 0;
		for (HerbTask task : d.herbTasks)
		{
			if (task.isAllotment())
			{
				continue;
			}
			herbPatches++;
			if (task.getCondition() == PatchCondition.UNKNOWN)
			{
				unseen++;
			}
			else if (task.isDue())
			{
				ready++;
			}
			else if (task.getReadyAt() > 0)
			{
				if (nextGrown == 0 || task.getReadyAt() < nextGrown)
				{
					nextGrown = task.getReadyAt();
					nextName = task.getLocation().getDisplayName();
				}
				allGrown = Math.max(allGrown, task.getReadyAt());
			}
		}
		herbTab.add(heading(d.herbActive
			? "Herb run in progress: " + d.herbPlantedCount() + "/" + d.herbDueCount()
			: ready + " of " + herbPatches + " herb patches ready" + (unseen > 0 ? ", " + unseen + " not seen yet" : "")));
		if (nextName != null)
		{
			herbTab.add(text("Next grown: " + nextName + " in " + duration(nextGrown - now) + ". All grown in "
				+ duration(allGrown - now) + ".", MUTED));
		}
		if (d.herbActive && d.herbStartedAt > 0)
		{
			herbTab.add(text("Started " + Instant.ofEpochMilli(d.herbStartedAt).atZone(ZoneId.systemDefault()).format(CLOCK)
				+ " · " + duration(now - d.herbStartedAt) + " so far", MUTED));
		}
		herbTab.add(runButtons(() -> plugin.startNewHerbRun(), () -> plugin.finishHerbRun(), d.herbActive));
		herbTab.add(bankFilterBox(d, TreeRunningConfig.BankFilter.HERB));
		if (d.herbLastMatch != null)
		{
			herbTab.add(text("Last: " + d.herbLastMatch, GOOD));
		}

		// Needs, at the top like the Trees tab
		herbTab.add(Box.createVerticalStrut(4));
		herbTab.add(herbNeedsBox(d));

		// Patches
		herbTab.add(section("Patches"));
		for (HerbTask task : d.herbTasks)
		{
			herbTab.add(patchCard(d, task, now));
		}
		herbTab.add(text("DF is disease-free. Choose each patch's herb in the settings, so your most valuable seeds go in "
			+ "the DF patches.", MUTED));

		// Checklist
		herbTab.add(section("Run checklist"));
		HerbLocation current = null;
		List<String> growing = new ArrayList<>();
		for (HerbTask task : d.herbTasks)
		{
			if (!task.isDue())
			{
				growing.add(task.getLocation().getDisplayName() + (task.isAllotment() ? " " + task.getLabel().toLowerCase() : "")
					+ (task.getReadyAt() > 0 ? " (" + duration(task.getReadyAt() - now) + ")" : ""));
				continue;
			}
			if (task.getLocation() != current)
			{
				current = task.getLocation();
				JLabel loc = heading(current.getDisplayName() + (current.isDiseaseFree() ? "  DF" : ""));
				loc.setForeground(ColorScheme.BRAND_ORANGE);
				herbTab.add(Box.createVerticalStrut(4));
				herbTab.add(loc);
				String travel = d.travelNote(current);
				if (!travel.trim().isEmpty())
				{
					herbTab.add(text(travel, MUTED));
				}
			}
			if (task.isAllotment() || d.herbTasks.stream().anyMatch(t -> t.isAllotment() && t.getLocation() == task.getLocation() && t.isDue()))
			{
				// Only label the two when a location has both.
				JLabel sub = small(task.getLabel(), ColorScheme.LIGHT_GRAY_COLOR);
				sub.setBorder(new EmptyBorder(2, 2, 0, 0));
				herbTab.add(sub);
			}
			for (HerbTask.Step step : task.getSteps())
			{
				List<String> stepKeys = task.stepKeys(step.getId());
				int doneHere = task.doneCount(d.herbDone, step.getId());
				String progress = task.getPatchCount() > 1 && doneHere > 0 && doneHere < task.getPatchCount()
					? " (" + doneHere + "/" + task.getPatchCount() + ")" : "";
				JCheckBox box = checkBox(step.getLabel() + progress, task.isDone(d.herbDone, step.getId()));
				box.addActionListener(e -> plugin.toggleHerbStep(stepKeys));
				herbTab.add(box);
			}
		}
		if (!growing.isEmpty())
		{
			herbTab.add(text("Still growing: " + String.join(", ", growing), MUTED));
		}

		// Profit this run
		herbTab.add(section("Profit: this run"));
		if (d.herbActive || d.herbPicked > 0)
		{
			herbTab.add(row("Picked", NUM.format(d.herbPicked)));
			herbTab.add(gpRow("Harvest value", d.herbValue));
			if (d.countSeeds)
			{
				herbTab.add(gpRow("Seeds planted", -d.herbSeedCost));
			}
			else
			{
				herbTab.add(row("Seeds planted", "not counted"));
			}
			for (Map.Entry<String, Integer> seed : d.herbSeeds.entrySet())
			{
				herbTab.add(row("  " + seed.getValue() + " × " + seed.getKey() + " seed", ""));
			}
			herbTab.add(gpRow("Compost", -d.herbCompostCost));
			long payments = d.herbCost - d.herbSeedCost - d.herbCompostCost;
			if (payments > 0)
			{
				herbTab.add(gpRow("Payments", -payments));
			}
			herbTab.add(gpRow("Profit", d.herbValue - d.herbCost));
			herbTab.add(row("Farming XP", NUM.format(d.herbXp)));
		}
		else
		{
			herbTab.add(text("Starts when you harvest, compost or plant the first patch.", MUTED));
		}

		// History
		herbTab.add(section("Herb run history"));
		addHistory(herbTab, d.herbRuns, i -> plugin.deleteHerbRun(i), () -> plugin.clearHerbHistory(), true);

		// Picker
		herbTab.add(section("Which herb pays best"));
		herbTab.add(text(d.countSeeds
			? "Per patch at live prices: the herbs you'd pick on average, minus the seed and compost, with the bonuses "
			+ "in your settings. Disease-free patches do a little better."
			: "Per patch at live prices: the herbs you'd pick on average, minus compost. Seed costs are off (ironman).", MUTED));
		JPanel grid = supplyGrid();
		addPickerRow(grid, "Herb", "Value", "Seed", "Profit", "XP", ColorScheme.LIGHT_GRAY_COLOR, ColorScheme.LIGHT_GRAY_COLOR);
		for (HerbPlanner.Estimate e : d.picker)
		{
			addPickerRow(grid, e.getHerb().getDisplayName(), compact(e.getValue()),
				d.countSeeds && e.getSeedCost() > 0 ? "−" + compact(e.getSeedCost()) : "–", gp(e.getProfit()), NUM.format(Math.round(e.getXp())),
				d.countSeeds ? BAD : MUTED, e.getProfit() >= 0 ? GOOD : BAD);
		}
		herbTab.add(grid);
	}

	private JComponent patchCard(PanelData d, HerbTask task, long now)
	{
		JPanel card = new JPanel(new BorderLayout(4, 0));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(3, 5, 3, 5)));
		card.setAlignmentX(LEFT_ALIGNMENT);

		String name = task.getLocation().getDisplayName() + (task.isAllotment() ? " " + task.getLabel().toLowerCase() : "");
		JPanel left = new JPanel(new GridLayout(2, 1));
		left.setOpaque(false);
		JLabel title = small(name + (task.getLocation().isDiseaseFree() && !task.isAllotment() ? "  DF" : ""), Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		left.add(title);
		Long est = d.herbEstimates.get(task.getKey());
		left.add(small(task.isAllotment()
			? (task.getPatchCount() > 1 ? task.getPatchCount() + " patches: " : "") + task.getPlantName()
			: "Herb patch: " + task.getPlantName() + (est != null ? " · ≈ " + gp(est) : ""),
			task.isAllotment() ? ColorScheme.LIGHT_GRAY_COLOR : HERB));
		card.add(left, BorderLayout.CENTER);

		String chip;
		Color color;
		switch (task.getCondition())
		{
			case GROWING:
				chip = task.getReadyAt() > 0 ? duration(task.getReadyAt() - now) : "Growing";
				color = WARN;
				break;
			case DISEASED:
				chip = "Diseased";
				color = BAD;
				break;
			case DEAD:
				chip = "Dead";
				color = BAD;
				break;
			case EMPTY:
				chip = "Empty";
				color = ColorScheme.LIGHT_GRAY_COLOR;
				break;
			case UNKNOWN:
				chip = "Not seen";
				color = MUTED;
				break;
			default:
				chip = "Ready";
				color = GOOD;
				break;
		}
		JLabel state = small(chip, color);
		state.setFont(FontManager.getRunescapeBoldFont());
		card.add(state, BorderLayout.EAST);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
		return card;
	}

	private JComponent herbNeedsBox(PanelData d)
	{
		JPanel box = column();
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(5, 6, 5, 6)));
		int shortCount = 0;
		for (HerbPlanner.Need n : d.herbNeeds)
		{
			if (n.isShort())
			{
				shortCount++;
			}
		}
		JPanel head = new JPanel(new BorderLayout());
		head.setOpaque(false);
		JLabel title = small("This run needs", Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		head.add(title, BorderLayout.WEST);
		head.add(small(shortCount > 0 ? shortCount + " short" : d.herbNeeds.isEmpty() ? "" : "All carried", shortCount > 0 ? BAD : MUTED),
			BorderLayout.EAST);
		head.setAlignmentX(LEFT_ALIGNMENT);
		head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		box.add(head);
		if (d.herbNeeds.isEmpty())
		{
			box.add(small("Nothing left to plant.", MUTED));
		}
		for (HerbPlanner.Need n : d.herbNeeds)
		{
			box.add(needRow(n.isShort() ? n.getCarried() + "/" + n.getNeeded() : NUM.format(n.getNeeded()), n.getName(),
				n.isShort() ? BAD : Color.WHITE));
		}
		if (d.compost.isBottomless())
		{
			box.add(needRow("1", "Bottomless compost bucket (filled)", d.compostCarried ? Color.WHITE : BAD));
		}
		box.add(needRow("1", "Spade, seed dibber" + (anyEmpty(d) ? ", rake" : ""), Color.WHITE));
		if (d.secateursExpected)
		{
			box.add(text(d.secateursCarried ? "Magic secateurs: with you (+10% herbs)" : "Magic secateurs aren't with you: about 10% fewer herbs",
				d.secateursCarried ? GOOD : BAD));
		}
		box.setAlignmentX(LEFT_ALIGNMENT);
		box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height + 4));
		return box;
	}

	private static boolean anyEmpty(PanelData d)
	{
		for (HerbTask t : d.herbTasks)
		{
			if (t.getCondition() == PatchCondition.EMPTY)
			{
				return true;
			}
		}
		return false;
	}

	private static JPanel needRow(String count, String name, Color countColor)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel n = number(count);
		n.setForeground(countColor);
		n.setPreferredSize(new Dimension(40, 15));
		row.add(n, BorderLayout.WEST);
		row.add(small(name, ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.CENTER);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
		return row;
	}

	private static JPanel gpRow(String label, long gp)
	{
		JPanel panel = row(label, gp(gp));
		JLabel value = (JLabel) panel.getComponent(1);
		value.setForeground(gp > 0 ? GOOD : gp < 0 ? BAD : Color.WHITE);
		return panel;
	}

	/** +1.2M, −45k, 950. */
	static String gp(long n)
	{
		String sign = n > 0 ? "+" : n < 0 ? "−" : "";
		return sign + compact(Math.abs(n));
	}

	/** 1h 05m, 12m 30s. */
	static String duration(long ms)
	{
		long s = Math.max(0, ms) / 1000;
		long h = s / 3600;
		long m = (s % 3600) / 60;
		if (h > 0)
		{
			return h + "h " + String.format("%02dm", m);
		}
		return m + "m " + String.format("%02ds", s % 60);
	}

	/** One line per crop: saplings and protection, colored by patch type. */
	private JComponent needsBox(List<RunNeeds.Line> needs, int slots)
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
		JLabel total = small(shortCount > 0 ? shortCount + " short" : RunNeeds.totalSaplings(needs) + " to plant · " + slots + "/28 slots",
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

	private static void addPickerRow(JPanel grid, String name, String value, String seed, String profit, String xp, Color seedColor,
		Color profitColor)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = grid.getComponentCount() / 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(1, 0, 0, 3);
		c.gridx = 0;
		c.weightx = 1;
		grid.add(small(name, ColorScheme.LIGHT_GRAY_COLOR), c);
		c.weightx = 0;
		c.gridx = 1;
		grid.add(fixed(value, Color.WHITE, 34), c);
		c.gridx = 2;
		grid.add(fixed(seed, seedColor, 36), c);
		c.gridx = 3;
		grid.add(fixed(profit, profitColor, 38), c);
		c.gridx = 4;
		c.insets = new Insets(1, 0, 0, 0);
		grid.add(fixed(xp, MUTED, 34), c);
		grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
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
