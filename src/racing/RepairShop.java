package racing;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The mechanic. Repairing always brings a car back to 100% condition and
 * costs money. The price formula lives in Car.getRepairCost(): half of the
 * car's value, scaled by how damaged it is - so the same rule works for
 * every car, old or new.
 */
public class RepairShop {

    public static boolean needsRepair(Car car) {
        return car.getCondition() < 100;
    }

    public static int getRepairCost(Car car) {
        return car.getRepairCost();
    }

    /** Repairs the car if the driver can pay. Returns a message for the screen. */
    public static String repairCar(Driver driver, Car car) {
        if (!needsRepair(car)) return "The " + car.getName() + " is already in perfect condition.";
        int cost = getRepairCost(car);
        if (!driver.spend(cost)) return "Not enough money! The repair costs $" + cost + ".";
        car.repair();
        driver.recordRepair();
        return "The " + car.getName() + " is fully repaired (100%)!";
    }

    // ------------------------------------------------------------------
    // Interior pixel art (240 x 160 grid)
    // ------------------------------------------------------------------
    public static void paintInterior(Graphics2D g) {
        // walls
        GameWindow.fill(g, new Color(86, 100, 120), 0, 0, 240, 100);
        GameWindow.fill(g, new Color(70, 82, 100), 0, 70, 240, 30);
        // pegboard with tools
        GameWindow.fill(g, new Color(190, 160, 110), 10, 10, 100, 50);
        for (int y = 14; y < 58; y += 6) {
            for (int x = 14; x < 108; x += 6) GameWindow.fill(g, new Color(150, 120, 80), x, y, 1, 1);
        }
        // wrenches
        for (int i = 0; i < 6; i++) {
            GameWindow.fill(g, new Color(190, 195, 205), 16 + i * 14, 16, 3, 22 + (i % 3) * 4);
            GameWindow.fill(g, new Color(150, 155, 165), 14 + i * 14, 14, 7, 4);
        }
        // hammer + screwdrivers
        GameWindow.fill(g, new Color(150, 100, 50), 24, 44, 28, 3);
        GameWindow.fill(g, new Color(120, 125, 135), 20, 41, 8, 9);
        GameWindow.fill(g, new Color(220, 60, 50), 64, 44, 24, 3);
        GameWindow.fill(g, new Color(60, 120, 220), 64, 50, 24, 3);

        // clock/warning sign
        GameWindow.fill(g, new Color(240, 200, 40), 140, 14, 36, 26);
        GameWindow.fill(g, new Color(30, 30, 34), 142, 16, 32, 22);
        GameWindow.fill(g, new Color(240, 200, 40), 148, 20, 4, 10);
        GameWindow.fill(g, new Color(240, 200, 40), 148, 32, 4, 3);
        GameWindow.fill(g, new Color(240, 200, 40), 158, 20, 12, 3);
        GameWindow.fill(g, new Color(240, 200, 40), 158, 26, 12, 3);
        GameWindow.fill(g, new Color(240, 200, 40), 158, 32, 12, 3);

        // concrete floor
        GameWindow.fill(g, new Color(100, 102, 112), 0, 100, 240, 60);
        Color seam = new Color(82, 84, 94);
        for (int x = 0; x < 240; x += 40) GameWindow.fill(g, seam, x, 100, 1, 60);
        GameWindow.fill(g, seam, 0, 124, 240, 1);
        GameWindow.fill(g, seam, 0, 146, 240, 1);
        GameWindow.fill(g, new Color(60, 62, 72), 0, 100, 240, 2);

        // car lift (two posts and a platform)
        GameWindow.fill(g, new Color(230, 160, 30), 150, 50, 8, 62);
        GameWindow.fill(g, new Color(230, 160, 30), 218, 50, 8, 62);
        GameWindow.fill(g, new Color(90, 94, 106), 148, 108, 12, 6);
        GameWindow.fill(g, new Color(90, 94, 106), 216, 108, 12, 6);
        GameWindow.fill(g, new Color(200, 200, 210), 150, 70, 76, 4);
        GameWindow.fill(g, new Color(140, 140, 150), 150, 74, 76, 2);
        // warning stripes on floor
        for (int x = 140; x < 236; x += 8) {
            GameWindow.fill(g, new Color(240, 200, 40), x, 150, 4, 4);
            GameWindow.fill(g, new Color(30, 30, 34), x + 4, 150, 4, 4);
        }

        // red tool chest
        GameWindow.fill(g, new Color(180, 40, 40), 12, 74, 44, 38);
        GameWindow.fill(g, new Color(220, 70, 60), 12, 74, 44, 3);
        for (int i = 0; i < 3; i++) {
            GameWindow.fill(g, new Color(140, 28, 30), 16, 80 + i * 10, 36, 8);
            GameWindow.fill(g, new Color(200, 200, 210), 30, 83 + i * 10, 8, 2);
        }
        // tyres
        for (int i = 0; i < 4; i++) {
            GameWindow.fill(g, new Color(26, 26, 30), 68, 88 + i * 6, 28, 6);
            GameWindow.fill(g, new Color(76, 76, 82), 78, 89 + i * 6, 8, 4);
        }
        // oil barrels
        GameWindow.fill(g, new Color(50, 50, 56), 106, 84, 18, 26);
        GameWindow.fill(g, new Color(80, 80, 90), 106, 88, 18, 2);
        GameWindow.fill(g, new Color(80, 80, 90), 106, 100, 18, 2);
        GameWindow.fill(g, new Color(200, 50, 40), 126, 90, 16, 20);
        GameWindow.fill(g, new Color(240, 90, 70), 126, 94, 16, 2);

        // lamps
        for (int x = 30; x < 240; x += 90) {
            GameWindow.fill(g, new Color(40, 40, 46), x, 0, 26, 3);
            GameWindow.fill(g, new Color(255, 245, 200), x + 2, 3, 22, 2);
            g.setColor(new Color(255, 245, 200, 36));
            g.fillRect(x - 6, 5, 38, 22);
        }
    }
}
