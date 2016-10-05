package com.skyguy126.gci;

import org.pmw.tinylog.Logger;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXColorPicker;
import com.jfoenix.controls.JFXSlider;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

public class ControlController {

	@FXML private JFXButton playButton;
	@FXML private JFXButton stopButton;
	@FXML private JFXButton screenShotButton;
	
	@FXML private JFXSlider timeSlider;
	@FXML private JFXColorPicker coordAxisColorPicker;
	
	@FXML
    public void initialize() {
		stopButton.setDisable(true);
		
        timeSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				Logger.debug("Slider value: {}", newValue.intValue());
			}
		});
    }
	
	@FXML
	public void handlePlayButton() {
		playButton.setDisable(true);
		stopButton.setDisable(false);
		timeSlider.setDisable(true);
		timeSlider.setStyle("-fx-opacity: 1");
		
		Logger.debug("Play pressed");
	}
	
	@FXML
	public void handleStopButton() {
		playButton.setDisable(false);
		stopButton.setDisable(true);
		timeSlider.setDisable(false);
		
		Logger.debug("Stop pressed");
	}
}
