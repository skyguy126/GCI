package com.skyguy126.gci;

import java.awt.Color;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.border.EmptyBorder;

public class LaunchActivity {

	public void launch() {
		System.out.println("Launching GCI...");

		JWindow splash = new JWindow();
		JLabel splashContent = new JLabel("Launching GCI...", new ImageIcon(getClass().getClassLoader().getResource("res/launch.gif")), JLabel.CENTER);

		splashContent.setForeground(Color.WHITE);
		splashContent.setFont(Shared.BUTTON_FONT);
		splashContent.setBorder(new EmptyBorder(20, 5, 20, 20));

		splash.setAlwaysOnTop(true);
		splash.getContentPane().setBackground(Shared.UI_COLOR);
		splash.add(splashContent);
		splash.pack();
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
