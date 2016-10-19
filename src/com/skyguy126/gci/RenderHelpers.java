package com.skyguy126.gci;

import java.awt.Color;

import org.pmw.tinylog.Logger;

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

	public static void renderStatic3DText(GL2 gl, TextRenderer textRenderer, String text, int posX, int posY, int posZ,
			float scale, float curAngleX, float curAngleY) {
		
		// TODO
		
		textRenderer.begin3DRendering();
		textRenderer.setColor(Color.WHITE);
		textRenderer.setSmoothing(true);
		float radX = (float) Math.toRadians(curAngleX);
		float radY = (float) Math.toRadians(curAngleY);
		float staticX = (float) (Math.cos(-radX) * posX);
		float staticZ = (float) (Math.sin(-radX) * posX);
		float hyp = (float) (Math.sqrt(Math.pow(staticX, 2) + Math.pow(staticZ, 2)));
		float staticY = (float) (Math.sin(-radY) * hyp);
		textRenderer.draw3D(text, staticX, 0, staticZ, scale);
		gl.glRotatef(-curAngleX, 0, 1, 0);
		gl.glRotatef(-curAngleY, 1, 0, 0);
		textRenderer.end3DRendering();
		textRenderer.flush();
	}
}
