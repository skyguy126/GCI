package com.skyguy126.gci;

import org.pmw.tinylog.Logger;

import com.jfoenix.controls.JFXButton;

import javafx.fxml.FXML;

public class ControlController {

	@FXML private JFXButton playButton;
	@FXML private JFXButton stopButton;
	
	@FXML
	public void handlePlayButton() {
		Logger.debug("Play");
	}
	
	@FXML
	public void handleStopButton() {
		Logger.debug("Stop");
	}
	
}
