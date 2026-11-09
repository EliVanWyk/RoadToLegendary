package racing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

/**
 * The dealership. Holds the data for every car in the game:
 *  - 15 dealership cars (3 per rank)
 *  - 8 special retro cars that are only won as race rewards
 * All names, prices and stats are in this one file so they are easy to balance.
 */
public class CarShop {

    private static ArrayList<Car> catalog;
    private static ArrayList<Car> retroCars;

    /** The 15 cars that can be bought, ordered by rank. */
    public static ArrayList<Car> getCatalog() {
        if (catalog == null) {
            catalog = new ArrayList<Car>();
            //                    name                  price  spd acc hdl rank color                      style retro
            // ---- Beginner ----
            add("Toyota Corolla",       500,  45, 40, 50, 0, new Color(196, 200, 210), 0);
            add("Nissan Sentra",        600,  47, 44, 48, 0, new Color(150, 62, 62), 0);
            add("Honda Civic",          750,  50, 48, 54, 0, new Color(60, 110, 190), 1);
            // ---- Amateur ----
            add("Mazda MX-5",          1500,  60, 56, 72, 1, new Color(200, 40, 40), 2);
            add("Volkswagen GTI",      1700,  64, 60, 62, 1, new Color(235, 235, 240), 1);
            add("Subaru BRZ",          2000,  66, 62, 68, 1, new Color(240, 150, 30), 2);
            // ---- Professional ----
            add("Ford Mustang",        3500,  78, 72, 60, 2, new Color(30, 60, 170), 4);
            add("BMW M3",              4200,  80, 76, 72, 2, new Color(50, 50, 58), 0);
            add("Audi S5",             4800,  76, 74, 70, 2, new Color(125, 125, 132), 2);
            // ---- Expert ----
            add("Porsche 911",         7000,  88, 84, 84, 3, new Color(230, 200, 60), 2);
            add("Chevrolet Corvette C8", 8000, 92, 88, 80, 3, new Color(210, 50, 40), 3);
            add("Nissan GT-R",         9000,  90, 90, 86, 3, new Color(232, 232, 238), 2);
            // ---- Legendary ----
            add("Lamborghini Huracan", 14000, 100, 96, 88, 4, new Color(140, 210, 50), 3);
            add("Ferrari 488",         16000,  98, 98, 92, 4, new Color(200, 20, 20), 3);
            add("McLaren 720S",        18000, 102, 100, 95, 4, new Color(235, 120, 20), 3);
        }
        return catalog;
    }

    private static void add(String name, int price, int spd, int acc, int hdl, int rank, Color c, int style) {
        catalog.add(new Car(name, price, spd, acc, hdl, rank, c, style, false));
    }

    /** The 8 retro cars won as special rewards (index 0..7). */
    public static ArrayList<Car> getRetroCars() {
        if (retroCars == null) {
            retroCars = new ArrayList<Car>();
            retro("1970 Ford Mustang",     3000, 66, 62, 48, new Color(220, 120, 30), 4);
            retro("1986 Toyota AE86",      2200, 58, 55, 78, new Color(235, 235, 235), 1);
            retro("1969 Chevrolet Camaro", 3800, 72, 70, 52, new Color(40, 120, 90), 4);
            retro("1964 Jaguar E-Type",    5200, 74, 66, 64, new Color(30, 80, 60), 2);
            retro("1965 Shelby Cobra",     8500, 84, 82, 58, new Color(40, 90, 200), 2);
            retro("1973 Porsche 911 RS",   9500, 82, 76, 78, new Color(240, 240, 240), 2);
            retro("1963 Corvette Stingray", 10500, 86, 78, 66, new Color(170, 30, 40), 2);
            retro("1991 Acura NSX",       13000, 92, 84, 88, new Color(220, 220, 30), 3);
        }
        return retroCars;
    }

    private static void retro(String name, int price, int spd, int acc, int hdl, Color c, int style) {
        retroCars.add(new Car(name, price, spd, acc, hdl, 0, c, style, true));
    }

    /** A rewarded retro car arrives in poor condition (20% - 45%). */
    public static Car createBrokenRetroCar(int index, java.util.Random rng) {
        Car c = getRetroCars().get(index).copy();
        c.setCondition(20 + rng.nextInt(26));
        return c;
    }

