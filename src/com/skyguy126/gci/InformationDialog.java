package com.skyguy126.gci;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.pmw.tinylog.Logger;

@SuppressWarnings("serial")
public class InformationDialog extends JDialog {
	
	public InformationDialog(Frame parent, String titleText, String filePath, ImageIcon icon) {
		super(parent, titleText, true);
		
		setSize(600, 400);
		setResizable(false);
		setLocationRelativeTo(parent);
		setIconImage(icon.getImage());
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				setVisible(false);
			}
		});

		try {
			
			BufferedImage infoImg = ImageIO.read(getClass().getClassLoader().getResource("res/info.png"));
			JPanel infoJPanel = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					g.drawImage(infoImg, 0, 0, null);
				}
			};

			add(infoJPanel);

			Logger.debug("Loaded information");
		} catch (Exception e) {
			Logger.error("Error loading information: {}", e);
			System.exit(-1);
		}
	}
}
