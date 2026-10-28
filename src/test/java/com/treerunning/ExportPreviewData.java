package com.treerunning;

import com.treerunning.core.InventoryPlanner;
import com.treerunning.core.PlanSettings;
import com.treerunning.core.RunPlanner;
import com.treerunning.core.RunTask;
import com.treerunning.core.XpProjection;
import com.treerunning.data.CropType;
import com.treerunning.data.PatchLocation;
import com.treerunning.data.PatchType;
import com.treerunning.data.RouteStep;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev tool: writes the crop/route tables plus reference results to JSON, so the HTML
 * preview uses exactly the plugin's data and can be checked against the Java planner.
 */
public class ExportPreviewData
{
	public static void main(String[] args) throws Exception
	{
		Map<String, Object> out = new LinkedHashMap<>();

		List<Map<String, Object>> crops = new ArrayList<>();
		for (CropType c : CropType.values())
		{
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("key", c.name());
			m.put("name", c.getDisplayName());
			m.put("type", c.getPatchType().name());
			m.put("level", c.getLevel());
			m.put("xp", c.getXp());
			m.put("growth", c.getGrowthMinutes());
			m.put("planted", c.getPlantedItem().getName());
			m.put("payment", c.getPayment() == null ? null : c.getPayment().getName());
			m.put("paymentQty", c.getPaymentQuantity());
			m.put("produce", c.getProduce() == null ? null : c.getProduce().getName());
			m.put("seedOnly", c.getSapling() == null);
			crops.add(m);
		}
		out.put("crops", crops);

		List<Map<String, Object>> steps = new ArrayList<>();
		for (RouteStep s : RouteStep.values())
		{
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("key", s.name());
			m.put("name", s.getDisplayName());
			m.put("travel", s.getTravel());
			m.put("bank", s.getBank());
			m.put("carry", s.getCarryItem());
			steps.add(m);
		}
		out.put("steps", steps);
		out.put("fallbackBank", RouteStep.FALLBACK_BANK);

		List<Map<String, Object>> locations = new ArrayList<>();
		for (PatchLocation l : PatchLocation.values())
		{
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("key", l.name());
			m.put("type", l.getPatchType().name());
			m.put("step", l.getStep().name());
			m.put("count", l.getPatchCount());
			m.put("patchName", l.getPatchName());
			m.put("req", l.getRequirement());
			locations.add(m);
		}
		out.put("locations", locations);

		// Reference results for cross-checking the preview's JavaScript.
		List<Map<String, Object>> reference = new ArrayList<>();
		CropType[][] combos = {
			{CropType.YEW, CropType.PAPAYA, CropType.IRONWOOD},
			{CropType.YEW, CropType.PALM, CropType.IRONWOOD},
			{CropType.MAGIC, CropType.DRAGONFRUIT, CropType.MAHOGANY},
			{CropType.MAPLE, CropType.PAPAYA, CropType.TEAK},
		};
		for (CropType[] combo : combos)
		{
			Map<PatchType, CropType> map = new EnumMap<>(PatchType.class);
			map.put(PatchType.TREE, combo[0]);
			map.put(PatchType.FRUIT_TREE, combo[1]);
			map.put(PatchType.HARDWOOD, combo[2]);
			map.put(PatchType.CALQUAT, CropType.CALQUAT);
			map.put(PatchType.CELASTRUS, CropType.CELASTRUS);
			map.put(PatchType.REDWOOD, CropType.REDWOOD);
			map.put(PatchType.HESPORI, CropType.HESPORI);
			PlanSettings s = new PlanSettings(map, EnumSet.allOf(PatchLocation.class), 99, 1, 3, true);
			XpProjection p = XpProjection.toGoal(4_842_295, s);
			List<RunTask> tasks = RunPlanner.buildTasks(s, 89, Collections.emptyMap(), 0);
			InventoryPlanner.Plan plan = InventoryPlanner.plan(tasks, s);

			Map<String, Object> r = new LinkedHashMap<>();
			r.put("tree", combo[0].name());
			r.put("fruit", combo[1].name());
			r.put("hardwood", combo[2].name());
			r.put("runs", p.getRuns());
			Map<String, Integer> plantings = new LinkedHashMap<>();
			p.getPlantings().forEach((k, v) -> plantings.put(k.name(), v));
			r.put("plantings", plantings);
			r.put("capacity", plan.getSaplingCapacity());
			List<String> legBanks = new ArrayList<>();
			for (InventoryPlanner.Leg leg : plan.getLegs())
			{
				legBanks.add(leg.getBankAt() + " | " + leg.getSlots());
			}
			r.put("legs", legBanks);
			reference.add(r);
		}
		out.put("reference", reference);

		try (Writer w = new FileWriter(args.length > 0 ? args[0] : "build/preview-data.json"))
		{
			new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(out, w);
		}
	}
}
