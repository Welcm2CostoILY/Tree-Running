package com.treerunning;

import com.treerunning.core.BankStop;
import com.treerunning.core.Route;
import com.treerunning.core.RunNeeds;
import com.treerunning.core.XpTable;
import com.treerunning.data.RouteStep;
import java.util.Collections;
import com.treerunning.core.PlanSettings;
import com.treerunning.core.RunPlanner;
import com.treerunning.core.RunTask;
import com.treerunning.core.SupplyCalculator;
import com.treerunning.core.XpProjection;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * Dev tool: renders each tab of the side panel to a PNG with sample data, without
 * starting the client. Run with java.awt.headless=true.
 */
public class PanelSnapshot
{
	public static void main(String[] args) throws Exception
	{
		File outDir = new File(args.length > 0 ? args[0] : "build/snapshots");
		outDir.mkdirs();

		Map<PatchType, CropType> crops = new EnumMap<>(PatchType.class);
		crops.put(PatchType.TREE, CropType.YEW);
		crops.put(PatchType.FRUIT_TREE, CropType.PAPAYA);
		crops.put(PatchType.HARDWOOD, CropType.IRONWOOD);
		crops.put(PatchType.CALQUAT, CropType.CALQUAT);
		crops.put(PatchType.CELASTRUS, CropType.CELASTRUS);
		crops.put(PatchType.REDWOOD, CropType.REDWOOD);
		crops.put(PatchType.HESPORI, CropType.HESPORI);
		PlanSettings settings = new PlanSettings(crops, EnumSet.allOf(PatchLocation.class), XpTable.xpForLevel(99), 1);

		int xp = 4_842_295;
		XpProjection planned = XpProjection.toGoal(xp, settings);
		List<RunTask> tasks = RunPlanner.buildTasks(settings, 89, new HashMap<>(), System.currentTimeMillis());
		Set<String> done = new HashSet<>();
		for (int i = 0; i < 4; i++)
		{
			done.add(tasks.get(i).getKey());
		}
		Map<Integer, Integer> have = new HashMap<>();
		have.put(CropType.YEW.getSapling().getId(), 60);
		have.put(CropType.PAPAYA.getSeed().getId(), 400);
		have.put(CropType.YEW.getPayment().getId(), 1200);
		Map<Integer, Long> price = new HashMap<>();
		price.put(CropType.YEW.getSeed().getId(), 12_655L);
		price.put(CropType.PAPAYA.getSeed().getId(), 1_046L);
		price.put(CropType.IRONWOOD.getSeed().getId(), 31_248L);
		price.put(CropType.YEW.getPayment().getId(), 947L);
		price.put(CropType.PAPAYA.getPayment().getId(), 171L);
		List<BankStop> banks = Collections.singletonList(new BankStop("a", RouteStep.CATHERBY, true, "Catherby bank"));

		PanelData data = new PanelData();
		data.loggedIn = true;
		data.currentXp = xp + 52_000;
		data.level = 89;
		data.goalXp = settings.getGoalXp();
		data.plannedXpPerRun = planned.getAverageXpPerRun();
		data.plannedRuns = planned.getRuns();
		data.runsLeft = planned.getRuns();
		data.tasks = tasks;
		data.items = Route.build(tasks, banks);
		data.done = done;
		data.runActive = true;
		data.runXpSoFar = 52_000;
		data.lastMatch = "Yew – Gnome Stronghold";
		data.needs = RunNeeds.compute(tasks);
		data.supplies = SupplyCalculator.compute(XpProjection.forRuns(xp, planned.getRuns(), settings).getPlantings(),
			id -> have.getOrDefault(id, 0), id -> price.getOrDefault(id, 0L));
		data.bankSeenAt = System.currentTimeMillis() - 2 * 3_600_000L;

		SwingUtilities.invokeAndWait(() ->
		{
			TreeRunningPanel panel = new TreeRunningPanel(null);
			// Every setting has a default, so an empty implementation is the default config.
			panel.loadSettings(new TreeRunningConfig()
			{
			}, banks, location -> true);
			panel.update(data);
			JTabbedPane tabs = (JTabbedPane) panel.getComponent(0);
			for (int i = 0; i < tabs.getTabCount(); i++)
			{
				tabs.setSelectedIndex(i);
				panel.setSize(new Dimension(242, 2600));
				layoutAll(panel);
				BufferedImage image = new BufferedImage(242, 2600, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = image.createGraphics();
				panel.printAll(g);
				g.dispose();
				try
				{
					ImageIO.write(image, "png", new File(outDir, "tab-" + i + ".png"));
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		});
		System.out.println("Wrote snapshots to " + outDir.getAbsolutePath());
	}

	private static void layoutAll(Component c)
	{
		c.doLayout();
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				layoutAll(child);
			}
		}
		c.validate();
	}
}
