package com.skyguy126.gci;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import org.pmw.tinylog.Logger;

public class Interpreter {
	private ArrayList<ArrayList<String>> gCodeArray;

	private ArrayList<float[][]> vertexValues;
	private ArrayList<String> spindleSpeedText;
	private ArrayList<String> feedRateText;
	private ArrayList<String> currentCommandText;
	private ArrayList<Color> currentLineColor;
	private ArrayList<Integer> currentTimeScale;

	private float startX;
	private float startY;
	private float startZ;

	private int totalTicks;

	public Interpreter(ArrayList<ArrayList<String>> g) {
		Logger.debug("Interpreter initiated");

		this.gCodeArray = g;
		this.vertexValues = new ArrayList<float[][]>();
		this.spindleSpeedText = new ArrayList<String>();
		this.feedRateText = new ArrayList<String>();
		this.currentCommandText = new ArrayList<String>();
		this.currentLineColor = new ArrayList<Color>();
		this.currentTimeScale = new ArrayList<Integer>();

		this.totalTicks = 0;

		this.startX = 0;
		this.startY = 0;
		this.startZ = 0;
	}

	public boolean generateAbsolute() {

		// Keep track of current commands and other text that needs to be
		// displayed in opengl
		// Whenever we add a point to the pointValues array also add the current
		// spindle speed
		// and current feed rate text to their respective arrays so we can loop
		// through them in
		// opengl
		String curCmd = "";
		String curSpindleSpeedText = "";
		String curFeedRateText = "";

		// Set the start positions set by the user or default them to 0

		float lastX = startX * Shared.SEGMENT_SCALE_MULTIPLIER;
		float lastY = startY * Shared.SEGMENT_SCALE_MULTIPLIER;
		float lastZ = startZ * Shared.SEGMENT_SCALE_MULTIPLIER;
		float lastI = 0f;
		float lastJ = 0f;

		float curX = 0f;
		float curY = 0f;
		float curZ = 0f;

		float curFeedRate = 10f;

		for (int i = 0; i < gCodeArray.size(); i++) {

			// G or M command should be the first item in the array for the
			// current line
			curCmd = gCodeArray.get(i).get(0);
			Logger.debug("{}", curCmd);

			// Check for the command and perform specific actions
			switch (curCmd) {
			case "M05":
				Logger.debug("Found M05");
				break;
			case "M03":
				curSpindleSpeedText = gCodeArray.get(i).get(1).substring(1);
				Logger.debug("Set spindle speed to {}", curSpindleSpeedText);
				break;
			case "G00":
			case "G01":

				// Extract the X Y and Z float values from line
				// Gather F value also

				for (int x = 1; x < gCodeArray.get(i).size(); x++) {
					String curArg = gCodeArray.get(i).get(x);
					Logger.debug("Current arg for {}: {}", curCmd, curArg);

					if (curArg.startsWith("X"))
						curX = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("Y"))
						curY = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("Z"))
						curZ = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("F")) {
						curFeedRateText = curArg.substring(1);
						curFeedRate = Float.parseFloat(curFeedRateText);
					}

				}
				Logger.debug("X{} Y{} Z{} FeedRate {}", curX, curY, curZ, curFeedRateText);

				// Find distance between coordinates and calculate number of
				// segments to draw

				double powX = Math.pow((curX - lastX), 2);
				double powY = Math.pow((curY - lastY), 2);
				double powZ = Math.pow((curZ - lastZ), 2);
				double distance = Math.sqrt(powX + powY + powZ);

				// Divide by feed rate to find actual simulation speed
				int numSegments = (int) ((distance * Shared.SEGMENT_GENERATION_MULTIPLIER) / curFeedRate);
				float xSegmentLength = (float) ((curX - lastX) / numSegments);
				float ySegmentLength = (float) ((curY - lastY) / numSegments);
				float zSegmentLength = (float) ((curZ - lastZ) / numSegments);

				Logger.debug("distance: {}, segments: {}", distance, numSegments);
				Logger.debug("last xyz {}, {}, {}", lastX, lastY, lastZ);
				Logger.debug("cur xyz {}, {}, {}", curX, curY, curZ);
				Logger.debug("xSlope {} ySlope {} zSlope {}", xSegmentLength, ySegmentLength, zSegmentLength);

				Logger.debug("---------- BEGIN VERTEX ARRAY ----------");

				for (int x = 0; x < numSegments; x++) {
					float[][] vertexArray = new float[2][3];

					// We need to flip the Y and Z coordinates and multiply Y by
					// -1 because
					// the XY plane is moving horizontally while the Z axis is
					// moving vertically on a CNC
					// The GL coordinate system uses the Y axis for vertical
					// movement

					vertexArray[0][0] = lastX;
					vertexArray[0][2] = lastY * -1;
					vertexArray[0][1] = lastZ;

					lastX += xSegmentLength;
					lastY += ySegmentLength;
					lastZ += zSegmentLength;

					vertexArray[1][0] = lastX;
					vertexArray[1][2] = lastY * -1;
					vertexArray[1][1] = lastZ;

					if (curCmd.equals("G00")) {
						this.currentLineColor.add(Color.GREEN);
					} else if (curCmd.equals("G01")) {
						this.currentLineColor.add(Color.BLUE);
					}

					Logger.debug("{}", Arrays.deepToString(vertexArray));

					// Add values to main vertex array
					this.vertexValues.add(vertexArray);
					this.feedRateText.add("Feed Rate: " + curFeedRateText);
					this.spindleSpeedText.add("Spindle Speed: " + curSpindleSpeedText);
					this.currentCommandText.add("Current Command: " + curCmd);
					this.currentTimeScale.add(1);

					this.totalTicks++;
				}

