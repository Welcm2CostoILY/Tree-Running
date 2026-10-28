package com.treerunning;

import com.treerunning.core.InventoryPlanner;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.data.RouteStep;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel with four tabs: Progress, Run, Supplies and Inventory.
 */
class TreeRunningPanel extends PluginPanel
{
	private static final NumberFormat NUM = NumberFormat.getIntegerInstance();
	private static final Color GOOD = new Color(0x6C, 0xC2, 0x4A);
	private static final Color BAD = new Color(0xE0, 0x6C, 0x5A);
	private static final Color MUTED = ColorScheme.MEDIUM_GRAY_COLOR;

	private final TreeRunningPlugin plugin;
	private final JPanel progressTab = column();
	private final JPanel runTab = column();
	private final JPanel suppliesTab = column();
	private final JPanel inventoryTab = column();

	TreeRunningPanel(TreeRunningPlugin plugin)
	{
		super(false);
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("XP", scroll(progressTab));
		tabs.addTab("Run", scroll(runTab));
		tabs.addTab("Supplies", scroll(suppliesTab));
		tabs.addTab("Inventory", scroll(inventoryTab));
		add(tabs, BorderLayout.CENTER);

		update(PanelData.EMPTY);
	}

	void update(PanelData d)
	{
		buildProgress(d);
		buildRun(d);
		buildSupplies(d);
		buildInventory(d);
		revalidate();
		repaint();
	}

	// ------------------------------------------------------------------ Progress

