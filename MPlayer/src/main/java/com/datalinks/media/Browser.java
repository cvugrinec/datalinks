/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datalinks.media;

import java.net.URISyntaxException;
import java.net.URL;
import javafx.geometry.HPos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 *
 * @author chris
 */
public class Browser extends Region {

    final ComboBox comboBox = new ComboBox();
    final WebView browser = new WebView();
    final WebEngine webEngine;
    private final HBox toolBar;
    
    public Browser(Stage stage) throws URISyntaxException {
        
        this.webEngine = browser.getEngine();
       //apply the styles
        getStyleClass().add("browser");

        comboBox.setPrefWidth(60);
        
                // create the toolbar
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);

        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().add(comboBox);
     
        webEngine.load(this.getClass().getResource("/azure-demo.html").toExternalForm());
        
        getChildren().add(toolBar);
        getChildren().add(browser);

    }
    
       @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(browser,0,0,w,h-tbHeight,0,HPos.CENTER,VPos.CENTER);
        layoutInArea(toolBar,0,h-tbHeight,w,tbHeight,0,HPos.CENTER,VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 900;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 600;
    }

    
}