    /** Rebuilds a car from the line written by Car.serialize(). Returns null if the car is unknown. */
    public static Car deserialize(String line) {
        try {
            String[] p = line.split("\\|");
            Car template = null;
            for (Car c : getCatalog()) if (c.getName().equals(p[0])) template = c;
            if (template == null) for (Car c : getRetroCars()) if (c.getName().equals(p[0])) template = c;
            if (template == null) return null;
            Car car = template.copy();
            String[] lv = p[2].split(",");
            car.restoreState(Integer.parseInt(p[1]),
                    new int[] { Integer.parseInt(lv[0]), Integer.parseInt(lv[1]), Integer.parseInt(lv[2]) },
                    Integer.parseInt(p[3]), Integer.parseInt(p[4]));
            return car;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The starter car every new driver receives: Toyota Corolla. */
    public static Car createStarterCar() {
        return getCatalog().get(0).copy();
    }

    // ------------------------------------------------------------------
    // Buying
    // ------------------------------------------------------------------
    public static boolean isRankUnlocked(Driver driver, Car template) {
        return driver.getRank() >= template.getRankRequired();
    }

    /** How many copies of this model the driver already owns. */
    public static int countOwned(Driver driver, Car template) {
        int n = 0;
        for (Car c : driver.getGarage().getCars()) {
            if (c.getName().equals(template.getName())) n++;
        }
        return n;
    }

    /** Tries to buy a car. Returns a message to show the player. */
    public static String buyCar(Driver driver, Car template) {
        if (!isRankUnlocked(driver, template)) {
            return "You need the " + Career.RANK_NAMES[template.getRankRequired()] + " rank to buy this car.";
        }
        if (!driver.spend(template.getPrice())) {
            return "Not enough money! You need $" + (template.getPrice() - driver.getMoney()) + " more.";
        }
        Car bought = template.copy();
        driver.getGarage().addCar(bought);
        driver.recordPurchase();
        return "You bought the " + bought.getName() + "! It is waiting in your garage.";
    }

    // ------------------------------------------------------------------
    // Interior pixel art (240 x 160 grid)
    // ------------------------------------------------------------------
    public static void paintInterior(Graphics2D g) {
        // bright showroom wall
        GameWindow.fill(g, new Color(232, 236, 244), 0, 0, 240, 104);
        GameWindow.fill(g, new Color(205, 212, 226), 0, 96, 240, 8);
        // big glass windows showing the sky
        for (int i = 0; i < 3; i++) {
            int x = 10 + i * 76;
            GameWindow.fill(g, new Color(70, 80, 110), x, 12, 68, 66);
            GameWindow.fill(g, new Color(130, 195, 240), x + 3, 15, 62, 60);
            GameWindow.fill(g, new Color(180, 220, 250), x + 3, 15, 62, 14);
            GameWindow.fill(g, Color.WHITE, x + 12, 40, 18, 5);
            GameWindow.fill(g, Color.WHITE, x + 36, 52, 20, 5);
            GameWindow.fill(g, new Color(70, 80, 110), x + 33, 15, 2, 60);
        }
        // checker floor
        for (int y = 104; y < 160; y += 8) {
            for (int x = 0; x < 240; x += 8) {
                boolean light = ((x / 8) + (y / 8)) % 2 == 0;
                GameWindow.fill(g, light ? new Color(238, 238, 244) : new Color(150, 156, 176), x, y, 8, 8);
            }
        }
        // display platform
        GameWindow.fill(g, new Color(60, 60, 80), 36, 128, 168, 12);
        GameWindow.fill(g, new Color(200, 40, 50), 40, 124, 160, 6);
        GameWindow.fill(g, new Color(240, 90, 90), 40, 124, 160, 2);
        // spot lights
        for (int x = 40; x < 240; x += 70) {
            GameWindow.fill(g, new Color(50, 50, 60), x, 0, 16, 4);
            g.setColor(new Color(255, 250, 200, 50));
            g.fillRect(x - 10, 4, 36, 18);
        }
        // banners
        GameWindow.fill(g, new Color(40, 80, 190), 4, 80, 6, 18);
        GameWindow.fill(g, new Color(230, 190, 40), 4, 80, 6, 3);
        GameWindow.fill(g, new Color(40, 80, 190), 230, 80, 6, 18);
        GameWindow.fill(g, new Color(230, 190, 40), 230, 80, 6, 3);
    }
}
