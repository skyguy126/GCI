package com.skyguy126.gci;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import java.awt.geom.RoundRectangle2D;

public class CustomSliderUI extends BasicSliderUI {

	private BasicStroke trackStroke = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	public CustomSliderUI(JSlider s) {
		super(s);
	}

	@Override
	protected Color getFocusColor() {
		return Shared.UI_COLOR;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g, c);
	}

	@Override
	public void paintTrack(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(trackStroke);
		g2d.setPaint(Color.WHITE);
		g2d.drawLine(trackRect.x, trackRect.y + trackRect.height / 2, trackRect.x + trackRect.width,
				trackRect.y + trackRect.height / 2);
		g2d.setStroke(oldStroke);
	}

	@Override
	public void paintThumb(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		int x = thumbRect.x;
		int y = thumbRect.y;
		int width = thumbRect.width;
		int height = thumbRect.height;

		g2d.setPaint(Color.WHITE);
		g2d.fill(new RoundRectangle2D.Float(x, y, width, height, 5, 5));
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

	@Override
	public void paintTicks(Graphics g) {
		Rectangle tickBounds = tickRect;

		g.setColor(Color.WHITE);

		if (slider.getOrientation() == JSlider.HORIZONTAL) {
			g.translate(0, tickBounds.y);

			int value = slider.getMinimum();
			int xPos = 0;

			if (slider.getMinorTickSpacing() > 0) {
				while (value <= slider.getMaximum()) {
					xPos = xPositionForValue(value);
					paintMinorTickForHorizSlider(g, tickBounds, xPos);
					value += slider.getMinorTickSpacing();
				}
			}

			if (slider.getMajorTickSpacing() > 0) {
				value = slider.getMinimum();

				while (value <= slider.getMaximum()) {
					xPos = xPositionForValue(value);
					paintMajorTickForHorizSlider(g, tickBounds, xPos);
					value += slider.getMajorTickSpacing();
				}
			}

			g.translate(0, -tickBounds.y);
		}
	}
}
