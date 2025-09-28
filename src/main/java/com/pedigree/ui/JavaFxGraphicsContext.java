package com.pedigree.ui;

import com.pedigree.render.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class JavaFxGraphicsContext implements GraphicsContext {

    private final javafx.scene.canvas.GraphicsContext gc;

    public JavaFxGraphicsContext(javafx.scene.canvas.GraphicsContext gc) {
        this.gc = gc;
    }

    @Override
    public void setStrokeColor(int r, int g, int b, double a) {
        gc.setStroke(Color.rgb(r, g, b, a));
    }

    @Override
    public void setFillColor(int r, int g, int b, double a) {
        gc.setFill(Color.rgb(r, g, b, a));
    }

    @Override
    public void setLineWidth(double w) {
        gc.setLineWidth(w);
    }

    @Override
    public void setFontSize(double size) {
        try {
            gc.setFont(Font.font(size));
        } catch (Throwable ignore) {
            // Fallback: ignore if font cannot be set in current context
        }
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        gc.fillRect(x, y, w, h);
    }

    @Override
    public void drawRect(double x, double y, double w, double h) {
        gc.strokeRect(x, y, w, h);
    }

    @Override
    public void drawText(String text, double x, double y) {
        gc.fillText(text, x, y);
    }
}
