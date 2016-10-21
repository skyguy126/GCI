package com.skyguy126.gci;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JTextField;

@SuppressWarnings("serial")
public class CustomJTextField extends JTextField {
	public CustomJTextField() {
		super();
		setEditable(false);
		setFont(Shared.UI_FONT);
		setBackground(Shared.UI_COLOR);
		setForeground(Color.WHITE);
		setHorizontalAlignment(JTextField.CENTER);
	}
}
