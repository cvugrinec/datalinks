package com.datalinks.media.player;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;



public class Mplayer  extends Application{

    private Scene scene;
    private static String TITLE = "DL Media Player";
    
/*
public static void main(String[] args) {
        launch(args);
    }
 */

    @Override
    public void start(Stage stage) throws Exception {
        // create scene
        stage.setTitle(TITLE);
        scene = new Scene(new Browser(stage), 900, 600, Color.web("#666970"));
        stage.setScene(scene);
        // apply CSS style
        scene.getStylesheets().add("BrowserToolbar.css");
        // show stage
        stage.show();

    }
}
