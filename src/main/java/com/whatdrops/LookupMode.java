package com.whatdrops;

public enum LookupMode
{
	ALWAYS("Always"),
	SHIFT("Hold Shift"),
	NEVER("Never");

	private final String label;

	LookupMode(String label)
	{
		this.label = label;
	}

	/** Whether the right-click entry shows. A null mode (an unreadable saved setting) counts as the default, ALWAYS. */
	public static boolean shows(LookupMode mode, boolean shiftHeld)
	{
		return mode == null || mode == ALWAYS || (mode == SHIFT && shiftHeld);
	}

	@Override
	public String toString()
	{
		return label;
	}
}
