package racing;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The paint shop. Paint is purely cosmetic: it never changes a car's speed,
 * acceleration or handling. The new colour stays on the car for the rest of
 * the current game.
 */
public class PaintShop {

    public static int getPaintCount() { return Car.PAINT_NAMES.length; }
    public static String getPaintName(int index) { return Car.PAINT_NAMES[index]; }
    public static int getPaintPrice(int index) { return Car.PAINT_PRICES[index]; }

    /** Paints the car if the driver can pay. Returns a message for the screen. */
    public static String paintCar(Driver driver, Car car, int paintIndex) {
        if (car.getPaintIndex() == paintIndex) {
            return "The " + car.getName() + " already has " + Car.PAINT_NAMES[paintIndex] + " paint.";
        }
        int cost = getPaintPrice(paintIndex);
        if (!driver.spend(cost)) return "Not enough money! That paint costs $" + cost + ".";
        car.setPaint(paintIndex);
        return "The " + car.getName() + " now has a fresh " + Car.PAINT_NAMES[paintIndex] + " paint job!";
    }

    // ------------------------------------------------------------------
    // Interior pixel art (240 x 160 grid)
    // ------------------------------------------------------------------
    public static void paintInterior(Graphics2D g) {
        // white walls
        GameWindow.fill(g, new Color(238, 236, 230), 0, 0, 240, 100);
        GameWindow.fill(g, new Color(212, 210, 204), 0, 90, 240, 10);

        // colourful paint splashes on the wall
        Color[] splash = { new Color(220, 50, 50), new Color(50, 110, 220), new Color(240, 190, 40),
                           new Color(60, 170, 90), new Color(160, 80, 200), new Color(240, 120, 40) };
        int[][] spots = { {14, 12, 18}, {60, 30, 14}, {100, 10, 20}, {150, 26, 16}, {196, 12, 22}, {30, 56, 12}, {120, 52, 14}, {210, 56, 12} };
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0], y = spots[i][1], s = spots[i][2];
            Color c = splash[i % splash.length];
            GameWindow.fill(g, c, x, y, s, s - 4);
            GameWindow.fill(g, c, x + 2, y - 2, s - 4, s);
            GameWindow.fill(g, c, x + s / 2 - 1, y + s - 4, 3, 8);      // drip
            GameWindow.fill(g, c.brighter(), x + 3, y + 1, 4, 3);
        }

        // floor with colour drips
        GameWindow.fill(g, new Color(110, 112, 122), 0, 100, 240, 60);
        GameWindow.fill(g, new Color(62, 64, 74), 0, 100, 240, 2);
        for (int i = 0; i < 22; i++) {
            int x = (i * 37) % 230 + 4;
            int y = 108 + (i * 23) % 46;
            GameWindow.fill(g, splash[i % splash.length], x, y, 5 + i % 4, 3);
        }

        // spray booth (blue masking panel with window)
        GameWindow.fill(g, new Color(40, 70, 130), 150, 20, 80, 82);
        GameWindow.fill(g, new Color(70, 110, 190), 154, 24, 72, 74);
        GameWindow.fill(g, new Color(190, 225, 250), 160, 30, 60, 36);
        GameWindow.fill(g, Color.WHITE, 164, 34, 14, 3);
        GameWindow.fill(g, new Color(40, 70, 130), 189, 30, 2, 36);
        GameWindow.fill(g, new Color(250, 200, 40), 154, 74, 72, 4);

        // paint cans on a shelf
        GameWindow.fill(g, new Color(120, 84, 50), 10, 76, 120, 4);
        for (int i = 0; i < 9; i++) {
            Color c = splash[i % splash.length];
            GameWindow.fill(g, c, 14 + i * 13, 62, 10, 14);
            GameWindow.fill(g, new Color(200, 200, 210), 14 + i * 13, 62, 10, 2);
            GameWindow.fill(g, Color.WHITE, 16 + i * 13, 67, 6, 4);
        }
        // big paint bucket stack
        GameWindow.fill(g, new Color(200, 60, 50), 14, 104, 22, 22);
        GameWindow.fill(g, new Color(60, 110, 200), 40, 108, 22, 18);
        GameWindow.fill(g, new Color(240, 190, 40), 27, 88, 22, 16);
        GameWindow.fill(g, new Color(210, 210, 220), 14, 104, 22, 3);
        GameWindow.fill(g, new Color(210, 210, 220), 40, 108, 22, 3);
        GameWindow.fill(g, new Color(210, 210, 220), 27, 88, 22, 3);

        // lamps
        for (int x = 24; x < 240; x += 80) {
            GameWindow.fill(g, new Color(40, 40, 46), x, 0, 26, 3);
            GameWindow.fill(g, new Color(255, 250, 215), x + 2, 3, 22, 2);
            g.setColor(new Color(255, 250, 215, 36));
            g.fillRect(x - 6, 5, 38, 22);
        }
    }
}
