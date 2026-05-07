package game.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class CameraManager {
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 2.0;

    private double x = 0.0;
    private double y = 0.0;
    private double zoom = 1.0;

    public void pan(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public void applyZoom(double factor) {
        zoom *= factor;
        zoom = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, zoom));
    }

    public void applyTransformsAndClamp(Canvas canvas, double worldWidthPx, double worldHeightPx) {
        double visualW = worldWidthPx * zoom;
        double visualH = worldHeightPx * zoom;

        // Clamp camera to screen bounds
        x = Math.max(-visualW + 200, Math.min(800, x));
        y = Math.max(-visualH + 200, Math.min(600, y));

        canvas.getTransforms().clear();
        canvas.getTransforms().addAll(
                new Scale(zoom, zoom, 0, 0),
                new Translate(x, y)
        );
    }

    // Translates Mouse coordinates to Grid World coordinates
    public double screenToWorldX(double localX) { return (localX - x) / zoom; }
    public double screenToWorldY(double localY) { return (localY - y) / zoom; }
}