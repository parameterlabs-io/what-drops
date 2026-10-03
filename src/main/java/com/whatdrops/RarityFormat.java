package com.whatdrops;

public enum RarityFormat
{
	FRACTION("Fraction (1/512)"),
	PERCENT("Percent (0.195%)");

	private final String label;

	RarityFormat(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
