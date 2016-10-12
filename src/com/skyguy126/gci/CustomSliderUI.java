package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

public class CustomSliderUI extends BasicSliderUI {

	public CustomSliderUI(JSlider s) {
		super(s);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g, c);
	}

	// ONLY FOR HORIZONTAL SLIDERS
	
	@Override
	public void paintTrack(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setStroke(g2d.getStroke());
		g2d.setPaint(Color.WHITE);
		g2d.drawLine(trackRect.x, trackRect.y + trackRect.height / 2, trackRect.x + trackRect.width,
				trackRect.y + trackRect.height / 2);
	}

	@Override
	protected void scrollDueToClickInTrack(int direction) {
		int value = slider.getValue();

		if (slider.getOrientation() == JSlider.HORIZONTAL) {
			value = this.valueForXPosition(slider.getMousePosition().x);
		} else if (slider.getOrientation() == JSlider.VERTICAL) {
			value = this.valueForYPosition(slider.getMousePosition().y);
		}

		slider.setValue(value);
	}
}