	private void buildProgress(PanelData d)
	{
		progressTab.removeAll();
		if (!d.loggedIn)
		{
			progressTab.add(text("Log in to see your Farming progress.", MUTED));
			return;
		}

		progressTab.add(heading("Level " + d.level + " → " + d.goalLevel));
		JProgressBar bar = new JProgressBar(0, 1000);
		double span = Math.max(1, d.goalXp - xpStart(d));
		bar.setValue((int) (1000 * Math.min(1, (d.currentXp - xpStart(d)) / span)));
		bar.setForeground(GOOD);
		bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bar.setStringPainted(true);
		bar.setString(NUM.format(d.xpRemaining()) + " XP to go");
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		bar.setAlignmentX(LEFT_ALIGNMENT);
		progressTab.add(bar);
		progressTab.add(Box.createVerticalStrut(8));

		if (d.xpRemaining() == 0)
		{
			progressTab.add(text("Goal reached. Enjoy the cape!", GOOD));
			return;
		}

		if (d.runsLeft > 0)
		{
			int days = (int) Math.ceil(d.runsLeft / (double) d.runsPerDay);
			progressTab.add(big(d.runsLeft + " runs left"));
			progressTab.add(row("Days left", String.valueOf(days)));
			progressTab.add(row("Goal date", LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
			progressTab.add(text(d.runsLeftFromActual
				? "Based on your last " + d.actualRunsCounted + " run" + (d.actualRunsCounted == 1 ? "" : "s") + "."
				: "Planned estimate. Finish a run to switch to your real XP.", MUTED));
		}
		else
		{
			progressTab.add(text("Your current plan can't reach this goal. Check your crops and patches.", BAD));
		}

		progressTab.add(Box.createVerticalStrut(8));
		progressTab.add(heading("XP per run"));
		progressTab.add(row("Your average", d.actualXpPerRun > 0 ? NUM.format(Math.round(d.actualXpPerRun)) : "–"));
		progressTab.add(row("Planned", NUM.format(Math.round(d.plannedXpPerRun))));
		progressTab.add(row("Planned runs", d.plannedRuns > 0 ? String.valueOf(d.plannedRuns) : "–"));
		if (d.runActive)
		{
			progressTab.add(row("This run so far", NUM.format(d.runXpSoFar)));
		}

		progressTab.add(Box.createVerticalStrut(8));
		JButton clear = button("Clear run history");
		clear.addActionListener(e -> plugin.clearHistory());
		progressTab.add(clear);
	}

	/** Start of the progress bar: the XP of the current level. */
	private static int xpStart(PanelData d)
	{
		return com.treerunning.core.XpTable.xpForLevel(Math.min(d.level, d.goalLevel));
	}

	// ------------------------------------------------------------------ Run checklist

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
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
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
		runTab.add(text("Planting ticks off the exact patch you planted, in any order. Click to tick or untick by hand.", MUTED));
		if (d.lastMatch != null)
		{
			runTab.add(text("Last planted: " + d.lastMatch, GOOD));
		}

		RouteStep currentStep = null;
		for (RunTask task : d.tasks)
		{
			if (task.getStep() != currentStep)
			{
				currentStep = task.getStep();
				runTab.add(Box.createVerticalStrut(6));
				JLabel location = heading(currentStep.getDisplayName());
				location.setForeground(ColorScheme.BRAND_ORANGE);
				runTab.add(location);
				runTab.add(text(currentStep.getTravel(), MUTED));
			}

			String label = task.getCrop().getDisplayName() + " – " + task.getLocation().getPatchType().getDisplayName()
				+ (task.getLocation().getPatchName() != null ? " (" + task.getLocation().getPatchName() + ")" : "")
				+ (task.isDue() ? "" : " (" + task.getNote() + ")");
			JCheckBox box = new JCheckBox("<html><body style='width:150px'>" + escape(label) + "</body></html>");
			box.setForeground(task.isDue() ? Color.WHITE : MUTED);
			box.setAlignmentX(LEFT_ALIGNMENT);
			box.setSelected(d.done.contains(task.getKey()));
			box.setBackground(ColorScheme.DARK_GRAY_COLOR);
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setEnabled(task.isDue());
			String key = task.getKey();
			box.addActionListener(e -> plugin.toggleTask(key));
			runTab.add(box);
		}

		if (d.tasks.isEmpty())
		{
			runTab.add(text("No patches enabled. Turn some on in the plugin settings.", MUTED));
		}
	}

	// ------------------------------------------------------------------ Supplies

	private void buildSupplies(PanelData d)
	{
		suppliesTab.removeAll();
		if (!d.loggedIn)
		{
			suppliesTab.add(text("Log in to see your supplies.", MUTED));
			return;
		}

		suppliesTab.add(heading(d.runsLeft > 0 ? "Needed for " + d.runsLeft + " runs" : "Supplies"));
		suppliesTab.add(text("Crop rows count seeds, seedlings and saplings together.", MUTED));
		suppliesTab.add(text(d.bankSeenAt == 0
			? "Open your bank once so the plugin can count what you have."
			: "Bank counted when you last opened it, plus your inventory.", d.bankSeenAt == 0 ? BAD : MUTED));

		JPanel header = gridRow("Item", "Have", "Need", ColorScheme.LIGHT_GRAY_COLOR);
		suppliesTab.add(header);
		boolean paymentsStarted = false;
		for (SupplyCalculator.SupplyLine line : d.supplies)
		{
			if (line.isPayment() && !paymentsStarted)
			{
				paymentsStarted = true;
				suppliesTab.add(Box.createVerticalStrut(4));
				suppliesTab.add(heading("Payments"));
			}
			Color color = line.getShort() > 0 ? BAD : GOOD;
			suppliesTab.add(gridRow(line.getName(), NUM.format(line.getHave()), NUM.format(line.getNeed()), color));
		}

		int shortItems = 0;
		for (SupplyCalculator.SupplyLine line : d.supplies)
		{
			if (line.getShort() > 0)
			{
				shortItems++;
			}
		}
		suppliesTab.add(Box.createVerticalStrut(6));
		suppliesTab.add(text(shortItems == 0 ? "You have everything you need." : shortItems + " items short (in red).",
			shortItems == 0 ? GOOD : BAD));
	}

	// ------------------------------------------------------------------ Inventory plan

	private void buildInventory(PanelData d)
	{
		inventoryTab.removeAll();
		InventoryPlanner.Plan plan = d.inventoryPlan;
		if (!d.loggedIn || plan == null)
		{
			inventoryTab.add(text("Log in to see your inventory plan.", MUTED));
			return;
		}

		inventoryTab.add(heading("Carry the whole run"));
		inventoryTab.add(text(plan.getReservedSlots() + " slots for your own items (spade, coins, house teleport…)", MUTED));
		for (String item : plan.getCarryItems())
		{
			inventoryTab.add(row(item, "1"));
		}
		for (Map.Entry<String, Integer> payment : plan.getPayments().entrySet())
		{
			inventoryTab.add(row(payment.getKey() + " (noted)", NUM.format(payment.getValue())));
		}
		if (!plan.getNotedProduce().isEmpty())
		{
			inventoryTab.add(text("Keep " + plan.getPickingSlots() + " slots free to pick each tree, then use the produce on the "
				+ "tool leprechaun. It notes it but doesn't bank it, so each noted stack stays in your inventory: "
				+ String.join(", ", plan.getNotedProduce()) + ".", MUTED));
		}

		inventoryTab.add(Box.createVerticalStrut(6));
		inventoryTab.add(heading(plan.getSaplingCapacity() + " slots for saplings · "
			+ plan.getBankStops() + " bank stop" + (plan.getBankStops() == 1 ? "" : "s")));

		int legNumber = 1;
		for (InventoryPlanner.Leg leg : plan.getLegs())
		{
			inventoryTab.add(Box.createVerticalStrut(6));
			inventoryTab.add(text((legNumber == 1 ? "Start: " : "Bank: ") + leg.getBankAt(), ColorScheme.BRAND_ORANGE));
			for (Map.Entry<String, Integer> item : leg.getWithdraw().entrySet())
			{
				inventoryTab.add(row("Withdraw " + item.getKey(), String.valueOf(item.getValue())));
			}
			StringBuilder stops = new StringBuilder();
			for (RouteStep step : leg.getSteps())
			{
				if (stops.length() > 0)
				{
					stops.append(" → ");
				}
				stops.append(step.getDisplayName());
			}
			inventoryTab.add(text(stops.toString(), MUTED));
			legNumber++;
		}

		if (plan.getLegs().isEmpty())
		{
			inventoryTab.add(text("Nothing is due right now.", MUTED));
		}
	}

	// ------------------------------------------------------------------ small widgets

	private static JPanel column()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		return panel;
	}

