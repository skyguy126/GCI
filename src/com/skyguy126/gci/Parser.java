package com.skyguy126.gci;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

	private BufferedReader fileReader;
	private MeasurementMode measurementMode = null;
	private CoordinateMode coordinateMode = null;
	private ArrayList<ArrayList<String>> gCodeArray;

	public Parser(String filePath) {

		Configurator.defaultConfig().formatPattern("{date} - {level}: {message}").level(Level.DEBUG).activate();

		Logger.debug("Parser logger initiated");

		try {
			this.fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
			Logger.debug("Loaded file input");
		} catch (IOException e) {
			Logger.error(e);
		}
	}

	public boolean parse() {

		boolean valid = true;

		try {

			// Remove the byte order mark from start of file
			this.fileReader.mark(1);
			if (this.fileReader.read() != 0xFEFF)
				this.fileReader.reset();
			else
				Logger.debug("Removed byte order mark from filestream");

			String currentLine;
			int lineNum = 0;

			// Loop through each line of file
			while ((currentLine = this.fileReader.readLine()) != null) {

				lineNum++;

				// Skip over any blank lines
				if (currentLine.trim().isEmpty()) {
					Logger.warn("Empty line at line {}", lineNum);
					continue;
				}

				// Check for proper comment syntax and remove comments
				if (currentLine.contains("(")) {
					currentLine = currentLine.substring(0, currentLine.indexOf("(")) + "\n";
				} else if (currentLine.contains(")") && !currentLine.contains("(")) {
					Logger.error("Invalid comment syntax at line {}", lineNum);
					valid = false;
					break;
				}

				// Begin looping through each command
				ArrayList<String> cmdList = new ArrayList<String>();
				boolean exitLoop = false;
				String lastCmd = "";
				int cmdNum = -1;

				// Remove \n and double spaces before processing
				for (String x : currentLine.replaceAll("\n", "").replaceAll("\\s+", " ").split(" ")) {
					cmdNum++;

					// Assert N tag at start of line
					if (cmdNum == 0 && !Pattern.compile("[N]\\d+").matcher(x.trim()).matches()) {
						Logger.error("Invalid N tag at line {}", lineNum);
						valid = false;
					} else if (cmdNum == 1) {
						// Command following N tag MUST be G or M

						Matcher mg = Pattern.compile("[G]\\d+").matcher(x.trim());
						Matcher mm = Pattern.compile("[M]\\d+").matcher(x.trim());

						if (!mg.matches() && !mm.matches()) {
							Logger.error("Syntax error on line {}. N tag must be followed by a proper G or M tag",
									lineNum);
							valid = false;
						}
					}
					
					if (cmdNum == 0) {
						continue;
					}
					
					switch (x.trim()) {
						case "G04":
						case "G05":
						case "G80":
						case "G81":
						case "G82":
						case "M00":
						case "M01":
						case "M06":
						case "M08":
						case "M09":
						case "M10":
						case "M11":
						case "M30":
						case "M47":
							break;
						case "G20":
							if (measurementMode == null) {
								measurementMode = MeasurementMode.INCH;
								Logger.debug("Measurement mode: in");
							} else {
								Logger.error("Measurement mode can only be defined once");
								exitLoop = true;
								valid = false;
							}
							break;
						case "G21":
							if (measurementMode == null) {
								measurementMode = MeasurementMode.MILLIMETER;	
								Logger.debug("Measurement mode: mm");
							} else {
								Logger.error("Measurement mode can only be defined once");
								exitLoop = true;
								valid = false;
							}
							break;
						case "G90":
							if (coordinateMode == null) {
								coordinateMode = CoordinateMode.ABSOLUTE;	
							} else {
								Logger.error("Coordinate mode can only be defined once");
								exitLoop = true;
								valid = false;
							}
							break;
						case "G91":
							if (coordinateMode == null) {
								coordinateMode = CoordinateMode.RELATIVE;	
							} else {
								Logger.error("Coordinate mode can only be defined once");
								exitLoop = true;
								valid = false;
							}
							break;
						default:
							Logger.error("Invalid code on line {}", lineNum);
							exitLoop = true;
							valid = false;
							break;
					}

					if (exitLoop) {
						break;
					}

				}
			}

			this.fileReader.close();

			if (valid)
				Logger.info("Parse success!");
			else
				Logger.error("Parse failed!");

			return valid;
		} catch (IOException e) {
			Logger.error(e);
			return false;
		}
	}
}
