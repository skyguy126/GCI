package com.skyguy126.gci;

import org.pmw.tinylog.Level;

public class Shared {
	public static final String LOG_FORMAT = "{date} - {level}: {message}";
	public static final String VERSION = "v1.0";
	
	public static final boolean DEBUG_MODE = true;
	public static final boolean VSYNC = true;
	
	public static final Level LOG_LEVEL = (DEBUG_MODE) ? Level.DEBUG : Level.INFO;
	
	// TODO allow these values to be changed
	public static double PAN_SENSITIVITY_MULTIPLIER = 10.0;
	public static double ZOOM_SENSITIVITY_MULTIPLIER = 10.0;
	public static double ROTATE_SENSITIVITY_MULTIPLIER = 10.0;
	
	public static final double DECREASE_SENSITIVITY_MULTIPLIER = 2.5;
	
	public static final int SEGMENT_GENERATION_MULTIPLIER = 50;
}
