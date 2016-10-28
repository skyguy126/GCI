package com.skyguy126.gci;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import org.pmw.tinylog.Logger;

public class Interpreter {
	private static final Color lineColorBlue = new Color(0, 255, 255);

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

	private float minZ;
	private float maxZ;
	private float minY;
	private float maxY;
	private float minX;
	private float maxX;
	
	private boolean rampFlag;
	
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

		this.startX = 0;
		this.startY = 0;
		this.startZ = 0;
		
		this.totalTicks = 0;

		this.minX = 0f;
		this.maxX = 0f;
		this.minY = 0f;
		this.maxY = 0f;
		this.minZ = 0f;
		this.maxZ = 0f;
		
		this.rampFlag = false;
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

			// Parse all arguments in current line
			for (int x = 0; x < gCodeArray.get(i).size(); x++) {
				String curArg = gCodeArray.get(i).get(x);
				Logger.debug("Current code block: {}", curArg);

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
				} else if (curArg.startsWith("S")) {
					curSpindleSpeedText = curArg.substring(1);
				}
			}

			curCmd = gCodeArray.get(i).get(0);

			if (!(curCmd.startsWith("G") || curCmd.startsWith("M")))
				continue;

			Logger.debug("Current command: {}", curCmd);

			// Check for the command and perform specific actions
			switch (curCmd) {
			case "M5":
				Logger.debug("Found M05");
				break;
			case "M3":
				curSpindleSpeedText = gCodeArray.get(i).get(1).substring(1);
				Logger.debug("Set spindle speed to {}", curSpindleSpeedText);
				break;
			case "G0":
			case "G1":
				processBounds(curX, curY, curZ);

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

					if (curCmd.equals("G0")) {
						this.currentLineColor.add(Color.GREEN);
					} else if (curCmd.equals("G1")) {
						this.currentLineColor.add(lineColorBlue);
					}

					Logger.debug("{}", Arrays.deepToString(vertexArray));

					// Add values to main vertex array
					this.vertexValues.add(vertexArray);
					this.feedRateText.add(curFeedRateText);
					this.spindleSpeedText.add(curSpindleSpeedText);
					this.currentCommandText.add(curCmd);
					this.currentTimeScale.add(1);
					this.totalTicks++;
				}

				Logger.debug("---------- END VERTEX ARRAY ----------");

				// Reset last coordinates for next cycle
				lastX = curX;
				lastY = curY;
				lastZ = curZ;
				break;
			case "G2":
			case "G3":
				Logger.debug("Arc interpolation");
				
				// I and J values are ALWAYS relative
				
				lastI += lastX;
				lastJ += lastY;
				
				Logger.debug("X{} Y{} Z{} I{} J{} FeedRate {}", curX, curY, curZ, lastI, lastJ, curFeedRateText);

				// Find radius with distance formula
				double radius = Math.sqrt(Math.pow((curX - lastI), 2) + Math.pow((curY - lastJ), 2));
				Logger.debug("Radius: {}", radius);

				double angleS = Math.atan2(lastY - lastJ, lastX - lastI);
				double angleE = Math.atan2(curY - lastJ, curX - lastI);
				double totalTheta = angleS - angleE;

				if (totalTheta < 0) {
					totalTheta += 2 * Math.PI;
				}

				if (curCmd.equals("G3")) {
					totalTheta = 2 * Math.PI - totalTheta;
				}
				
				double arcLength = totalTheta * radius;
				double totalArcSegments = (int) (arcLength * Shared.SEGMENT_GENERATION_MULTIPLIER
						* Shared.ARC_GENERATION_MULTIPLIER / curFeedRate);
				double dTheta = totalTheta / (totalArcSegments - 1);

				Logger.debug("Starting Angle: {} Ending Angle: {}", angleS, angleE);
				Logger.debug("Last X: {} Last Y: {}", lastX, lastY);
				Logger.debug("Last I: {} last J: {}", lastI, lastJ);
				Logger.debug("curX: {} curY: {}", curX, curY);
				Logger.debug("Total theta: {}", totalTheta);

				if (!rampFlag && curZ != lastZ) {
					Logger.warn("Ramping detected");
					rampFlag = true;
				}
					

				Logger.debug("---------- BEGIN VERTEX ARRAY ----------");

				for (int x = 0; x < totalArcSegments; x++) {
					float[][] vertexArray = new float[2][3];

					vertexArray[0][0] = lastX;
					vertexArray[0][2] = lastY * -1;
					vertexArray[0][1] = lastZ;

					if (curCmd.equals("G3")) {
						lastX = (float) (lastI + radius * Math.cos(angleS + x * dTheta));
						lastY = (float) (lastJ + radius * Math.sin(angleS + x * dTheta));
					}

					if (curCmd.equals("G2")) {
						lastX = (float) (lastI + radius * Math.cos(angleS - x * dTheta));
						lastY = (float) (lastJ + radius * Math.sin(angleS - x * dTheta));
					}

					vertexArray[1][0] = lastX;
					vertexArray[1][2] = lastY * -1;
					vertexArray[1][1] = lastZ;

					Logger.debug("{}", Arrays.deepToString(vertexArray));
					processBounds(lastX, lastY, lastZ);

					this.vertexValues.add(vertexArray);
					this.feedRateText.add(curFeedRateText);
					this.currentLineColor.add(lineColorBlue);
					this.spindleSpeedText.add(curSpindleSpeedText);
					this.currentCommandText.add(curCmd);
					this.currentTimeScale.add(Shared.ARC_GENERATION_MULTIPLIER);
					this.totalTicks++;
				}

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

		Logger.debug("Vertex array size: {} TimeScale Size: {} Color size: {}", this.vertexValues.size(),
				this.currentTimeScale.size(), this.currentLineColor.size());
		return true;
	}

	private void processBounds(float x, float y, float z) {
		if (x < this.minX)
			this.minX = x;
		else if (x > this.maxX)
			this.maxX = x;

		if (y < this.minY)
			this.minY = y;
		else if (y > this.maxY)
			this.maxY = y;

		if (z < this.minZ)
			this.minZ = z;
		else if (z > this.maxZ)
			this.maxZ = z;
	}

	public float[] getBounds() {
		return new float[] { this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ };
	}

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

	public ArrayList<Color> getCurrentLineColor() {
		return currentLineColor;
	}

	public int getTotalTicks() {
		return totalTicks;
	}
}
