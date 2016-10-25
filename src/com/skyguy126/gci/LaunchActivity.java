package com.skyguy126.gci;

import java.awt.Color;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import com.sun.awt.AWTUtilities;

public class LaunchActivity {

	public void launch() {
		System.out.println("Launching GCI...");

		JFrame splash = new JFrame();
		JLabel splashContent = new JLabel("GCI - " + Shared.VERSION,
				new ImageIcon(getClass().getClassLoader().getResource("res/launch.gif")), JLabel.CENTER);

		splashContent.setForeground(Color.WHITE);
		splashContent.setFont(Shared.UI_FONT);
		splashContent.setBorder(new EmptyBorder(20, 5, 20, 20));

		splash.setUndecorated(true);
		AWTUtilities.setWindowShape(splash, new RoundRectangle2D.Double(0, 0, 250, 100, 50, 50));
		splash.setSize(250, 100);
		splash.setAlwaysOnTop(true);
		splash.getContentPane().setBackground(Shared.UI_COLOR);
		splash.add(splashContent);
		splash.setLocationRelativeTo(null);
		splash.setVisible(true);

		try {
			CountDownLatch status = new CountDownLatch(1);
			new RenderUI(status);
			status.await();
			splash.dispose();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		new LaunchActivity().launch();
	}
}
