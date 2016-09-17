package com.skyguy126.gci;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
	
	private BufferedReader reader;
	
	public Parser(String filePath) {
		try {
			this.reader = new BufferedReader(new FileReader(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
