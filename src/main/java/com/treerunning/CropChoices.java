package com.treerunning;

import com.treerunning.data.CropType;

/**
 * The dropdown choices shown in the plugin settings, one enum per patch type.
 */
public final class CropChoices
{
	private CropChoices()
	{
	}

	public enum Tree
	{
		OAK(CropType.OAK),
		WILLOW(CropType.WILLOW),
		MAPLE(CropType.MAPLE),
		YEW(CropType.YEW),
		MAGIC(CropType.MAGIC);

		private final CropType crop;

		Tree(CropType crop)
		{
			this.crop = crop;
		}

		public CropType crop()
		{
			return crop;
		}

		@Override
		public String toString()
		{
			return crop.getDisplayName();
		}
	}

	public enum Fruit
	{
		APPLE(CropType.APPLE),
		BANANA(CropType.BANANA),
		ORANGE(CropType.ORANGE),
		CURRY(CropType.CURRY),
		PINEAPPLE(CropType.PINEAPPLE),
		PAPAYA(CropType.PAPAYA),
		PALM(CropType.PALM),
		DRAGONFRUIT(CropType.DRAGONFRUIT);

		private final CropType crop;

		Fruit(CropType crop)
		{
			this.crop = crop;
		}

		public CropType crop()
		{
			return crop;
		}

		@Override
		public String toString()
		{
			return crop.getDisplayName();
		}
	}

	public enum Hardwood
	{
		TEAK(CropType.TEAK),
		MAHOGANY(CropType.MAHOGANY),
		CAMPHOR(CropType.CAMPHOR),
		IRONWOOD(CropType.IRONWOOD),
		ROSEWOOD(CropType.ROSEWOOD);

		private final CropType crop;

		Hardwood(CropType crop)
		{
			this.crop = crop;
		}

		public CropType crop()
		{
			return crop;
		}

		@Override
		public String toString()
		{
			return crop.getDisplayName();
		}
	}
}
