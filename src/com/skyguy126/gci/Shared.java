package com.skyguy126.gci;

import org.pmw.tinylog.Level;

public class Shared {
	public static final String LOG_FORMAT = "{date} - {level}: {message}";
	public static final String VERSION = "v0.2a";
	public static final String SOURCE_CODE_URL = "https://github.com/skyguy126/GCI";
	
	public static final boolean DEBUG_MODE = true;
	public static final boolean VSYNC = true;
	public static final boolean USE_SYSTEM_LOOK_AND_FEEL = false;
	
	public static final double PAN_SENSITIVITY_MULTIPLIER = 10.0;
	public static final double ZOOM_SENSITIVITY_MULTIPLIER = 10.0;
	public static final double ROTATE_SENSITIVITY_MULTIPLIER = 10.0;
	public static final double DECREASE_SENSITIVITY_MULTIPLIER = 2.5;
	
	public static final int SEGMENT_GENERATION_MULTIPLIER = 50;
	public static final int ARC_GENERATION_MULTIPLIER = 5;
	public static final int SEGMENT_SCALE_MULTIPLIER = 10;
	
	public static final long TIME_SCALE = 17;
	
	public static final Level LOG_LEVEL = (DEBUG_MODE) ? Level.DEBUG : Level.INFO;
}
