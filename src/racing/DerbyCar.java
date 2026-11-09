package racing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * The beat-up cars you can buy for ONE demolition derby.
 * They are separate from the cars in your garage: you pay, you smash, the car is scrapped.
 */
final class DerbyCar {

    final String name;
    final int price;
    final double hp;        // how much damage it can take
    final double top;       // top speed (pixels per second in the arena)
    final double accel;     // how fast it speeds up
    final double turn;      // how sharply it steers (radians per second)
    final double mass;      // heavier cars hit harder and get pushed less
    final Color color;
    final String blurb;
    private BufferedImage side, topImg;

    private static ArrayList<DerbyCar> catalog;

    private DerbyCar(String name, int price, double hp, double top, double accel, double turn, double mass, Color color, String blurb) {
        this.name = name;
        this.price = price;
        this.hp = hp * 1.5;
        this.top = top;
        this.accel = accel;
        this.turn = turn;
        this.mass = mass;
        this.color = color;
        this.blurb = blurb;
    }

    static ArrayList<DerbyCar> catalog() {
        if (catalog == null) {
            catalog = new ArrayList<DerbyCar>();
            catalog.add(new DerbyCar("Rusty Wagon", 150, 60, 232, 215, 2.7, 1.0, new Color(120, 150, 90), "Cheap and cheerful. Dodge a lot!"));
            catalog.add(new DerbyCar("Beater Sedan", 250, 70, 238, 225, 2.8, 1.0, new Color(165, 92, 70), "A tired old sedan that still turns well."));
            catalog.add(new DerbyCar("Pickup Brute", 500, 95, 245, 230, 2.5, 1.4, new Color(205, 172, 60), "A bull bar and a lot of attitude."));
            catalog.add(new DerbyCar("Muscle Mauler", 800, 80, 290, 290, 2.6, 1.2, new Color(70, 112, 185), "Fast and angry. Great for fast hits."));
            catalog.add(new DerbyCar("Delivery Van", 1000, 125, 218, 190, 2.2, 1.8, new Color(235, 235, 235), "Slow but built like a safe."));
            catalog.add(new DerbyCar("School Bus Smasher", 1400, 170, 205, 170, 1.9, 2.4, new Color(240, 190, 30), "A huge yellow wall that nobody can push."));
            catalog.add(new DerbyCar("Armored Limo", 1800, 145, 255, 215, 2.1, 2.0, new Color(40, 40, 48), "Long, heavy and very hard to kill."));
            catalog.add(new DerbyCar("Monster Truck", 2500, 200, 262, 245, 2.3, 2.8, new Color(212, 52, 42), "The king of the derby. Crush them all!"));
        }
        return catalog;
    }

    /** The prize money for this car if you place 1st, 2nd or 3rd. */
    int prize(int place) {
        if (place == 1) return 500 + price * 2;
        if (place == 2) return price;
        if (place == 3) return price / 2;
        return 0;
    }

    BufferedImage sidePicture() {
        if (side == null) side = CarArt.side(name, 0, color);
        return side;
    }

    BufferedImage topPicture() {
        if (topImg == null) topImg = CarArt.top(name, 0, color);
        return topImg;
    }

    BufferedImage topPicture(Color c) { return CarArt.top(name, 0, c); }

    // stat bars from 0 to 1 (for the shop)
    double armorBar() { return Math.min(1.0, hp / 300.0); }
    double speedBar() { return Math.min(1.0, (top - 190) / 110.0); }
    double powerBar() { return Math.min(1.0, (accel - 150) / 150.0); }
    double weightBar() { return Math.min(1.0, mass / 2.8); }
    double handlingBar() { return Math.min(1.0, (turn - 1.6) / 1.4); }
}
