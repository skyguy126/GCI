package com.skyguy126.gci;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class CustomJDialog extends JDialog {
	public CustomJDialog(Frame parent, String titleText, String labelText, String buttonText,
			Runnable buttonAction, ImageIcon icon) {
		super(parent, titleText, true);

		JPanel messagePane = new JPanel();
		JPanel buttonPane = new JPanel();
		JLabel label = new JLabel(labelText, JLabel.CENTER);

		CustomJButton extensionErrorDialogButton = new CustomJButton(buttonText);
		extensionErrorDialogButton.setFocusPainted(false);
		extensionErrorDialogButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonAction.run();
			}
		});

		label.setForeground(Color.WHITE);
		label.setFont(Shared.UI_FONT);
		messagePane.add(label);
		messagePane.setBackground(Shared.UI_COLOR);
		messagePane.setBorder(new EmptyBorder(5, 20, 0, 20));
		buttonPane.add(extensionErrorDialogButton);
		buttonPane.setBackground(Shared.UI_COLOR);

		add(messagePane, BorderLayout.CENTER);
		add(buttonPane, BorderLayout.SOUTH);
		setIconImage(icon.getImage());
		setResizable(false);
		pack();
		setLocationRelativeTo(parent);
	}
}