				Logger.debug("---------- END VERTEX ARRAY ----------");

				// Reset last coordinates for next cycle
				lastX = curX;
				lastY = curY;
				lastZ = curZ;
				break;
			case "G02":
			case "G03":
				Logger.debug("Arc interpolation");

				for (int x = 1; x < gCodeArray.get(i).size(); x++) {
					String curArg = gCodeArray.get(i).get(x);
					Logger.debug("Current arg for G00: {}", curArg);

					if (curArg.startsWith("X"))
						curX = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("Y"))
						curY = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("Z"))
						curZ = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("I"))
						lastI = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("J"))
						lastJ = Float.parseFloat(curArg.substring(1)) * Shared.SEGMENT_SCALE_MULTIPLIER;
					else if (curArg.startsWith("F")) {
						curFeedRateText = curArg.substring(1);
						curFeedRate = Float.parseFloat(curFeedRateText);
					}

				}
				Logger.debug("X{} Y{} Z{} I{} J{} FeedRate {}", curX, curY, curZ, lastI, lastJ, curFeedRateText);

				// Find radius with distance formula
				double radius = Math.sqrt(Math.pow((curX - lastI), 2) + Math.pow((curY - lastJ), 2));
				Logger.debug("Radius: {}", radius);

				// Law of cosines to find total number radians to travel
				double cDistance = Math.sqrt(Math.pow((curX - lastX), 2) + Math.pow((curY - lastY), 2));
				double cbaValue = Math.pow(cDistance, 2) - 2 * (Math.pow(radius, 2));
				double abValue = -2 * Math.pow(radius, 2);

				double totalTheta = Math.acos(cbaValue / abValue);

				Logger.debug("Last X: {} Last Y: {}", lastX, lastY);
				Logger.debug("Last I: {} last J: {}", lastI, lastJ);
				Logger.debug("curX: {} curY: {}", curX, curY);
				Logger.debug("Total theta: {}", totalTheta);

				double totalArcSegments = (int) (totalTheta * Shared.SEGMENT_GENERATION_MULTIPLIER
						* Shared.ARC_GENERATION_MULTIPLIER / curFeedRate);
				float dTheta = (float) (totalTheta / (totalArcSegments - 1));
				float sTheta =(float) (Math.atan2(lastY - lastJ, lastX - lastI));
				Logger.debug("Starting theta: {}", sTheta);
				if (curZ != lastZ)
					Logger.warn("Ramping detected");

				for (int x = 0; x < totalArcSegments; x++) {
					float[][] vertexArray = new float[2][3];

					vertexArray[0][0] = lastX;
					vertexArray[0][2] = lastY * -1;
					vertexArray[0][1] = lastZ;

					if (curCmd.equals("G03")) {
						lastX = (float) (lastI + radius * Math.cos(sTheta + x * dTheta));
						lastY = (float) (lastJ + radius * Math.sin(sTheta + x * dTheta));
					}

					if (curCmd.equals("G02")) {
						lastX = (float) (lastI + radius * Math.cos(sTheta - x * dTheta));
						lastY = (float) (lastJ + radius * Math.sin(sTheta - x * dTheta));
					}

					vertexArray[1][0] = lastX;
					vertexArray[1][2] = lastY * -1;
					vertexArray[1][1] = lastZ;
					Logger.debug("{}", Arrays.deepToString(vertexArray));
					// Add values to main vertex array
					this.vertexValues.add(vertexArray);
					this.feedRateText.add("Feed Rate: " + curFeedRateText);
					this.currentLineColor.add(Color.BLUE);
					this.spindleSpeedText.add("Spindle Speed: " + curSpindleSpeedText);
					this.currentCommandText.add("Current Command: " + curCmd);
					this.currentTimeScale.add(Shared.ARC_GENERATION_MULTIPLIER);

					this.totalTicks++;

				}

				// TODO arc generation
				// Reset last coordinates for next cycle
				Logger.debug("---------- END VERTEX ARRAY ----------");
				lastX = curX;
				lastY = curY;
				lastZ = curZ;

				break;
			default:
				Logger.error("Error at command {}", curCmd);
				return false;
			}
		}

		Logger.debug("Vertex array size: {} Total ticks: {} TimeScale Size: {}", this.vertexValues.size(),
				this.totalTicks, this.currentTimeScale.size());
		return true;
	}

	// Set start position or use default value of 0
	public void setStartPosition(float x, float y, float z) {
		this.startX = x;
		this.startY = y;
		this.startZ = z;
	}

	public ArrayList<float[][]> getVertexValues() {
		return vertexValues;
	}

	public ArrayList<String> getSpindleSpeedText() {
		return spindleSpeedText;
	}

	public ArrayList<Integer> getCurrentTimeScale() {
		return currentTimeScale;
	}

	public ArrayList<String> getFeedRateText() {
		return feedRateText;
	}

	public ArrayList<String> getCurrentCommandText() {
		return currentCommandText;
	}

	public int getTotalTicks() {
		return totalTicks;
	}

	public ArrayList<Color> getCurrentLineColor() {
		return currentLineColor;
	}
}
