package com.treerunning;

import com.treerunning.core.HerbTask;
import com.treerunning.core.RouteItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines in the game world: the next tree patch brightly, other due tree patches and
 * herb patches more faintly, and the gardener of a patch you've planted but not paid.
 */
class PatchHighlightOverlay extends Overlay
{
	private static final Color NEXT = new Color(0x6C, 0xC2, 0x4A);
	private static final Color OTHER = new Color(0xE0, 0xC0, 0x40, 0x90);
	private static final Color HERB = new Color(0x8F, 0xD1, 0x6A, 0xC0);
	private static final Color PAY = new Color(0xFF, 0x98, 0x1F);

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
		PanelData d = plugin.getData();
		if (!d.loggedIn)
		{
			return null;
		}
		Map<String, GameObject> objects = plugin.getPatchObjects();
		if (plugin.getConfig().highlightPatches())
		{
			RouteItem next = d.nextItem();
			String nextKey = next != null && !next.isBank() ? next.getKey() : null;
			for (RouteItem item : d.items)
			{
				if (item.isBank() || !item.isDue() || d.done.contains(item.getKey()))
				{
					continue;
				}
				GameObject object = objects.get(item.getKey());
				if (object != null)
				{
					boolean isNext = item.getKey().equals(nextKey);
					outlines.drawOutline(object, isNext ? 3 : 2, isNext ? NEXT : OTHER, isNext ? 4 : 2);
				}
			}
			for (HerbTask task : d.herbTasks)
			{
				if (!task.isDue() || d.herbDone.contains(task.stepKey(HerbTask.PLANT)))
				{
					continue;
				}
				GameObject object = objects.get(task.getKey());
				if (object != null)
				{
					outlines.drawOutline(object, 2, HERB, 2);
				}
			}
		}
		if (d.highlightGardeners && (!d.unpaid.isEmpty() || !d.unpaidAllotments.isEmpty()))
		{
			Map<Integer, NPC> gardeners = plugin.getGardeners();
			java.util.Set<String> keys = new java.util.HashSet<>(d.unpaid);
			keys.addAll(d.unpaidAllotments);
			for (String key : keys)
			{
				TrackedPatch patch = TrackedPatch.BY_KEY.get(key);
				NPC npc = patch == null ? null : gardeners.get(patch.gardener);
				if (npc != null)
				{
					outlines.drawOutline(npc, 2, PAY, 3);
				}
			}
		}
		return null;
	}
}
