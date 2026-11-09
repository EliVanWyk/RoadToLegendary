package racing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * One car. Stores its stats, condition, paint and upgrades, and knows how to
 * draw its two pixel-art pictures (big side view + small top-down racing view).
 *
 * All the pictures are drawn with code (no image files needed), so the project
 * works straight out of Eclipse without an assets folder.
 */
public class Car {

    // ---- constants used all over the game (easy to tweak for balance) ----
    public static final int SPEED = 0;
    public static final int ACCELERATION = 1;
    public static final int HANDLING = 2;
    public static final String[] STAT_NAMES = { "Speed", "Acceleration", "Handling" };

    public static final int MAX_UPGRADE_LEVEL = 5;      // 5 levels per category
    public static final int POINTS_PER_UPGRADE = 4;     // each level adds +4 to the stat

    public static final String[] PAINT_NAMES = { "Factory", "Red", "Blue", "Black", "White", "Gold", "Metallic" };
    public static final int[] PAINT_PRICES = { 0, 50, 50, 75, 75, 150, 200 };
    public static final Color[] PAINT_COLORS = {
        null,                       // Factory = the car's own colour
        new Color(210, 40, 40),
        new Color(40, 90, 215),
        new Color(38, 38, 44),
        new Color(240, 240, 240),
        new Color(235, 190, 40),
        new Color(165, 175, 190)
    };

    // ---- data that never changes for a car ----
    private final String name;
    private final int price;
    private final int baseSpeed;
    private final int baseAcceleration;
    private final int baseHandling;
    private final int rankRequired;
    private final Color factoryColor;
    private final int bodyStyle;      // 0 sedan, 1 hatch, 2 coupe, 3 supercar, 4 muscle
    private final boolean retro;

    // ---- data that changes while playing ----
    private int condition = 100;
    private int paintIndex = 0;
    private final int[] upgradeLevels = new int[3];
    private int upgradeSpending = 0;

    // ---- cached pictures ----
    private BufferedImage largeImage;
    private BufferedImage raceImage;
    private int imagePaint = -1;

    public Car(String name, int price, int speed, int acceleration, int handling,
               int rankRequired, Color factoryColor, int bodyStyle, boolean retro) {
        this.name = name;
        this.price = price;
        this.baseSpeed = speed;
        this.baseAcceleration = acceleration;
        this.baseHandling = handling;
        this.rankRequired = rankRequired;
        this.factoryColor = factoryColor;
        this.bodyStyle = bodyStyle;
        this.retro = retro;
    }

    /** A brand-new copy of this car (used when buying from the shop). */
    public Car copy() {
        Car c = new Car(name, price, baseSpeed, baseAcceleration, baseHandling,
                rankRequired, factoryColor, bodyStyle, retro);
        c.condition = condition;
        return c;
    }

    // ------------------------------------------------------------------
    // Basic getters
    // ------------------------------------------------------------------
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getRankRequired() { return rankRequired; }
    public boolean isRetro() { return retro; }
    public int getCondition() { return condition; }
    public int getPaintIndex() { return paintIndex; }
    public String getPaintName() { return PAINT_NAMES[paintIndex]; }
    public int getUpgradeLevel(int category) { return upgradeLevels[category]; }
    public int getUpgradeSpending() { return upgradeSpending; }

    public void setCondition(int c) { condition = Math.max(0, Math.min(100, c)); }

    /** One line of text that describes everything about this car that changes (used by the save file). */
    public String serialize() {
        return name.replace('|', '/') + "|" + paintIndex + "|" + upgradeLevels[0] + "," + upgradeLevels[1] + "," + upgradeLevels[2]
                + "|" + upgradeSpending + "|" + condition;
    }

    /** Puts saved paint, upgrades and condition back onto a car. */
    public void restoreState(int paint, int[] levels, int spending, int cond) {
        paintIndex = Math.max(0, Math.min(PAINT_NAMES.length - 1, paint));
        for (int i = 0; i < 3; i++) upgradeLevels[i] = Math.max(0, Math.min(MAX_UPGRADE_LEVEL, levels[i]));
        upgradeSpending = Math.max(0, spending);
        condition = Math.max(0, Math.min(100, cond));
        imagePaint = -1;
    }

