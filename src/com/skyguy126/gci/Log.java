package com.skyguy126.gci;

import java.util.EnumSet;
import java.util.Set;
import javax.swing.JLabel;
import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.VMShutdownHook;
import org.pmw.tinylog.writers.Writer;



public class Log implements Writer{
	
	private RenderUI rui;
	
	public Log(RenderUI x){
		rui = x;
	}
	
	@Override
	public void close() throws Exception {

	}

	@Override
	public void flush() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<LogEntryValue> getRequiredLogEntryValues() {
		// TODO Auto-generated method stub
		return 
	}

	@Override
	public void init(Configuration arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(LogEntry logData) throws Exception {
		// TODO Auto-generated method stub
		rui.appendToLog("wew");
		rui.appendToLog(logData);
		System.out.println("kek");
	}
	
}
