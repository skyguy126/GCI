package com.skyguy126.gci;

import java.io.File;
import org.pmw.tinylog.Logger;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ControlUI extends Application {
	
	public static Stage mainStage;

	public static void instantiate() {
		Application.launch(ControlUI.class, (java.lang.String[]) null);
	}

	@Override
	public void start(Stage stage) throws Exception {
		try {
			
			// TODO why is getClassLoader() not working?
			
			Parent parent = FXMLLoader.load(new File("fxml/controls.fxml").toURL());
			stage.setScene(new Scene(parent));
			stage.setTitle("Controls");

			// Prevent window closing
			stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent e) {
					e.consume();
				}
			});
			mainStage = stage;
			stage.show();
		} catch (Exception ex) {
			Logger.error(ex);
		}
	}

}
