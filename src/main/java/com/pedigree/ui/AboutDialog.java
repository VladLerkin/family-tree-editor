package com.pedigree.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutDialog {

    private static final String APP_NAME = "Family Tree Editor";
    private static final String APP_VERSION = "v1.2.7";
    private static final String AUTHOR_EMAIL = "domfindus@gmail.com";

    public static void show() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About " + APP_NAME);
        alert.setHeaderText(null);
        
        // Create custom content with formatted text and hyperlink
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER_LEFT);
        
        // App name (bold, larger font)
        Label nameLabel = new Label(APP_NAME);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        // Version
        Label versionLabel = new Label("Version " + APP_VERSION);
        versionLabel.setFont(Font.font("System", 12));
        
        // Free software statement
        Label freeLabel = new Label("This program is free software.");
        freeLabel.setFont(Font.font("System", 12));
        
        // Author and contact info with hyperlink
        TextFlow contactFlow = new TextFlow();
        Text authorText = new Text("Author: ");
        authorText.setFont(Font.font("System", 12));
        
        Hyperlink emailLink = new Hyperlink(AUTHOR_EMAIL);
        emailLink.setFont(Font.font("System", 12));
        emailLink.setOnAction(e -> openEmail(AUTHOR_EMAIL));
        emailLink.setPadding(new Insets(0));
        emailLink.setStyle("-fx-border-color: transparent;");
        
        contactFlow.getChildren().addAll(authorText, emailLink);
        
        // Feedback notice
        Label feedbackLabel = new Label("Please send all comments and feedback to the email above.");
        feedbackLabel.setFont(Font.font("System", 12));
        feedbackLabel.setWrapText(true);
        feedbackLabel.setMaxWidth(400);
        
        content.getChildren().addAll(nameLabel, versionLabel, freeLabel, contactFlow, feedbackLabel);
        
        alert.getDialogPane().setContent(content);
        alert.getButtonTypes().setAll(ButtonType.OK);
        
        alert.showAndWait();
    }
    
    private static void openEmail(String email) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    desktop.mail(new URI("mailto:" + email));
                }
            }
        } catch (IOException | URISyntaxException ex) {
            // Silently fail if email client cannot be opened
            System.err.println("Could not open email client: " + ex.getMessage());
        }
    }
}
