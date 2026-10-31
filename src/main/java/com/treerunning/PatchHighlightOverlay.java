package com.treerunning;

import com.treerunning.core.RouteItem;
import com.treerunning.data.PatchLocation;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines the run's patches in the game world: the next patch brightly, the other due
 * patches you haven't done yet more faintly.
 */
class PatchHighlightOverlay extends Overlay
{
	private static final Color NEXT = new Color(0x6C, 0xC2, 0x4A);
	private static final Color OTHER = new Color(0xE0, 0xC0, 0x40, 0x90);

	private final TreeRunningPlugin plugin;
	private final ModelOutlineRenderer outlines;

	@Inject
	PatchHighlightOverlay(TreeRunningPlugin plugin, ModelOutlineRenderer outlines)
	{
		this.plugin = plugin;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.getConfig().highlightPatches())
		{
			return null;
		}
		PanelData d = plugin.getData();
		if (!d.loggedIn || d.items.isEmpty())
		{
			return null;
		}
		RouteItem next = d.nextItem();
		PatchLocation nextPatch = next != null && !next.isBank() ? next.getTask().getLocation() : null;
		Map<PatchLocation, GameObject> objects = plugin.getPatchObjects();
		for (RouteItem item : d.items)
		{
			if (item.isBank() || !item.isDue() || d.done.contains(item.getKey()))
			{
				continue;
			}
			PatchLocation location = item.getTask().getLocation();
			GameObject object = objects.get(location);
			if (object != null)
			{
				boolean isNext = location == nextPatch;
				outlines.drawOutline(object, isNext ? 3 : 2, isNext ? NEXT : OTHER, isNext ? 4 : 2);
			}
		}
		return null;
	}
}
