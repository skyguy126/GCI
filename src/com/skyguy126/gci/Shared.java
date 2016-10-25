package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Font;

import org.pmw.tinylog.Level;

public class Shared {
	public static final String LOG_FORMAT = "{date} - {level}: {message}";
	public static final String VERSION = "v0.5a";
	public static final String SOURCE_CODE_URL = "https://github.com/skyguy126/GCI";

	public static final boolean DEBUG_MODE = false;
	public static final boolean VSYNC = true;
	public static final boolean USE_SYSTEM_LOOK_AND_FEEL = false;
	public static final boolean ENABLE_ANTIALIAS = true;

	public static final double PAN_SENSITIVITY_MULTIPLIER = 10.0;
	public static final double ZOOM_SENSITIVITY_MULTIPLIER = 10.0;
	public static final double ROTATE_SENSITIVITY_MULTIPLIER = 10.0;
	public static final double DECREASE_SENSITIVITY_MULTIPLIER = 2.5;

	public static volatile float SEGMENT_SCALE_MULTIPLIER = 10f;
	public static volatile float SEGMENT_GENERATION_MULTIPLIER = 30f;
	public static final int ARC_GENERATION_MULTIPLIER = 5;
	
	public static final long TIME_SCALE = 20;

	public static final Level LOG_LEVEL = (DEBUG_MODE) ? Level.DEBUG : Level.INFO;
	public static final Font UI_FONT = new Font("Arial", Font.PLAIN, 20);
	public static final Color UI_COLOR = new Color(69, 90, 100);
}
