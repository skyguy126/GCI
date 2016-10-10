package com.skyguy126.gci;

import org.pmw.tinylog.Level;

public interface LogWriter {
	void appendToLog(String entry, Level level);
}
