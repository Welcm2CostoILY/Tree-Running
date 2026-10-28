package com.treerunning;

import com.treerunning.core.InventoryPlanner;
import com.treerunning.core.RunTask;
import com.treerunning.data.RouteStep;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * In-game checklist: the current stop, how to get there, its patches, and any bank stop.
 */
class TreeRunningOverlay extends OverlayPanel
{
	private static final Color DONE = new Color(0x6C, 0xC2, 0x4A);
	private static final Color TODO = Color.WHITE;
	private static final Color HINT = new Color(0xB0, 0xB0, 0xB0);
	private static final Color BANK = new Color(0xFF, 0x98, 0x1F);

	private final TreeRunningPlugin plugin;

	@Inject
	TreeRunningOverlay(TreeRunningPlugin plugin)
	{
		super(plugin);
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		TreeRunningConfig config = plugin.getConfig();
		PanelData d = plugin.getData();
		if (!config.showOverlay() || !d.loggedIn || d.tasks.isEmpty())
		{
			return null;
		}
		if (config.overlayOnlyDuringRun() && !d.runActive)
		{
			return null;
		}

		panelComponent.setPreferredSize(new Dimension(230, 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Tree run " + d.doneCount() + "/" + d.dueCount())
			.color(DONE)
			.build());

		RunTask next = d.nextTask();
		if (next == null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("All due patches planted.").leftColor(DONE).build());
			return super.render(graphics);
		}

		RouteStep step = next.getStep();

		String bank = bankBefore(d.inventoryPlan, step, d);
		if (bank != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Bank first: " + bank).leftColor(BANK).build());
		}

		panelComponent.getChildren().add(LineComponent.builder().left(step.getDisplayName()).leftColor(Color.YELLOW).build());
		panelComponent.getChildren().add(LineComponent.builder().left(step.getTravel()).leftColor(HINT).build());

		for (RunTask task : d.tasks)
		{
			if (task.getStep() != step || !task.isDue())
			{
				continue;
			}
			boolean done = d.done.contains(task.getKey());
			panelComponent.getChildren().add(LineComponent.builder()
				.left((done ? "[x] " : "[ ] ") + task.getCrop().getDisplayName() + " " + task.getLocation().getPatchType().getDisplayName().toLowerCase()
					+ (task.getLocation().getPatchName() != null ? " (" + task.getLocation().getPatchName().toLowerCase() + ")" : ""))
				.leftColor(done ? DONE : TODO)
				.build());
		}

		RouteStep after = stepAfter(d, step);
		if (after != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Then").right(after.getDisplayName()).leftColor(HINT).build());
		}

		return super.render(graphics);
	}

	/** If this stop starts a new leg and nothing in it is planted yet, where to bank first. */
	private static String bankBefore(InventoryPlanner.Plan plan, RouteStep step, PanelData d)
	{
		if (plan == null)
		{
			return null;
		}
		for (int i = 1; i < plan.getLegs().size(); i++)
		{
			InventoryPlanner.Leg leg = plan.getLegs().get(i);
			if (!leg.getSteps().isEmpty() && leg.getSteps().get(0) == step)
			{
				for (RunTask task : d.tasks)
				{
					if (task.getStep() == step && d.done.contains(task.getKey()))
					{
						return null;
					}
				}
				return leg.getBankAt();
			}
		}
		return null;
	}

	private static RouteStep stepAfter(PanelData d, RouteStep step)
	{
		boolean passed = false;
		for (RunTask task : d.tasks)
		{
			if (!task.isDue())
			{
				continue;
			}
			if (task.getStep() == step)
			{
				passed = true;
			}
			else if (passed && !d.done.contains(task.getKey()))
			{
				return task.getStep();
			}
		}
		return null;
	}
}