	private static JComponent scroll(JPanel content)
	{
		JPanel wrapper = new WidthTrackingPanel();
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(content, BorderLayout.NORTH);
		javax.swing.JScrollPane pane = new javax.swing.JScrollPane(wrapper);
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.getVerticalScrollBar().setUnitIncrement(16);
		pane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return pane;
	}

	private static JLabel heading(String s)
	{
		JLabel label = new JLabel("<html><body style='width:175px'>" + escape(s) + "</body></html>");
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
		JLabel label = new JLabel("<html><body style='width:175px'>" + escape(s) + "</body></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel row(String left, String right)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel l = new JLabel(left);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JLabel r = new JLabel(right);
		r.setFont(FontManager.getRunescapeSmallFont());
		r.setForeground(Color.WHITE);
		panel.add(l, BorderLayout.WEST);
		panel.add(r, BorderLayout.EAST);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static JPanel gridRow(String name, String have, String need, Color color)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel l = new JLabel(name);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel numbers = new JPanel(new GridLayout(1, 2, 6, 0));
		numbers.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel h = new JLabel(have, JLabel.RIGHT);
		h.setFont(FontManager.getRunescapeSmallFont());
		h.setForeground(color);
		JLabel n = new JLabel(need, JLabel.RIGHT);
		n.setFont(FontManager.getRunescapeSmallFont());
		n.setForeground(Color.WHITE);
		numbers.add(h);
		numbers.add(n);
		numbers.setPreferredSize(new Dimension(84, 16));
		panel.add(l, BorderLayout.CENTER);
		panel.add(numbers, BorderLayout.EAST);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static JButton button(String s)
	{
		JButton b = new JButton(s);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setFocusPainted(false);
		b.setAlignmentX(LEFT_ALIGNMENT);
		return b;
	}

	/** Scrollable content that always matches the panel width, so nothing runs off the right edge. */
	private static final class WidthTrackingPanel extends JPanel implements javax.swing.Scrollable
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

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
