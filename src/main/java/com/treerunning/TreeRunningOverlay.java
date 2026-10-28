package com.treerunning;

import com.treerunning.core.Route;
import com.treerunning.core.RouteItem;
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

		RouteItem next = d.nextItem();
		if (next == null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("All due patches planted.").leftColor(DONE).build());
			return super.render(graphics);
		}

		RouteStep step = next.getStep();
		panelComponent.getChildren().add(LineComponent.builder().left(step.getDisplayName()).leftColor(Color.YELLOW).build());
		panelComponent.getChildren().add(LineComponent.builder().left(step.getTravel()).leftColor(HINT).build());

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

		RouteStep after = Route.stepAfter(d.items, next, d.done);
		if (after != null)
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Then").right(after.getDisplayName()).leftColor(HINT).build());
		}

		return super.render(graphics);
	}
}
