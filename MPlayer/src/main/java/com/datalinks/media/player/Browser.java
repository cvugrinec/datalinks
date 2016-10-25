package com.datalinks.media.player;

import com.datalinks.media.backend.MediaBackendServices;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebErrorEvent;


/**
 *
 * @author chris
 */
public class Browser extends Region {

    final static Logger logger = Logger.getLogger("Browser");
    final static String USER_AGENTS = "Safari/537.44 Mozilla/5.0 AppleWebKit/537.44 JavaFX/8.0";

    MediaBackendServices mbes = new MediaBackendServices();
    ObservableList<Hyperlink> options = FXCollections.observableArrayList();
    
            
    final ComboBox comboBox;
    final WebView browser = new WebView();
    final WebEngine webEngine;
    private final HBox toolBar;
    
    public Browser(Stage stage) throws URISyntaxException {

        
        this.comboBox = new ComboBox(options);
        this.webEngine = browser.getEngine();
                
        comboBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue ov, Object t, Object t1) -> {
            logger.log(Level.INFO,"Changed to : {0}",t1.toString());
        });
        
        
       //apply the styles
        getStyleClass().add("browser");

        comboBox.setPrefWidth(60);
        
                // create the toolbar
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);

        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().add(comboBox);

        String srcBrowser = this.getClass().getResource("/azure-internal.html").toExternalForm();

        //  TODO: Hier nog mee experimeteren
        webEngine.setUserAgent(USER_AGENTS);
        webEngine.setJavaScriptEnabled(true);
        
        webEngine.setOnAlert((WebEvent<String> arg0) -> {
            logger.log(Level.INFO,"Oeps: {0}",arg0.toString());
        });
        
        webEngine.setOnError((WebErrorEvent t) -> {
            logger.log(Level.SEVERE,"Error occured: {0}",t.getMessage());
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        });

        //  Info
        logger.log(Level.INFO, "Agent: {0}", webEngine.userAgentProperty().getValue());
        logger.log(Level.INFO, "File: {0}", srcBrowser);
        
        //  Loading initial page
        webEngine.load(srcBrowser);
        
        getChildren().add(toolBar);
        getChildren().add(browser);

        //  Populating Links with stuff from service
        mbes.getMediaItems().forEach((mediaItem) -> {
            Hyperlink link = new Hyperlink();
            link.setText(mediaItem.getName());
            logger.log(Level.INFO,"Populating : {0} with link {1}",new Object[]{mediaItem.getName(),mediaItem.getLink()});
            link.setOnAction((ActionEvent e) -> {
                logger.log(Level.INFO,"should be redirecting to : {0}",mediaItem.getLink());
                webEngine.load(mediaItem.getLink());
            });
            options.add(link);
        });

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
