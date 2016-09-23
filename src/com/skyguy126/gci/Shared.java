package com.skyguy126.gci;

import org.pmw.tinylog.Level;

public class Shared {
	public static final String LOG_FORMAT = "{date} - {level}: {message}";
	public static final String VERSION = "v1.0";
	
	public static final boolean DEBUG_MODE = true;
	public static final Level LOG_LEVEL = (DEBUG_MODE) ? Level.DEBUG : Level.INFO;
}
