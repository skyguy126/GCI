package com.skyguy126.gci;

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
		Logger.debug("Parser initiated");

		try {
			this.fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
			Logger.debug("Loaded file input");
			this.gCodeArray = new ArrayList<ArrayList<String>>();
		} catch (IOException e) {
			Logger.error(e);
		}
	}

	public ArrayList<ArrayList<String>> getGCodeArray() {
		return this.gCodeArray;
	}

	public MeasurementMode getMeasurementMode() {
		return measurementMode;
	}

	public CoordinateMode getCoordinateMode() {
		return coordinateMode;
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

				// Check for proper comment syntax and remove comments
				if (currentLine.contains("(")) {
					currentLine = currentLine.substring(0, currentLine.indexOf("(")) + "\n";
				} else if (currentLine.contains(")") && !currentLine.contains("(")) {
					Logger.error("Invalid comment syntax at line {}", lineNum);
					valid = false;
					break;
				}

				// Skip over any blank lines
				if (currentLine.trim().isEmpty()) {
					Logger.debug("Empty line at line {}", lineNum);
					continue;
				}

				// Initialize array for loop
				ArrayList<String> cmdList = new ArrayList<String>();

				// Boolean operations are faster than string comparisons so we
				// use short circuit evaluation
				boolean lastCmdExists = false;

				// we cannot use valid for this because even if we find an error
				// we want to check the rest of the file
				boolean loopExit = false;

				// We don't want duplicate x/y/z coordinates
				boolean xExists = false;
				boolean yExists = false;
				boolean zExists = false;
				boolean iExists = false;
				boolean jExists = false;

				// Keep track of commands that need arguments
				String lastCmd = "";

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
						break;
					} else if (cmdNum == 1) {
						// Command following N tag MUST be G or M

						Matcher mg = Pattern.compile("[G]\\d+").matcher(x);
						Matcher mm = Pattern.compile("[M]\\d+").matcher(x);

						if (!mg.matches() && !mm.matches()) {
							Logger.error("Syntax error on line {}. N tag must be followed by a proper G or M tag",
									lineNum);
							valid = false;
							break;
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
							Logger.debug("Found S value: {}", x);
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

					if (lastCmdExists && (lastCmd.equals("G00") || lastCmd.equals("G01") || lastCmd.equals("G02")
							|| lastCmd.equals("G03"))) {

						// Search for X Y Z and F tags following G00 and G01

						if (Pattern.compile("[XYZ]([-])?((\\d+\\.\\d+)|(\\d+)|(\\.\\d+))").matcher(x).matches()) {

							Logger.debug("Detected coordinate value: {}", x);

							// Make sure coordinate can only be defined once on
							// one line
							if (!xExists && x.startsWith("X")) {
								xExists = true;
							} else if (!yExists && x.startsWith("Y")) {
								yExists = true;
							} else if (!zExists && x.startsWith("Z")) {
								zExists = true;
							} else {
								Logger.error("X Y or Z can only be defined once on line {}", lineNum);
								valid = false;
								break;
							}

							cmdList.add(x);
							continue;

						} else if (Pattern.compile("[IJ]([-])?((\\d+\\.\\d+)|(\\d+)|(\\.\\d+))").matcher(x).matches()) {

							Logger.debug("Detected coordinate value for arc: {}", x);

							if (!iExists && x.startsWith("I")) {
								iExists = true;
							} else if (!jExists && x.startsWith("J")) {
								jExists = true;
							} else {
								Logger.error("I or J can only be defined once on line {}", lineNum);
								valid = false;
								break;
							}

							cmdList.add(x);
							continue;

						} else if (Pattern.compile("[F]\\d+").matcher(x).matches()) {

							Logger.debug("Detected F value: {}", x);
							cmdList.add(x);
							continue;

						} else {

							Logger.error("Syntax error on line {}", lineNum);
							valid = false;
							break;

						}
					}

					switch (x) {
					case "G04":
					case "G4":
					case "G05":
					case "G5":
					case "G80":
					case "G81":
					case "G82":
					case "M00":
					case "M0":
					case "M01":
					case "M1":
					case "M06":
					case "M6":
					case "M08":
					case "M8":
					case "M09":
					case "M9":
					case "M10":
					case "M11":
					case "M30":
					case "M47":
					case "M02":
					case "M2":
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
							Logger.debug("Measurement mode: millimeter");
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
					case "M05":
					case "M5":
						cmdList.add(x);
						break;
					case "M03":
					case "M3":
					case "G00":
					case "G0":
					case "G01":
					case "G1":
					case "G02":
					case "G2":
					case "G03":
					case "G3":
						cmdList.add(x);
						lastCmdExists = true;
						lastCmd = x;
						break;
					default:
						if (x.startsWith("T")) {
							Logger.debug("Ignoring T tag on line {}, block {}", lineNum, cmdNum);
						} else {
							Logger.error("Invalid code on line {} block {}", lineNum, cmdNum);
							valid = false;
							loopExit = true;
							break;
						}
					}

					// Exit loop if 1 command block on a line is invalid
					if (loopExit) {
						break;
					}

				}

				// Make sure X Y or Z tag was given for G00 and G01
				if (lastCmdExists && (lastCmd.equals("G00") || lastCmd.equals("G01"))) {
					if (!(xExists || yExists || zExists) || (iExists || jExists)) {
						Logger.error("{} must be supplied with X Y or Z tag on line {}", lastCmd, lineNum);
						valid = false;
					}
				}

				// Make sure I and J tag was given for G02 and G03
				if (lastCmdExists && (lastCmd.equals("G02") || lastCmd.equals("G03"))) {
					if (zExists || !(iExists && jExists)) {
						Logger.error("{} must be supplied with I and J tag on line {}", lastCmd, lineNum);
						valid = false;
					}
				}

				// Add current line to main gcode array if command is valid
				if (valid && !cmdList.isEmpty()) {
					Logger.debug("Command list for line {}: {}", lineNum, cmdList.toString());
					gCodeArray.add(cmdList);
				}
			}

			this.fileReader.close();

			if (coordinateMode == null) {
				Logger.error("Coordinate mode not defined!");
				valid = false;
			}

			if (measurementMode == null) {
				Logger.error("Measurement mode not defined!");
				valid = false;
			}

			if (valid)
				Logger.debug("Parse success!");
			else
				Logger.error("Parse failed!");

			if (Shared.DEBUG_MODE) {
				System.out.println("\n\nCODE ARRAY\n");

				for (ArrayList<String> line : gCodeArray) {
					Logger.debug("----- {} -----", line.toString());
				}

				System.out.println("\n\n\n");
			}

			return valid;
		} catch (IOException e) {
			Logger.error(e);
			return false;
		}
	}
}
