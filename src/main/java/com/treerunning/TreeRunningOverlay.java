package com.treerunning;

import com.treerunning.core.Route;
import com.treerunning.core.HerbTask;
import com.treerunning.core.RouteItem;
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
 * In-game checklist: the current stop, how to get there, and its patches and bank stops in
 * the order you do them.
 */
class TreeRunningOverlay extends OverlayPanel
{
	static final Color DONE = new Color(0x6C, 0xC2, 0x4A);
	static final Color TODO = Color.WHITE;
	static final Color HINT = new Color(0xB0, 0xB0, 0xB0);
	static final Color BANK = new Color(0x7C, 0xC4, 0xFF);
	static final Color HERB = new Color(0x8F, 0xD1, 0x6A);
	static final Color PAY = new Color(0xFF, 0x98, 0x1F);

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
		if (!config.showOverlay() || !d.loggedIn)
		{
			return null;
		}
		// A herb run in progress takes over the overlay while no tree run is.
		if (d.herbActive && !d.runActive)
		{
			return renderHerbs(graphics, d);
		}
		if (d.tasks.isEmpty() || (config.overlayOnlyDuringRun() && !d.runActive))
		{
			return null;
		}

		panelComponent.setPreferredSize(new Dimension(230, 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Tree run " + d.doneCount() + "/" + d.dueCount())
			.color(DONE)
			.build());

		RouteItem next = d.nextItem();
		if (next == null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left(d.runActive ? "All done. Press Finish run." : "All due patches done.").leftColor(DONE).build());
			return super.render(graphics);
		}

		RouteStep step = next.getStep();
		panelComponent.getChildren().add(LineComponent.builder().left(step.getDisplayName()).leftColor(Color.YELLOW).build());
		String travel = d.travelNote(step);
		if (!travel.trim().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().left(travel).leftColor(HINT).build());
		}

		for (RouteItem item : d.items)
		{
			if (item.getStep() != step || !item.isDue())
			{
				continue;
			}
			boolean done = d.done.contains(item.getKey());
			String label = item.isBank() ? item.getBank().getLabel() : TreeRunningPanel.patchLabel(item.getTask());
			panelComponent.getChildren().add(LineComponent.builder()
				.left((done ? "[x] " : "[ ] ") + label)
				.leftColor(done ? DONE : item.isBank() ? BANK : TODO)
				.build());
		}

		for (HerbTask herb : d.onTheWay.getOrDefault(step, java.util.Collections.emptyList()))
		{
			boolean planted = herb.isDone(d.herbDone, HerbTask.PLANT);
			panelComponent.getChildren().add(LineComponent.builder()
				.left((planted ? "[x] " : "[ ] ") + "Herb: " + herb.getPlantName())
				.leftColor(planted ? DONE : HERB)
				.build());
		}

		addUnpaid(d);

		RouteStep after = Route.stepAfter(d.items, next, d.done);
		if (after != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Then").right(after.getDisplayName()).leftColor(HINT).build());
		}

		return super.render(graphics);
	}

	private void addUnpaid(PanelData d)
	{
		for (RunTask task : d.tasks)
		{
			if (d.unpaid.contains(task.getKey()))
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Not paid: " + TreeRunningPanel.patchLabel(task) + ", " + task.getStep().getDisplayName())
					.leftColor(PAY)
					.build());
			}
		}
	}

	private Dimension renderHerbs(Graphics2D graphics, PanelData d)
	{
		panelComponent.setPreferredSize(new Dimension(230, 0));
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Herb run " + d.herbPlantedCount() + "/" + d.herbDueCount())
			.color(DONE)
			.build());
		HerbTask next = null;
		for (HerbTask task : d.herbTasks)
		{
			if (task.isDue() && !task.isDone(d.herbDone, HerbTask.PLANT))
			{
				next = task;
				break;
			}
		}
		if (next == null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("All done. Press Finish run.").leftColor(DONE).build());
			return super.render(graphics);
		}
		panelComponent.getChildren().add(LineComponent.builder().left(next.getLocation().getDisplayName()).leftColor(Color.YELLOW).build());
		String herbTravel = d.travelNote(next.getLocation());
		if (!herbTravel.trim().isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().left(herbTravel).leftColor(HINT).build());
		}
		for (HerbTask task : d.herbTasks)
		{
			if (task.getLocation() != next.getLocation() || !task.isDue())
			{
				continue;
			}
			for (HerbTask.Step step : task.getSteps())
			{
				boolean done = task.isDone(d.herbDone, step.getId());
				panelComponent.getChildren().add(LineComponent.builder()
					.left((done ? "[x] " : "[ ] ") + (task.isAllotment() ? task.getLabel() + ": " : "") + step.getLabel())
					.leftColor(done ? DONE : TODO)
					.build());
			}
		}
		if (d.secateursExpected && !d.secateursCarried)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Magic secateurs aren't with you").leftColor(PAY).build());
		}
		return super.render(graphics);
	}
}
