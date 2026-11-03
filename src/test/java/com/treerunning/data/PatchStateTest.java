package com.treerunning.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PatchStateTest
{
	@Test
	public void treeStatesComeFromRuneLitesTable()
	{
		// Oak: 8-11 growing, 12 check-health, 13 grown (chop down), 14 stump (RuneLite PatchImplementation).
		PatchReading growing = PatchKind.TREE.read(8);
		assertEquals(PatchCondition.GROWING, growing.getCondition());
		assertEquals("Oak", growing.getCrop());
		assertEquals(0, growing.getStage());
		assertEquals(PatchCondition.CHECK_HEALTH, PatchKind.TREE.read(12).getCondition());
		assertEquals(PatchCondition.GROWN, PatchKind.TREE.read(13).getCondition());
		assertEquals(PatchCondition.STUMP, PatchKind.TREE.read(14).getCondition());
		assertEquals(PatchCondition.EMPTY, PatchKind.TREE.read(0).getCondition());
		assertTrue(PatchKind.TREE.read(0).isWeeds());
	}

	@Test
	public void everyHerbGrowsInFourStagesOfTwentyMinutes()
	{
		boolean sawRanarr = false;
		for (int v = 0; v < 256; v++)
		{
			PatchReading r = PatchKind.HERB.read(v);
			if (r.getCondition() == PatchCondition.GROWING && "Ranarr".equals(r.getCrop()) && r.getStage() == 0)
			{
				sawRanarr = true;
				assertEquals(20, r.getMinutesPerStage());
				assertEquals(80, r.maxMinutesLeft());
				assertTrue(HerbType.byCropName(r.getCrop()) == HerbType.RANARR);
			}
		}
		assertTrue(sawRanarr);
	}

	@Test
	public void allotmentCropsAreNamedLikeOurTypes()
	{
		for (int v = 0; v < 256; v++)
		{
			PatchReading r = PatchKind.ALLOTMENT.read(v);
			if (r.getCondition() == PatchCondition.GROWING && !r.isWeeds())
			{
				assertTrue("unknown allotment crop " + r.getCrop(), AllotmentType.byCropName(r.getCrop()) != null);
			}
		}
	}

	@Test
	public void regionExceptionsMatchRuneLite()
	{
		// Catherby: the herb farm's varbits reach into the fruit tree's region, and vice versa.
		assertTrue(HerbLocation.CATHERBY.isAt(2813, 3463, 0));
		assertTrue(HerbLocation.CATHERBY.isAt(2830, 3445, 0));
		assertFalse(PatchLocation.FRUIT_CATHERBY.isAt(2830, 3445, 0));
		assertTrue(PatchLocation.FRUIT_CATHERBY.isAt(2860, 3433, 0));
		assertFalse(HerbLocation.CATHERBY.isAt(2860, 3433, 0));
		// Fossil Island hardwood only on the ground floor.
		assertTrue(PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST.isAt(3715, 3835, 0));
		assertFalse(PatchLocation.HARDWOOD_FOSSIL_ISLAND_EAST.isAt(3715, 3835, 1));
	}
}