    /** Used for opponents: give a car all three upgrade levels at once. */
    public void setUpgradeLevels(int level) {
        for (int i = 0; i < 3; i++) upgradeLevels[i] = Math.max(0, Math.min(MAX_UPGRADE_LEVEL, level));
    }

    // ------------------------------------------------------------------
    // Stats (base + upgrades)
    // ------------------------------------------------------------------
    public int getBaseStat(int category) {
        if (category == SPEED) return baseSpeed;
        if (category == ACCELERATION) return baseAcceleration;
        return baseHandling;
    }

    public int getStat(int category) {
        return getBaseStat(category) + upgradeLevels[category] * POINTS_PER_UPGRADE;
    }

    public int getSpeed() { return getStat(SPEED); }
    public int getAcceleration() { return getStat(ACCELERATION); }
    public int getHandling() { return getStat(HANDLING); }

    /** Damaged cars perform a little worse (never worse than 80% of normal). */
    public double getConditionFactor() {
        return 0.80 + 0.20 * condition / 100.0;
    }

    /** One number summarising the car (shown in menus). */
    public int getOverallRating() {
        return (int) Math.round((getSpeed() + getAcceleration() + getHandling()) / 3.0 * getConditionFactor());
    }

    // ---- converted into race numbers (pixels per second) ----
    public double getTopSpeedPx() { return (120 + 2.5 * getSpeed()) * getConditionFactor(); }
    public double getAccelerationPx() { return (40 + 1.5 * getAcceleration()) * getConditionFactor(); }
    public double getSteeringPx() { return 120 + 2.2 * getHandling(); }

    // ------------------------------------------------------------------
    // Upgrades
    // ------------------------------------------------------------------
    /** Cost of the NEXT upgrade level, or -1 if this category is maxed out. */
    public int getUpgradeCost(int category) {
        int level = upgradeLevels[category];
        if (level >= MAX_UPGRADE_LEVEL) return -1;
        int unit = Math.max(50, price / 10);
        return unit * (level + 1);
    }

    public boolean isMaxed(int category) { return upgradeLevels[category] >= MAX_UPGRADE_LEVEL; }

    /** Applies the upgrade (the caller takes the money). */
    public void upgrade(int category) {
        int cost = getUpgradeCost(category);
        if (cost < 0) return;
        upgradeSpending += cost;
        upgradeLevels[category]++;
    }

    // ------------------------------------------------------------------
    // Repairs / damage
    // ------------------------------------------------------------------
    /** Repair price = half of the car's value, scaled by how damaged it is. */
    public int getRepairCost() {
        return (int) Math.round(price * (100 - condition) / 100.0 * 0.5);
    }

    public void repair() { condition = 100; }

    public void damage(int percent) { setCondition(condition - percent); }

    // ------------------------------------------------------------------
    // Paint
    // ------------------------------------------------------------------
    public void setPaint(int index) {
        paintIndex = index;
        imagePaint = -1;   // force the pictures to be redrawn
    }

    public Color getBodyColor(int paint) {
        return paint == 0 ? factoryColor : PAINT_COLORS[paint];
    }

    // ------------------------------------------------------------------
    // Selling
    // ------------------------------------------------------------------
    public int getSellValue() {
        double base = price * 0.5 * (0.4 + 0.6 * condition / 100.0);
        return (int) Math.round(base + upgradeSpending * 0.5);
    }

    // ------------------------------------------------------------------
    // Pictures
    // ------------------------------------------------------------------
    public BufferedImage getLargeImage() {
        refreshImages();
        return largeImage;
    }

    public BufferedImage getRaceImage() {
        refreshImages();
        return raceImage;
    }

    /** Large picture in any paint (used for the Paint Shop preview). */
    public BufferedImage getLargeImage(int paint) {
        if (paint == paintIndex) return getLargeImage();
        return CarArt.side(name, bodyStyle, getBodyColor(paint));
    }

    private void refreshImages() {
        if (imagePaint == paintIndex && largeImage != null) return;
        Color body = getBodyColor(paintIndex);
        largeImage = CarArt.side(name, bodyStyle, body);
        raceImage = CarArt.top(name, bodyStyle, body);
        imagePaint = paintIndex;
    }

    @Override
    public String toString() {
        return name + " (" + getPaintName() + ", " + condition + "%)";
    }
}
