package com.skyguy126.gci;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.pmw.tinylog.Logger;

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
				String lastNTag = (Shared.VALIDATE_N_TAG) ? "" : "N/A";

				// Keep track of the command block number
				int cmdNum = -1;

				String[] currentLineArray = currentLine.replaceAll("\n", "").replaceAll("\\s+", " ").split(" ");
				int currentLineSize = currentLineArray.length;

				Logger.debug("Entering loop for line {}, arg size: {}", lineNum, currentLineSize);

				// Remove \n and double spaces before processing
				for (String x : currentLineArray) {
					// Trim trailing whitespace and increment command number
					x = x.trim();
					cmdNum++;

					// Assert N tag at start of line
					if (cmdNum == 0) {
						boolean validNTag = Pattern.compile("[N]\\d+").matcher(x).matches();

						if (Shared.VALIDATE_N_TAG && !validNTag) {
							Logger.error("Invalid N tag at line {}", lineNum);
							valid = false;
							break;
						} else if (Shared.VALIDATE_N_TAG && validNTag && currentLineSize == 1) {
							Logger.warn("Empty statement at line {} ({})", lineNum, lastNTag);
							break;
						} else if (validNTag) {
							lastNTag = x;
							continue;
						}
					}

					if (Pattern.compile("[S]\\d+").matcher(x).matches()) {
						Logger.debug("Detected S value: {}", x);
						cmdList.add(x);
						continue;
					}

					// Search for X Y Z and F tags following G0 and G1

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
							Logger.error("X Y or Z can only be defined once on line {} ({})", lineNum, lastNTag);
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
							Logger.error("I or J can only be defined once on line {} ({})", lineNum, lastNTag);
							valid = false;
							break;
						}

						cmdList.add(x);
						continue;

					} else if (Pattern.compile("[F]((\\d+\\.\\d+)|(\\d+)|(\\.\\d+))").matcher(x).matches()) {

						Logger.debug("Detected F value: {}", x);
						cmdList.add(x);
						continue;

					} else if (Pattern.compile("[S]\\d+").matcher(x).matches()) {

						Logger.debug("Detected S value: {}", x);
						cmdList.add(x);
						continue;

					}

					if (Pattern.compile("[GMT]\\d+").matcher(x).matches()) {
						char gCodeCmd = x.charAt(0);

						if (gCodeCmd == 'T') {
							Logger.debug("Ignoring T tag on line {}, block {}", lineNum, cmdNum);
							continue;
						}

						int gCodeValue = Integer.parseInt(x.substring(1));
						String gCode = gCodeCmd + String.valueOf(gCodeValue);

						switch (gCode) {
						case "G4":
						case "G5":
						case "G80":
						case "G81":
						case "G82":
						case "M0":
						case "M1":
						case "M6":
						case "M8":
						case "M9":
						case "M10":
						case "M11":
						case "M30":
						case "M47":
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
						case "M5":
							cmdList.add(gCode);
							break;
						case "M3":
						case "G0":
						case "G1":
						case "G2":
						case "G3":
							cmdList.add(gCode);
							break;
						default:
							Logger.error("Invalid code on line {} block {} ({})", lineNum, cmdNum, lastNTag);
							valid = false;
							loopExit = true;
							break;
						}

					} else if (Pattern.compile("[S]\\d+").matcher(x).matches()) {
						Logger.debug("Detected S value: {}", x);
						cmdList.add(x);
					} else if (Pattern.compile("[F]((\\d+\\.\\d+)|(\\d+)|(\\.\\d+))").matcher(x).matches()) {
						Logger.debug("Detected F value: {}", x);
						cmdList.add(x);
					} else {
						Logger.error("Invalid code on line {} block {} ({})", lineNum, cmdNum, lastNTag);
						valid = false;
						break;
					}

					// Exit loop if 1 command block on a line is invalid
					if (loopExit) {
						break;
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
				for (ArrayList<String> line : gCodeArray) {
					Logger.debug("----- {} -----", line.toString());
				}
			}

			return valid;
		} catch (IOException e) {
			Logger.error(e);
			return false;
		}
	}
}
