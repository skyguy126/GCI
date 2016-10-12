package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class CustomJButton extends JButton {
	CustomJButton(String title) {
		super(title);
		setContentAreaFilled(false);
		super.setForeground(Color.WHITE);
	}
	
	@Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setPaint(Shared.UI_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();

        super.paintComponent(g);
    }
}
