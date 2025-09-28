package com.pedigree.render;

public interface GraphicsContext {
    void setStrokeColor(int r, int g, int b, double a);
    void setFillColor(int r, int g, int b, double a);
    void setLineWidth(double w);
    void setFontSize(double size);
    void drawLine(double x1, double y1, double x2, double y2);
    void fillRect(double x, double y, double w, double h);
    void drawRect(double x, double y, double w, double h);
    void drawText(String text, double x, double y);
}
