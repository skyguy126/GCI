package com.skyguy126.gci;

import java.awt.Color;

import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderHelpers {

	public static void renderText(TextRenderer textRenderer, String text, int posX, int posY, int width, int height) {
		textRenderer.beginRendering(width, height);
		textRenderer.setColor(Color.WHITE);
		textRenderer.setSmoothing(true);
		textRenderer.draw(text, posX, posY);
		textRenderer.endRendering();
		textRenderer.flush();
	}
}
