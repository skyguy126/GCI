package com.skyguy126.gci;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import org.pmw.tinylog.Logger;

@SuppressWarnings("serial")
public class InformationDialog extends JDialog {
	
	public InformationDialog(Frame parent, String titleText, String filePath, ImageIcon icon) {
		super(parent, titleText, true);
		
		setSize(600, 600);
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
			BufferedReader infoHtmlReader = new BufferedReader(
					new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath), "UTF-8"));

			infoHtmlReader.mark(1);
			if (infoHtmlReader.read() != 0xFEFF)
				infoHtmlReader.reset();
			else
				Logger.debug("Removed byte order mark from filestream");

			StringBuffer infoHtml = new StringBuffer();
			String currentLine;

			while ((currentLine = infoHtmlReader.readLine()) != null) {
				infoHtml.append(currentLine);
			}

			infoHtmlReader.close();

			JEditorPane infoPane = new JEditorPane("text/html", infoHtml.toString());
			infoPane.setSize(600, 600);
			BufferedImage infoImg = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration().createCompatibleImage(600, 600);
			infoPane.print(infoImg.getGraphics());

			JPanel infoJPanel = new JPanel() {
				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					g.drawImage(infoImg, 0, 0, null);
				}
			};

			add(infoJPanel);

			Logger.debug("Loaded information html");
		} catch (Exception e) {
			Logger.error("Error loading information html: {}", e);
			System.exit(-1);
		}
	}
}
