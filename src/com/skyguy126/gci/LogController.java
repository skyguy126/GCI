package com.skyguy126.gci;

import java.util.EnumSet;
import java.util.Set;

import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.VMShutdownHook;
import org.pmw.tinylog.writers.Writer;

public class LogController implements Writer {
	
	private LogWriter logWriter;
	
	public LogController(LogWriter w) {
		this.logWriter = w;
	}

	@Override
	public void close() throws Exception {
		VMShutdownHook.unregister(this);
	}

	@Override
	public void flush() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<LogEntryValue> getRequiredLogEntryValues() {
		return EnumSet.of(LogEntryValue.RENDERED_LOG_ENTRY);
	}

	@Override
	public void init(Configuration arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(LogEntry entry) throws Exception {
		String logEntry = entry.getLevel() + " - " + entry.getMessage();
		System.out.println(entry.getDate() + ": " + logEntry);
		logWriter.appendToLog(logEntry, entry.getLevel());		
	}
}
