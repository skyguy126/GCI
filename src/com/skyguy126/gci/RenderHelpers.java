package com.skyguy126.gci;

import java.awt.Color;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderHelpers {

	public static void renderTestCube(GL2 gl) {
		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f(10f, -10f, -10f);
		gl.glColor3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(10f, 10f, -10f);
		gl.glColor3f(0.0f, 0.0f, 1.0f);
		gl.glVertex3f(-10f, 10f, -10f);
		gl.glColor3f(1.0f, 0.0f, 1.0f);
		gl.glVertex3f(-10f, -10f, -10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glVertex3f(10f, -10f, 10);
		gl.glVertex3f(10f, 10, 10f);
		gl.glVertex3f(-10f, 10f, 10f);
		gl.glVertex3f(-10f, -10f, 10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 0.0f, 1.0f);
		gl.glVertex3f(10f, -10f, -10f);
		gl.glVertex3f(10f, 10f, -10f);
		gl.glVertex3f(10f, 10f, 10f);
		gl.glVertex3f(10f, -10f, 10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(0.0f, 1.0f, 0.0f);
		gl.glVertex3f(-10f, -10f, 10f);
		gl.glVertex3f(-10f, 10f, 10f);
		gl.glVertex3f(-10f, 10f, -10f);
		gl.glVertex3f(-10f, -10f, -10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(0.0f, 0.0f, 1.0f);
		gl.glVertex3f(10f, 10f, 10f);
		gl.glVertex3f(10f, 10f, -10f);
		gl.glVertex3f(-10f, 10f, -10f);
		gl.glVertex3f(-10f, 10f, 10f);
		gl.glEnd();

		gl.glBegin(GL2.GL_POLYGON);
		gl.glColor3f(1.0f, 0.0f, 0.0f);
		gl.glVertex3f(10f, -10f, -10f);
		gl.glVertex3f(10f, -10f, 10f);
		gl.glVertex3f(-10f, -10f, 10f);
		gl.glVertex3f(-10f, -10f, -10f);
		gl.glEnd();
	}

	public static void renderLine(GL2 gl, Color color, float[] begin, float[] end, float width) {
		gl.glLineWidth(width);
		gl.glColor3f(color.getRed(), color.getGreen(), color.getBlue());
		gl.glBegin(GL2.GL_LINES);
		gl.glVertex3f(begin[0], begin[1], begin[2]);
		gl.glVertex3f(end[0], end[1], end[2]);
		gl.glEnd();
	}
	
	public static void renderText(TextRenderer textRenderer, String text, int posX, int posY, int width, int height) {
		textRenderer.beginRendering(width, height);
		textRenderer.setColor(Color.WHITE);
		textRenderer.setSmoothing(true);
		textRenderer.draw(text, posX, posY);
		textRenderer.endRendering();
		textRenderer.flush();
	}

	public static void render3DText(TextRenderer textRenderer, String text, int posX, int posY, int posZ, float scale) {
		textRenderer.begin3DRendering();
		textRenderer.setColor(Color.WHITE);
		textRenderer.setSmoothing(true);
		textRenderer.draw3D(text, posX, posY, posZ, scale);
		textRenderer.end3DRendering();
		textRenderer.flush();
	}
}
