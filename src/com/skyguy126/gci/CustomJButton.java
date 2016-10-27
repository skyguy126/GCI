package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class CustomJButton extends JButton {

	public CustomJButton(String title) {
		super(title);
		setContentAreaFilled(false);
		setForeground(Color.WHITE);
		setFocusPainted(false);
		setFont(Shared.UI_FONT);
	}

	@Override
	protected void paintComponent(Graphics g) {
		final Graphics2D g2d = (Graphics2D) g.create();

		g2d.setPaint(Shared.UI_COLOR);
		g2d.fillRect(0, 0, getWidth(), getHeight());
		g2d.dispose();

		super.paintComponent(g);
	}
}
