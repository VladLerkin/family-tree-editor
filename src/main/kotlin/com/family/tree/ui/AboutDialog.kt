package com.family.tree.ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Kotlin implementation of the About dialog.
 * Kept API-compatible: Java can call AboutDialog.show() thanks to @JvmStatic.
 */
object AboutDialog {
    private const val APP_NAME: String = "Family Tree Editor"
    private const val APP_VERSION: String = "v1.2.25"
    private const val AUTHOR_EMAIL: String = "domfindus@gmail.com"

    @JvmStatic
    fun show() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "About $APP_NAME"
        alert.headerText = null

        val content = VBox(10.0).apply {
            padding = Insets(10.0)
            alignment = Pos.CENTER_LEFT
        }

        val nameLabel = Label(APP_NAME).apply {
            font = Font.font("System", FontWeight.BOLD, 16.0)
        }

        val versionLabel = Label("Version $APP_VERSION").apply {
            font = Font.font("System", 12.0)
        }

        val freeLabel = Label("This program is free software.").apply {
            font = Font.font("System", 12.0)
        }

        val contactFlow = TextFlow().apply {
            val authorText = Text("Author: ").apply {
                font = Font.font("System", 12.0)
            }
            val emailLink = Hyperlink(AUTHOR_EMAIL).apply {
                font = Font.font("System", 12.0)
                setOnAction { openEmail(AUTHOR_EMAIL) }
                padding = Insets(0.0)
                style = "-fx-border-color: transparent;"
            }
            children.addAll(authorText, emailLink)
        }

        val feedbackLabel = Label("Please send all comments and feedback to the email above.").apply {
            font = Font.font("System", 12.0)
            isWrapText = true
            maxWidth = 400.0
        }

        content.children.addAll(nameLabel, versionLabel, freeLabel, contactFlow, feedbackLabel)

        alert.dialogPane.content = content
        alert.buttonTypes.setAll(ButtonType.OK)

        alert.showAndWait()
    }

    private fun openEmail(email: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    desktop.mail(URI("mailto:$email"))
                }
            }
        } catch (ex: IOException) {
            System.err.println("Could not open email client: ${ex.message}")
        } catch (ex: URISyntaxException) {
            System.err.println("Could not open email client: ${ex.message}")
        }
    }
}
