package racing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

/**
 * The player's collection of cars. A Driver owns exactly one Garage.
 * Buying a car ADDS it here - it never replaces another car.
 */
public class Garage {

    private final ArrayList<Car> cars = new ArrayList<Car>();
    private Car currentCar;

    public void addCar(Car car) {
        cars.add(car);
        if (currentCar == null) currentCar = car;
    }

    /** Removes every car (used when loading a saved game). */
    public void clear() {
        cars.clear();
        currentCar = null;
    }

    public ArrayList<Car> getCars() { return cars; }
    public int size() { return cars.size(); }
    public Car getCurrentCar() { return currentCar; }

    public void setCurrentCar(Car car) {
        if (cars.contains(car)) currentCar = car;
    }

    /** The garage must always keep at least one car so the player can still race. */
    public boolean canSell(Car car) {
        return cars.contains(car) && cars.size() > 1;
    }

    /**
     * Sells a car. If it was the current racing car, the garage safely
     * switches to another car first. Returns the money received, or -1 if the
     * sale is not allowed (the last car can't be sold).
     */
    public int sellCar(Driver driver, Car car) {
        if (!canSell(car)) return -1;
        int value = car.getSellValue();
        cars.remove(car);
        if (car == currentCar) currentCar = cars.get(0);
        driver.receiveMoney(value);
        return value;
    }

    /** Buys the next upgrade level for one category. Returns a message for the screen. */
    public String upgradeCar(Driver driver, Car car, int category) {
        if (car.isMaxed(category)) return Car.STAT_NAMES[category] + " is already at the maximum level!";
        int cost = car.getUpgradeCost(category);
        if (!driver.spend(cost)) return "Not enough money! You need $" + cost + ".";
        car.upgrade(category);
        return Car.STAT_NAMES[category] + " upgraded to level " + car.getUpgradeLevel(category) + "!";
    }

    // ------------------------------------------------------------------
    // Interior pixel art (drawn on a 240 x 160 grid that is scaled up 4x)
    // ------------------------------------------------------------------
    public static void paintInterior(Graphics2D g) {
        // brick wall
        GameWindow.fill(g, new Color(128, 74, 58), 0, 0, 240, 100);
        Color mortar = new Color(98, 58, 47);
        for (int row = 0; row < 100; row += 6) {
            GameWindow.fill(g, mortar, 0, row, 240, 1);
            int off = (row / 6 % 2 == 0) ? 0 : 8;
            for (int x = off; x < 240; x += 16) GameWindow.fill(g, mortar, x, row, 1, 6);
        }
        // concrete floor
        GameWindow.fill(g, new Color(96, 96, 106), 0, 100, 240, 60);
        Color seam = new Color(78, 78, 88);
        for (int x = 0; x < 240; x += 30) GameWindow.fill(g, seam, x, 100, 1, 60);
        GameWindow.fill(g, seam, 0, 118, 240, 1);
        GameWindow.fill(g, seam, 0, 138, 240, 1);
        GameWindow.fill(g, new Color(60, 60, 70), 0, 100, 240, 2);

        // roller door
        GameWindow.fill(g, new Color(50, 50, 58), 10, 18, 84, 82);
        GameWindow.fill(g, new Color(172, 176, 188), 12, 20, 80, 80);
        for (int y = 20; y < 100; y += 8) GameWindow.fill(g, new Color(140, 144, 156), 12, y, 80, 1);
        GameWindow.fill(g, new Color(110, 114, 126), 48, 94, 8, 3);

        // window
        GameWindow.fill(g, new Color(45, 30, 24), 112, 16, 44, 34);
        GameWindow.fill(g, new Color(140, 200, 240), 114, 18, 40, 30);
        GameWindow.fill(g, new Color(255, 255, 255), 118, 22, 10, 3);
        GameWindow.fill(g, new Color(45, 30, 24), 133, 18, 2, 30);
        GameWindow.fill(g, new Color(45, 30, 24), 114, 32, 40, 2);

        // shelves with boxes and cans
        GameWindow.fill(g, new Color(110, 78, 46), 168, 40, 62, 3);
        GameWindow.fill(g, new Color(110, 78, 46), 168, 66, 62, 3);
        GameWindow.fill(g, new Color(110, 78, 46), 168, 92, 62, 3);
        Color[] stuff = { new Color(200, 60, 50), new Color(60, 110, 200), new Color(230, 180, 40), new Color(90, 170, 90) };
        for (int i = 0; i < 6; i++) {
            GameWindow.fill(g, stuff[i % 4], 171 + i * 10, 31, 7, 9);
            GameWindow.fill(g, stuff[(i + 1) % 4], 171 + i * 10, 57, 7, 9);
            GameWindow.fill(g, stuff[(i + 2) % 4], 171 + i * 10, 83, 7, 9);
        }
        // tyre stack
        for (int i = 0; i < 3; i++) {
            GameWindow.fill(g, new Color(28, 28, 32), 112, 84 + i * 5, 26, 5);
            GameWindow.fill(g, new Color(70, 70, 76), 122, 85 + i * 5, 6, 3);
        }
        // oil drum
        GameWindow.fill(g, new Color(40, 90, 170), 144, 80, 16, 20);
        GameWindow.fill(g, new Color(70, 130, 220), 144, 84, 16, 2);
        GameWindow.fill(g, new Color(70, 130, 220), 144, 94, 16, 2);

        // parking bay lines on the floor
        Color yellow = new Color(235, 200, 50);
        GameWindow.fill(g, yellow, 20, 146, 200, 2);
        for (int x = 20; x <= 220; x += 50) GameWindow.fill(g, yellow, x, 124, 2, 24);

        // ceiling lamps with light
        for (int x = 40; x < 240; x += 80) {
            GameWindow.fill(g, new Color(40, 40, 46), x, 0, 24, 3);
            GameWindow.fill(g, new Color(255, 245, 190), x + 2, 3, 20, 2);
            g.setColor(new Color(255, 245, 190, 40));
            g.fillRect(x - 6, 5, 36, 20);
        }
    }
}
