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

				// Initialize array for loop
				ArrayList<String> cmdList = new ArrayList<String>();

				// Boolean operations are faster than string comparisons so we
				// use short circuit evaluation
				boolean lastCmdExists = false;

				// we cannot use valid for this because even if we find an error
				// we want to check the rest of the file
				boolean loopExit = false;

				// Keep track of cmds that need arguments
				String lastCmd = "";

				// This is especially necessary for move commands, do not accept
				// blank move commands
				boolean argsMet = false;

				// Keep track of the command block number
				int cmdNum = -1;

				Logger.debug("Entering loop for line {}", lineNum);

				// Remove \n and double spaces before processing
				for (String x : currentLine.replaceAll("\n", "").replaceAll("\\s+", " ").split(" ")) {
					// Trim trailing whitespace and increment command number
					x = x.trim();
					cmdNum++;

					// Assert N tag at start of line
					if (cmdNum == 0 && !Pattern.compile("[N]\\d+").matcher(x).matches()) {
						Logger.error("Invalid N tag at line {}", lineNum);
						valid = false;
						loopExit = true;
					} else if (cmdNum == 1) {
						// Command following N tag MUST be G or M

						Matcher mg = Pattern.compile("[G]\\d+").matcher(x);
						Matcher mm = Pattern.compile("[M]\\d+").matcher(x);

						if (!mg.matches() && !mm.matches()) {
							Logger.error("Syntax error on line {}. N tag must be followed by a proper G or M tag",
									lineNum);
							valid = false;
							loopExit = true;
						}
					}

					// No need to process the N tag, ignoring that anyways
					if (cmdNum == 0) {
						continue;
					}

					// Last command was M03 to start spindle and we need to get
					// spindle speed
					if (lastCmdExists && lastCmd.equals("M03")) {
						if (Pattern.compile("[S]\\d+").matcher(x).matches()) {
							Logger.debug("Found S value");
							cmdList.add(x);
							lastCmdExists = false;
							lastCmd = "";
							continue;
						} else {
							Logger.error("M03 must be followed by S tag indicating spindle speed on line {}", lineNum);
							valid = false;
							break;
						}
					}

					if (lastCmdExists && lastCmd.equals("G00")) {
						Logger.debug("Found G00 as last cmd");

						// Search for X Y Z and F tags following G00
						if (Pattern.compile("[X]\\d+([.]\\d+)").matcher(x).matches()) {
							Logger.debug("Detected x val");
							cmdList.add(x);
							argsMet = true;
							continue;
						} else if (Pattern.compile("[Y]\\d+([.]\\d+)").matcher(x).matches()) {
							Logger.debug("Detected y val");
							cmdList.add(x);
							argsMet = true;
							continue;
						} else if (Pattern.compile("[Z]\\d+([.]\\d+)").matcher(x).matches()) {
							Logger.debug("Detected z val");
							cmdList.add(x);
							argsMet = true;
							continue;
						} else if (Pattern.compile("[F]\\d+").matcher(x).matches()) {
							Logger.debug("Detected f val");
							cmdList.add(x);
							continue;
						} else if (!argsMet) {
							Logger.error("G00 must be supplied with X Y or Z tag on line {}", lineNum);
							valid = false;
							break;
						}
					}

					switch (x) {
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
					case "M03":
						Logger.debug("Set last cmd to M03");
						cmdList.add(x);
						lastCmdExists = true;
						lastCmd = x;
						break;
					case "G20":
						if (measurementMode == null) {
							measurementMode = MeasurementMode.INCH;
							Logger.debug("Measurement mode: inch");
						} else {
							Logger.error("Measurement mode can only be defined once");
							valid = false;
							loopExit = true;
						}
						break;
					case "G21":
						if (measurementMode == null) {
							measurementMode = MeasurementMode.MILLIMETER;
							Logger.debug("Measurement mode: mm");
						} else {
							Logger.error("Measurement mode can only be defined once");
							valid = false;
							loopExit = true;
						}
						break;
					case "G90":
						if (coordinateMode == null) {
							coordinateMode = CoordinateMode.ABSOLUTE;
							Logger.debug("Coordinate mode: absolute");
						} else {
							Logger.error("Coordinate mode can only be defined once");
							valid = false;
							loopExit = true;
						}
						break;
					case "G91":
						if (coordinateMode == null) {
							coordinateMode = CoordinateMode.RELATIVE;
							Logger.debug("Coordinate mode: relative");
						} else {
							Logger.error("Coordinate mode can only be defined once");
							valid = false;
							loopExit = true;
						}
						break;
					case "G00":
						Logger.debug("Set last cmd to G00");
						cmdList.add(x);
						lastCmdExists = true;
						lastCmd = x;
						break;
					default:
						Logger.error("Invalid code on line {} block {}", lineNum, cmdNum);
						valid = false;
						loopExit = true;
						break;
					}

					if (loopExit) {
						break;
					}

				}

				System.out.println(cmdList);

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
