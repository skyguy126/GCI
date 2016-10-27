package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Frame;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class LoadingDialog extends JDialog {
	public LoadingDialog(Frame parent, String titleText, String labelText, ImageIcon labelIcon, ImageIcon icon) {
		super(parent, titleText, true);
		
		JPanel loadingDialogPanel = new JPanel();
		JLabel loadingDialogLabel = new JLabel(labelText, labelIcon, JLabel.CENTER);

		loadingDialogPanel.setBackground(Shared.UI_COLOR);
		loadingDialogPanel.setBorder(new EmptyBorder(20, 5, 20, 20));
		loadingDialogLabel.setFont(Shared.UI_FONT);
		loadingDialogLabel.setForeground(Color.WHITE);
		loadingDialogPanel.add(loadingDialogLabel);
		
		add(loadingDialogPanel);
		setBackground(Shared.UI_COLOR);
		setResizable(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setIconImage(icon.getImage());
		pack();
		setLocationRelativeTo(parent);
		
	}
}
