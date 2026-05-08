package game.ui;

import game.logic.*;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GameRenderer {

    private static final double TILE_SIZE = GameConstants.TILE_SIZE;
    private final Map<String, Image> imageCache = new HashMap<>();

    private Image gridTexture;

    public GameRenderer() {
        preloadImages();
    }

    private void preloadImages() {
        // 1. Load all Machines from the "machine" folder
        for (MachineType type : MachineType.values()) {
            if (type == MachineType.NONE || type.getCategory() == null) continue;

            if (type.getFallBackText() == null) {
                imageCache.put(type.getImageName(), loadRequiredAsset(type.getImageName(), "machine"));
            } else {
                imageCache.put(type.getImageName(), getImage(type.getImageName(), type.getFallBackText(), "machine"));
            }
        }

        // 2. Load all Items from the "item" folder
        for (ItemType item : ItemType.values()) {
            String fallback = item.name().substring(0, Math.min(2, item.name().length()));
            fallback = fallback.substring(0, 1).toUpperCase() + fallback.substring(1).toLowerCase();

            imageCache.put(item.getImageName(), getImage(item.getImageName(), fallback, "item"));
        }

        gridTexture = getImage("grid.png", "Grid", "environment");
    }

    private Image loadRequiredAsset(String fileName, String subfolder) {
        String path = "/images/" + subfolder + "/" + fileName;
        InputStream stream = GameRenderer.class.getResourceAsStream(path);

        if (stream == null) {
            System.err.println("CRITICAL FAILURE: Missing required asset: " + path);
            System.exit(1);
        }

        return new Image(stream);
    }

    private Image getImage(String filename, String fallbackText, String subfolder) {
        String path = "/images/" + subfolder + "/" + filename;

        try (InputStream stream = GameRenderer.class.getResourceAsStream(path)) {
            if (stream != null) {
                Image loaded = new Image(stream);
                if (!loaded.isError()) return loaded;
            }
        } catch (Exception ignored) {}
        return createPlaceholderImage(fallbackText);
    }

    private Image createPlaceholderImage(String text) {
        int w = 50, h = 50;
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.WHITE); g.fillRect(0, 0, w, h);
        g.setStroke(Color.BLACK); g.strokeRect(0.5, 0.5, w - 1, h - 1);
        g.setFill(Color.BLACK); g.setFont(Font.font(11));
        g.fillText(text == null ? "?" : text, 6, 28);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snap = c.snapshot(params, null);
        return snap == null ? new WritableImage(w, h) : snap;
    }

    public Image imageForMachineType(MachineType s) {
        return imageCache.get(s.getImageName());
    }

    private double facingAngle(Direction d) {
        return switch (d) {
            case RIGHT -> 0; case DOWN -> 90; case LEFT -> 180; case UP -> 270;
        };
    }

    private void drawImageRotated(GraphicsContext gc, Image img, double x, double y, double w, double h, double degrees) {
        if (img == null) return;
        gc.save();
        gc.translate(x + w / 2.0, y + h / 2.0);
        gc.rotate(degrees);
        gc.drawImage(img, -w / 2.0, -h / 2.0, w, h);
        gc.restore();
    }

    public void render(Canvas gameCanvas, GridSystem logicGrid, ShopManager shopManager,
                       double mouseWorldX, double mouseWorldY, Direction placementFacing, boolean shopVisible) {

        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        double worldW = logicGrid.getWidth() * TILE_SIZE;
        double worldH = logicGrid.getHeight() * TILE_SIZE;

        // Draw Floor
        for (int x = 0; x < logicGrid.getWidth(); x++) {
            for (int y = 0; y < logicGrid.getHeight(); y++) {
                gc.drawImage(gridTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Draw Grid Lines
        gc.setStroke(Color.color(0.25, 0.28, 0.32));
        gc.setLineWidth(1);
        for (int gx = 0; gx <= logicGrid.getWidth(); gx++) gc.strokeLine(gx * TILE_SIZE, 0, gx * TILE_SIZE, worldH);
        for (int gy = 0; gy <= logicGrid.getHeight(); gy++) gc.strokeLine(0, gy * TILE_SIZE, worldW, gy * TILE_SIZE);

        // Draw Machines and Items
        for (int x = 0; x < logicGrid.getWidth(); x++) {
            for (int y = 0; y < logicGrid.getHeight(); y++) {
                Machine m = logicGrid.getMachine(x, y);
                if (m == null) continue;

                double px = x * TILE_SIZE, py = y * TILE_SIZE;
                drawImageRotated(gc, imageForMachineType(m.getType()), px, py, TILE_SIZE, TILE_SIZE, facingAngle(m.getFacing()));

                // --- REPLACE THIS IF STATEMENT ---
                if (m.getCurrentItem() != null) {
                    double inset = TILE_SIZE * 0.2;

                    // NEW: Dynamically fetch the specific item's image!
                    String exactImageName = m.getCurrentItem().getType().getImageName();
                    Image exactItemImg = imageCache.get(exactImageName);

                    drawImageRotated(gc, exactItemImg, px + inset * 0.5, py + inset * 0.5, TILE_SIZE - inset, TILE_SIZE - inset, 0);
                }
                // ---------------------------------
            }
        }

        // Draw Hologram
        renderHologram(gc, logicGrid, shopManager, mouseWorldX, mouseWorldY, placementFacing, shopVisible);
    }

    private void renderHologram(GraphicsContext gc, GridSystem logicGrid, ShopManager shopManager,
                                double mouseWorldX, double mouseWorldY, Direction placementFacing, boolean shopVisible) {
        if (shopVisible || shopManager.getActiveSelection() == MachineType.NONE) return;

        int gx = (int) Math.floor(mouseWorldX / TILE_SIZE);
        int gy = (int) Math.floor(mouseWorldY / TILE_SIZE);
        if (!logicGrid.isInside(gx, gy)) return;

        boolean valid = logicGrid.getMachine(gx, gy) == null && shopManager.getInventoryCount(shopManager.getActiveSelection()) > 0;
        double px = gx * TILE_SIZE, py = gy * TILE_SIZE;

        gc.save();
        gc.setGlobalAlpha(0.4);
        drawImageRotated(gc, imageForMachineType(shopManager.getActiveSelection()), px, py, TILE_SIZE, TILE_SIZE, facingAngle(placementFacing));
        gc.setFill(valid ? Color.web("#3498db", 0.4) : Color.web("#e74c3c", 0.5));
        gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
        gc.restore();
    }
}