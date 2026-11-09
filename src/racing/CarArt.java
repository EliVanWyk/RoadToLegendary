package racing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Draws the pictures of every car, pixel by pixel, from a small "design sheet" per car.
 * Each car has its own proportions (long hood, short deck, tall roof...) and features
 * (spoiler, pop-up headlights, stripes, bull bar...), so a Corvette does not look like a Civic.
 *
 *  - side(): the big side-view picture used in shops and menus (96 x 40 pixels)
 *  - top():  the small top-down picture used in races (22 x 40 pixels, front is UP)
 */
final class CarArt {

    // ---- feature flags ----
    static final int F_SPOILER = 1, F_WING = 1 << 1, F_STRIPE = 1 << 2, F_SCOOP = 1 << 3, F_RACK = 1 << 4,
            F_BULL = 1 << 5, F_POPUP = 1 << 6, F_TWOTONE = 1 << 7, F_FLARE = 1 << 8, F_CONV = 1 << 9,
            F_TARGA = 1 << 10, F_SPLIT = 1 << 11, F_SIDEPIPE = 1 << 12, F_INTAKE = 1 << 13, F_DENT = 1 << 14,
            F_NUMBER = 1 << 15, F_WOOD = 1 << 16, F_BUMPER = 1 << 17, F_ARMOR = 1 << 18, F_BED = 1 << 19,
            F_MULTI = 1 << 20, F_PANEL = 1 << 21, F_ROUNDL = 1 << 22, F_ROUNDT = 1 << 23, F_WIRE = 1 << 24,
            F_PANDA = 1 << 25, F_CANOPY = 1 << 26, F_STRUT = 1 << 27, F_MISMATCH = 1 << 28, F_ROLL = 1 << 29,
            F_KIDNEY = 1 << 30;

    /** The design sheet of one car. All numbers are pixel positions in the 96 x 40 side picture. */
    private static final class Spec {
        int x0, x1;        // tail and nose
        int bb;            // bottom of the body
        int deckY, hoodY;  // height of the rear deck and of the hood (at the windscreen)
        int noseY;         // height of the very front
        int ws0, rw0;      // where the windscreen and the rear window start at the body
        int rf0, rf1;      // roof: rear and front end
        int roof;          // roof height
        int wr, fw, rw;    // wheel radius, front and rear wheel centre x
        int flags;
        Color accent;
        boolean wide, long_;
    }

    private static final Map<String, Spec> SPECS = new HashMap<String, Spec>();

    private static void def(String name, int x0, int x1, int bb, int deckY, int hoodY, int noseY, int ws0, int rw0,
                            int rf0, int rf1, int roof, int wr, int fw, int rw, int flags, Color accent) {
        Spec s = new Spec();
        s.x0 = x0; s.x1 = x1; s.bb = bb; s.deckY = deckY; s.hoodY = hoodY; s.noseY = noseY;
        s.ws0 = ws0; s.rw0 = rw0; s.rf0 = rf0; s.rf1 = rf1; s.roof = roof; s.wr = wr; s.fw = fw; s.rw = rw;
        s.flags = flags; s.accent = accent;
        SPECS.put(name, s);
    }

    private static final Color WHITE = new Color(245, 245, 245);
    private static final Color BLACK = new Color(30, 30, 36);
    private static final Color RED = new Color(215, 40, 40);

    static {
        //   name                      x0 x1  bb dk hd ny ws0 rw0 rf0 rf1 rf wr  fw rw   flags                                      accent
        // ---- Beginner ----
        def("Toyota Corolla",           3, 93, 30, 19, 19, 22, 64, 26, 33, 55,  7, 6, 73, 22, 0,                                      null);
        def("Nissan Sentra",            3, 93, 30, 19, 18, 21, 62, 26, 33, 52,  8, 6, 73, 22, F_ROUNDT,                               null);
        def("Honda Civic",              4, 92, 30, 18, 20, 24, 62, 12, 24, 50,  7, 6, 72, 22, F_SPOILER | F_ROUNDT,                   null);
        // ---- Amateur ----
        def("Mazda MX-5",               4, 92, 30, 21, 21, 24, 58, 28,  0,  0, 14, 6, 72, 24, F_CONV | F_POPUP | F_ROLL,              null);
        def("Volkswagen GTI",           5, 91, 30, 17, 18, 22, 63, 10, 22, 54,  5, 6, 72, 23, F_STRIPE | F_SPOILER,                   RED);
        def("Subaru BRZ",               3, 93, 30, 21, 20, 24, 58, 30, 42, 52, 12, 6, 73, 22, F_SPOILER | F_ROUNDT,                   null);
        // ---- Professional ----
        def("Ford Mustang",             3, 93, 30, 19, 18, 21, 60, 22, 38, 52,  9, 6, 74, 22, F_STRIPE | F_SCOOP,                     WHITE);
        def("BMW M3",                   3, 93, 30, 20, 18, 22, 64, 25, 33, 54,  8, 6, 73, 22, F_SCOOP | F_KIDNEY | F_SPOILER,         null);
        def("Audi S5",                  3, 93, 30, 20, 19, 22, 62, 14, 40, 52, 10, 6, 73, 22, F_ROUNDT,                               null);
        // ---- Expert ----
        def("Porsche 911",              4, 92, 30, 22, 21, 25, 57,  6, 30, 48, 10, 7, 70, 22, F_ROUNDL | F_SPOILER,                   null);
        def("Chevrolet Corvette C8",    3, 93, 31, 23, 23, 28, 54, 14, 26, 42, 14, 7, 72, 22, F_INTAKE | F_SPOILER | F_STRIPE,        BLACK);
        def("Nissan GT-R",              3, 93, 30, 19, 19, 23, 62, 20, 32, 50,  8, 7, 73, 22, F_WING | F_ROUNDT,                      null);
        // ---- Legendary ----
        def("Lamborghini Huracan",      3, 93, 31, 26, 25, 31, 54, 14, 30, 42, 16, 7, 72, 22, F_INTAKE | F_STRIPE,                    BLACK);
        def("Ferrari 488",              3, 93, 31, 25, 24, 30, 54, 12, 28, 42, 15, 7, 72, 22, F_INTAKE | F_ROUNDT | F_SPOILER,        null);
        def("McLaren 720S",             3, 93, 31, 24, 25, 30, 52, 10, 26, 40, 14, 7, 72, 22, F_INTAKE | F_WING | F_CANOPY,           null);
        // ---- Retro rewards ----
        def("1970 Ford Mustang",        3, 93, 30, 19, 18, 21, 60, 22, 38, 52,  9, 6, 74, 22, F_STRIPE | F_SCOOP | F_ROUNDL,          BLACK);
        def("1986 Toyota AE86",         5, 91, 30, 18, 20, 24, 60, 14, 25, 50,  8, 6, 71, 23, F_POPUP | F_PANDA | F_SPOILER,          null);
        def("1969 Chevrolet Camaro",    3, 93, 30, 19, 18, 21, 60, 26, 40, 52,  9, 6, 74, 22, F_STRIPE | F_SCOOP | F_ROUNDL,          WHITE);
        def("1964 Jaguar E-Type",       2, 94, 30, 22, 21, 27, 46,  8, 26, 36, 14, 6, 76, 20, F_ROUNDL | F_WIRE,                      null);
        def("1965 Shelby Cobra",        4, 92, 30, 21, 21, 24, 56, 28,  0,  0, 14, 6, 72, 24, F_CONV | F_SIDEPIPE | F_STRIPE | F_FLARE | F_WIRE | F_ROUNDL, WHITE);
        def("1973 Porsche 911 RS",      4, 92, 30, 21, 20, 24, 57,  8, 30, 48,  8, 6, 70, 22, F_ROUNDL | F_SPOILER | F_STRIPE | F_WIRE, BLACK);
        def("1963 Corvette Stingray",   3, 93, 30, 20, 18, 22, 58,  6, 30, 46,  9, 6, 73, 22, F_SPLIT | F_POPUP | F_SCOOP,           null);
        def("1991 Acura NSX",           3, 93, 31, 24, 24, 28, 54, 12, 28, 44, 14, 7, 72, 22, F_POPUP | F_TARGA | F_INTAKE | F_SPOILER, null);
        // ---- Demolition derby cars ----
        def("Rusty Wagon",              3, 92, 30, 18, 19, 22, 64,  4,  6, 56,  6, 6, 74, 22, F_RACK | F_WOOD | F_DENT | F_NUMBER,    new Color(150, 100, 60));
        def("Beater Sedan",             3, 93, 30, 19, 19, 22, 64, 25, 32, 54,  8, 6, 73, 22, F_DENT | F_NUMBER | F_MISMATCH,         null);
        def("Pickup Brute",             3, 93, 32, 22, 18, 21, 68, 42, 46, 62,  6, 7, 76, 21, F_BED | F_BULL | F_DENT | F_NUMBER,     null);
        def("Muscle Mauler",            3, 93, 30, 19, 18, 21, 60, 22, 38, 52,  9, 6, 74, 22, F_STRIPE | F_SCOOP | F_DENT | F_NUMBER | F_BUMPER, WHITE);
        def("Delivery Van",             3, 92, 31, 10, 19, 23, 68,  5,  6, 62,  5, 6, 76, 20, F_PANEL | F_BUMPER | F_DENT | F_NUMBER | F_ARMOR, null);
        def("School Bus Smasher",       2, 94, 31,  8, 17, 22, 76,  3,  3, 74,  4, 7, 78, 18, F_MULTI | F_BUMPER | F_DENT | F_NUMBER, new Color(40, 40, 40));
        def("Armored Limo",             1, 95, 30, 19, 19, 22, 70, 14, 20, 60,  9, 6, 79, 17, F_ARMOR | F_BUMPER | F_DENT | F_NUMBER, null);
        def("Monster Truck",           10, 86, 22,  9, 10, 12, 62, 30, 34, 54,  1, 11, 71, 25, F_BULL | F_DENT | F_NUMBER | F_STRUT | F_FLARE, null);
    }

    private CarArt() { }

    /** The old style number (0 sedan .. 4 muscle) is used for cars without their own design. */
    private static Spec spec(String name, int style) {
        Spec s = SPECS.get(name);
        if (s == null) {
            String[] reps = { "Toyota Corolla", "Honda Civic", "Subaru BRZ", "Ferrari 488", "Ford Mustang" };
            s = SPECS.get(reps[Math.max(0, Math.min(4, style))]);
        }
        s.wide = s.noseY >= 25 || (s.flags & (F_PANEL | F_MULTI | F_ARMOR | F_BED)) != 0 || s.wr >= 7;
        s.long_ = s.x1 - s.x0 >= 90;
        return s;
    }

    private static void px(Graphics2D g, Color c, int x, int y, int w, int h) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    private static boolean has(Spec s, int f) { return (s.flags & f) != 0; }

    // ------------------------------------------------------------------
    // tiny 3 x 5 pixel digits (derby numbers)
    // ------------------------------------------------------------------
    private static final String[] DIGITS = {
        "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
        "111100111001111", "111100111101111", "111001010010010", "111101111101111", "111101111001111"
    };

    private static void digits(Graphics2D g, int number, int x, int y, Color c) {
        String t = String.valueOf(Math.abs(number) % 100);
        for (int d = 0; d < t.length(); d++) {
            String bits = DIGITS[t.charAt(d) - '0'];
            for (int i = 0; i < 15; i++) {
                if (bits.charAt(i) == '1') px(g, c, x + d * 4 + (i % 3), y + i / 3, 1, 1);
            }
        }
    }

    /** A number from 1 to 99 that is always the same for the same car name. */
    static int numberFor(String name) {
        return 1 + Math.abs(name.hashCode()) % 98;
    }

    // ------------------------------------------------------------------
    // SIDE VIEW (96 x 40)
    // ------------------------------------------------------------------
    static BufferedImage side(String name, int style, Color body) {
        Spec s = spec(name, style);
        BufferedImage img = new BufferedImage(96, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color dark = body.darker(), darker = dark.darker(), light = body.brighter();
        Color glass = new Color(150, 205, 235), glassHi = new Color(215, 238, 250);
        Color tire = new Color(24, 24, 30), rim = new Color(196, 200, 210);
        Color arch = new Color(14, 14, 18);
        Color iron = new Color(96, 98, 108);
        Color rust = new Color(150, 84, 40);
        Random rnd = new Random(name.hashCode());
        int wy = 38 - s.wr;
        boolean conv = has(s, F_CONV);

        // shadow
        px(g, new Color(0, 0, 0, 70), s.x0 + 2, 37, s.x1 - s.x0 - 4, 2);

        // main body
        Polygon bodyP = new Polygon();
        bodyP.addPoint(s.x0, s.bb);
        bodyP.addPoint(s.x0, s.deckY);
        bodyP.addPoint(s.rw0, s.deckY);
        bodyP.addPoint(s.ws0, s.hoodY);
        bodyP.addPoint(s.x1 - 4, s.noseY);
        bodyP.addPoint(s.x1, s.noseY + 2);
        bodyP.addPoint(s.x1, s.bb);
        g.setColor(body);
        g.fill(bodyP);

        // greenhouse (the cabin)
        if (!conv) {
            Color roofColor = has(s, F_TWOTONE) && s.accent != null ? s.accent : body;
            Polygon cab = new Polygon();
            cab.addPoint(s.rw0 - 1, s.deckY);
            cab.addPoint(s.rf0, s.roof);
            cab.addPoint(s.rf1, s.roof);
            cab.addPoint(s.ws0 + 1, s.hoodY);
            g.setColor(roofColor);
            g.fill(cab);
            if (has(s, F_TARGA)) px(g, BLACK, s.rf0 + 2, s.roof, s.rf1 - s.rf0 - 3, 2);
            // glass
            Polygon gl = new Polygon();
            gl.addPoint(s.rw0 + 2, s.deckY - 1);
            gl.addPoint(Math.min(s.rf0 + 3, s.rf1 - 6), s.roof + 2);
            gl.addPoint(s.rf1 - 2, s.roof + 2);
            gl.addPoint(s.ws0 - 2, s.hoodY - 1);
            if (has(s, F_PANEL)) {
                // delivery van: glass only in the cab at the front
                gl = new Polygon();
                gl.addPoint(s.ws0 - 14, s.hoodY - 1);
                gl.addPoint(s.ws0 - 14, s.roof + 3);
                gl.addPoint(s.rf1 - 2, s.roof + 3);
                gl.addPoint(s.ws0 - 2, s.hoodY - 1);
            }
            g.setColor(glass);
            g.fill(gl);
            g.setColor(glassHi);
            g.drawLine(gl.xpoints[1] + 3, gl.ypoints[1], gl.xpoints[1] + 6, gl.ypoints[1]);
            g.drawLine(gl.xpoints[0] + 2, gl.ypoints[0] - 1, gl.xpoints[1] + 3, gl.ypoints[1] + 1);
            // pillars
            int left = gl.xpoints[0], right = gl.xpoints[3];
            if (has(s, F_MULTI)) {
                for (int x = left + 8; x < right - 4; x += 9) px(g, body, x, s.roof + 2, 2, s.deckY - s.roof - 3);
            } else if (has(s, F_SPLIT)) {
                px(g, darker, (gl.xpoints[0] + gl.xpoints[1]) / 2 + 1, gl.ypoints[1], 1, gl.ypoints[0] - gl.ypoints[1]);
            } else if (!has(s, F_CANOPY) && !has(s, F_PANEL) && right - left > 14) {
                px(g, roofColor, (left + right) / 2 + 3, s.roof + 2, 2, s.deckY - s.roof - 2);
            }
            // roof rack
            if (has(s, F_RACK)) {
                px(g, darker, s.rf0 + 1, s.roof - 2, s.rf1 - s.rf0 - 2, 1);
                px(g, darker, s.rf0 + 3, s.roof - 1, 1, 1);
                px(g, darker, s.rf1 - 4, s.roof - 1, 1, 1);
            }
        } else {
            // convertible: open seats, a little windscreen frame, a driver and a roll bar
            px(g, new Color(40, 30, 30), s.rw0 + 1, s.deckY - 3, s.ws0 - s.rw0 - 6, 4);
            px(g, new Color(150, 70, 50), s.rw0 + 3, s.deckY - 7, 5, 5);               // seat back
            g.setColor(new Color(240, 200, 160));
            g.fillOval(s.rw0 + 8, s.deckY - 11, 6, 6);                                     // driver's head
            px(g, glass, s.ws0 - 4, s.hoodY - 7, 2, 7);
            px(g, dark, s.ws0 - 5, s.hoodY - 8, 3, 1);
            if (has(s, F_ROLL)) {
                px(g, darker, s.rw0 + 1, s.deckY - 9, 2, 8);
                px(g, darker, s.rw0 + 1, s.deckY - 10, 6, 2);
            }
        }

        // body shading and details
        px(g, light, s.x0 + 3, s.deckY + 1, Math.max(1, s.rw0 - s.x0 - 3), 1);
        px(g, light, s.ws0 + 2, s.hoodY + 1, Math.max(1, s.x1 - 6 - s.ws0), 1);
        px(g, dark, s.x0 + 2, s.bb - 3, s.x1 - s.x0 - 4, 3);                              // lower sill
        int doorX = (s.rw0 + s.ws0) / 2;
        px(g, dark, doorX, s.deckY + 2, 1, s.bb - s.deckY - 5);
        px(g, rim, doorX + 3, s.deckY + 3, 3, 1);                                         // door handle
        px(g, body, s.ws0 - 1, s.hoodY - 3, 3, 2);                                        // mirror
        if (has(s, F_PANDA)) px(g, darker, s.x0 + 1, (s.deckY + s.bb) / 2 + 2, s.x1 - s.x0 - 2, s.bb - (s.deckY + s.bb) / 2 - 2);
        if (has(s, F_WOOD) && s.accent != null) {
            px(g, s.accent, s.x0 + 4, s.deckY + 3, s.x1 - s.x0 - 14, 5);
            for (int x = s.x0 + 6; x < s.x1 - 12; x += 5) px(g, s.accent.darker(), x, s.deckY + 3, 1, 5);
        }
        if (has(s, F_STRIPE) && s.accent != null) {
            int sy = (s.deckY + s.bb) / 2 - 1;
            px(g, s.accent, s.x0 + 2, sy, s.x1 - s.x0 - 6, 2);
        }
        if (has(s, F_SCOOP)) {
            px(g, darker, s.ws0 + 6, s.hoodY - 1, 10, 2);
            px(g, BLACK, s.ws0 + 8, s.hoodY - 1, 6, 1);
        }
        if (has(s, F_INTAKE)) {
            px(g, BLACK, s.rw0 + 3, s.deckY + 3, 9, 4);
            for (int i = 0; i < 4; i++) px(g, dark, s.rw0 + 4 + i * 2, s.deckY + 3, 1, 4);
        }
        if (has(s, F_SIDEPIPE)) px(g, rim, s.rw0 + 4, s.bb - 3, 24, 2);
        if (has(s, F_ARMOR)) {
            px(g, iron, s.x0 + 6, s.deckY + 3, s.x1 - s.x0 - 18, s.bb - s.deckY - 7);
            for (int x = s.x0 + 9; x < s.x1 - 14; x += 8) px(g, new Color(60, 62, 70), x, s.deckY + 3, 1, s.bb - s.deckY - 7);
            px(g, new Color(150, 152, 162), s.x0 + 6, s.deckY + 3, s.x1 - s.x0 - 18, 1);
        }
        if (has(s, F_MISMATCH)) {
            Color m = new Color(70, 150, 120);
            px(g, m, doorX - 12, s.deckY + 2, 24, s.bb - s.deckY - 5);
            px(g, m.darker(), doorX, s.deckY + 2, 1, s.bb - s.deckY - 5);
        }
        if (has(s, F_BED)) {
            px(g, new Color(40, 40, 46), s.x0 + 2, s.deckY - 1, s.rw0 - s.x0 - 4, 4);
            px(g, light, s.x0 + 1, s.deckY - 2, s.rw0 - s.x0 - 2, 1);
        }

        // wheels
        g.setClip(bodyP);
        for (int i = 0; i < 2; i++) {
            int cx = i == 0 ? s.fw : s.rw;
            g.setColor(arch);
            g.fillOval(cx - s.wr - 2, wy - s.wr - 2, 2 * s.wr + 4, 2 * s.wr + 4);
            if (has(s, F_FLARE)) {
                g.setColor(dark);
                g.drawOval(cx - s.wr - 3, wy - s.wr - 3, 2 * s.wr + 6, 2 * s.wr + 6);
            }
        }
        g.setClip(null);
        for (int i = 0; i < 2; i++) {
            int cx = i == 0 ? s.fw : s.rw;
            g.setColor(tire);
            g.fillOval(cx - s.wr, wy - s.wr, 2 * s.wr, 2 * s.wr);
            g.setColor(rim);
            g.fillOval(cx - s.wr + 3, wy - s.wr + 3, 2 * s.wr - 6, 2 * s.wr - 6);
            g.setColor(tire);
            if (has(s, F_WIRE)) {
                g.drawLine(cx - s.wr + 3, wy, cx + s.wr - 3, wy);
                g.drawLine(cx, wy - s.wr + 3, cx, wy + s.wr - 3);
                g.drawLine(cx - 2, wy - 2, cx + 2, wy + 2);
                g.drawLine(cx - 2, wy + 2, cx + 2, wy - 2);
            } else {
                px(g, new Color(120, 124, 136), cx - 1, wy - 1, 3, 3);
                px(g, tire, cx, wy, 1, 1);
            }
        }
        if (has(s, F_STRUT)) {
            px(g, iron, s.fw - 1, s.bb, 2, wy - s.bb - 2);
            px(g, iron, s.rw - 1, s.bb, 2, wy - s.bb - 2);
            px(g, iron, s.fw + 3, s.bb, 2, wy - s.bb - 2);
            px(g, iron, s.rw + 3, s.bb, 2, wy - s.bb - 2);
        }

        // lights, grille, bumpers
        int ny = s.noseY;
        if (has(s, F_POPUP)) {
            px(g, body, s.x1 - 12, ny - 3, 7, 3);
            px(g, new Color(255, 240, 150), s.x1 - 11, ny - 3, 4, 2);
        } else if (has(s, F_ROUNDL)) {
            g.setColor(rim);
            g.fillOval(s.x1 - 7, ny, 6, 6);
            g.setColor(new Color(255, 240, 160));
            g.fillOval(s.x1 - 6, ny + 1, 4, 4);
        } else {
            px(g, new Color(255, 240, 150), s.x1 - 4, ny + 1, 4, 3);
        }
        px(g, BLACK, s.x1 - 3, ny + 5, 3, Math.max(2, s.bb - ny - 9));
        if (has(s, F_KIDNEY)) {
            px(g, BLACK, s.x1 - 6, ny + 4, 2, 3);
            px(g, BLACK, s.x1 - 3, ny + 4, 2, 3);
        }
        px(g, new Color(170, 172, 182), s.x1 - 3, s.bb - 4, 4, 3);
        if (has(s, F_ROUNDT)) {
            g.setColor(RED);
            g.fillOval(s.x0, s.deckY + 1, 5, 5);
            g.setColor(new Color(255, 120, 100));
            g.fillRect(s.x0 + 1, s.deckY + 2, 2, 2);
        } else {
            px(g, RED, s.x0, s.deckY + 1, 2, 4);
        }
        px(g, new Color(150, 152, 160), s.x0, s.bb - 4, 3, 3);
        px(g, tire, s.x0 - 1, s.bb - 1, 2, 2);                                          // exhaust

        // aerodynamics and armour
        if (has(s, F_SPOILER)) {
            px(g, darker, s.x0 - 1, s.deckY - 3, 8, 2);
            px(g, darker, s.x0 + 2, s.deckY - 1, 2, 2);
        }
        if (has(s, F_WING)) {
            px(g, darker, s.x0 + 2, s.deckY - 8, 1, 7);
            px(g, darker, s.x0 + 7, s.deckY - 8, 1, 7);
            px(g, darker, s.x0 - 1, s.deckY - 10, 12, 2);
            px(g, dark, s.x0 - 1, s.deckY - 10, 1, 4);
        }
        if (has(s, F_BULL)) {
            px(g, iron, s.x1 - 4, ny + 1, 4, s.bb - ny - 3);
            px(g, new Color(160, 162, 172), s.x1 - 3, ny + 3, 3, 1);
            px(g, new Color(160, 162, 172), s.x1 - 3, ny + 8, 3, 1);
        }
        if (has(s, F_BUMPER)) {
            px(g, iron, s.x1 - 5, ny + 4, 6, s.bb - ny - 3);
            px(g, new Color(180, 182, 192), s.x1 - 5, ny + 4, 6, 1);
            px(g, BLACK, s.x1 - 3, ny + 7, 1, 1);
        }

        // derby wear: rust, dents, cracked glass and a painted number
        if (has(s, F_DENT)) {
            for (int i = 0; i < 7; i++) {
                int rx = s.x0 + 4 + rnd.nextInt(Math.max(4, s.x1 - s.x0 - 12));
                int ry = s.deckY + 1 + rnd.nextInt(Math.max(3, s.bb - s.deckY - 5));
                px(g, i % 2 == 0 ? rust : darker, rx, ry, 2 + rnd.nextInt(3), 1 + rnd.nextInt(2));
            }
            g.setColor(new Color(255, 255, 255, 200));
            if (!conv && !has(s, F_PANEL)) g.drawLine(s.rf1 - 6, s.roof + 3, s.rf1 - 11, s.roof + 7);
        }
        if (has(s, F_NUMBER)) {
            int nx = doorX - 5, ny2 = s.deckY + 3;
            if (has(s, F_PANEL) || has(s, F_MULTI)) { nx = (s.x0 + s.rw0) / 2 + 20; ny2 = s.deckY + 8; }
            px(g, WHITE, nx - 1, ny2 - 1, 11, 7);
            digits(g, numberFor(name), nx + (numberFor(name) < 10 ? 2 : 0), ny2, BLACK);
        }
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------
    // TOP VIEW (22 x 40), the car faces UP
    // ------------------------------------------------------------------
    static BufferedImage top(String name, int style, Color body) {
        Spec s = spec(name, style);
        BufferedImage img = new BufferedImage(22, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color dark = body.darker(), darker = dark.darker(), light = body.brighter();
        Color glass = new Color(140, 198, 232), glassDark = new Color(96, 150, 190);
        Color tire = new Color(22, 22, 28), iron = new Color(100, 102, 112);
        Color rust = new Color(150, 84, 40);
        Random rnd = new Random(name.hashCode() * 7L);
        double k = 37.0 / (s.x1 - s.x0);
        int[] yOf = new int[100];
        for (int x = 0; x < 100; x++) yOf[x] = (int) Math.round(1 + (s.x1 - x) * k);
        int w = s.wide ? 16 : 14;
        int bx = 11 - w / 2;
        int yNose = 1, yTail = 38;
        boolean conv = has(s, F_CONV);

        // body outline with a tapered nose and rounded tail
        for (int y = yNose; y <= yTail; y++) {
            int inset = 0;
            int fromNose = y - yNose, fromTail = yTail - y;
            if (fromNose == 0) inset = 3;
            else if (fromNose == 1) inset = 2;
            else if (fromNose == 2) inset = 1;
            if (s.noseY >= 25 && fromNose < 10) inset = Math.max(inset, (10 - fromNose) / 3 + 1);   // wedge-shaped nose
            if (fromTail == 0) inset = Math.max(inset, 3);
            else if (fromTail == 1) inset = Math.max(inset, 1);
            px(g, body, bx + inset, y, w - 2 * inset, 1);
            px(g, dark, bx + inset, y, 1, 1);
            px(g, dark, bx + w - inset - 1, y, 1, 1);
        }
        int yWs = yOf[s.ws0], yRf1 = yOf[Math.max(s.rf1, 1)], yRf0 = yOf[Math.max(s.rf0, 1)], yRw = yOf[Math.max(s.rw0, 1)];
        if (yRf1 <= yWs) yRf1 = yWs + 2;
        if (yRf0 <= yRf1 + 1) yRf0 = yRf1 + 2;
        if (yRw <= yRf0) yRw = yRf0 + 2;
        if (conv) { yRf1 = yWs + 2; yRf0 = yRw - 2; }
        int gx = bx + 2, gw = w - 4;

        // hood highlight
        px(g, light, bx + 3, yNose + 2, w - 6, Math.max(1, yWs - yNose - 3));

        if (conv) {
            px(g, glass, gx, yWs, gw, 2);                                            // small windscreen
            px(g, new Color(45, 34, 34), gx, yWs + 2, gw, yRw - yWs - 2);              // open cockpit
            px(g, new Color(150, 70, 50), gx + 1, yWs + 4, 3, 4);                      // seats
            px(g, new Color(150, 70, 50), gx + gw - 4, yWs + 4, 3, 4);
            g.setColor(new Color(240, 200, 160));
            g.fillRect(gx + 1, yWs + 5, 3, 3);                                            // driver
            if (has(s, F_ROLL)) {
                px(g, darker, gx, yWs + 9, gw, 1);
            }
        } else {
            Color roofColor = has(s, F_TWOTONE) && s.accent != null ? s.accent : body;
            if (has(s, F_TARGA)) roofColor = BLACK;
            px(g, glass, gx, yWs, gw, Math.max(1, yRf1 - yWs));                         // windscreen
            px(g, light, gx, yWs, gw, 1);
            px(g, roofColor, gx, yRf1, gw, Math.max(1, yRf0 - yRf1));                    // roof
            px(g, dark, gx, yRf1, 1, Math.max(1, yRf0 - yRf1));
            px(g, dark, gx + gw - 1, yRf1, 1, Math.max(1, yRf0 - yRf1));
            if (has(s, F_CANOPY)) px(g, glassDark, gx + 2, yRf1, gw - 4, Math.max(1, yRf0 - yRf1));
            px(g, glassDark, gx, yRf0, gw, Math.max(1, yRw - yRf0));                      // rear window
            if (has(s, F_SPLIT)) px(g, darker, 10, yRf0, 2, Math.max(1, yRw - yRf0));
            if (has(s, F_PANEL) || has(s, F_MULTI)) {
                // a van or bus has one big roof
                px(g, has(s, F_MULTI) ? s.accent == null ? body : body : body, gx - 1, yRf1, gw + 2, Math.max(1, 37 - yRf1));
                px(g, dark, 11, yRf1 + 1, 1, Math.max(1, 35 - yRf1));
            }
            if (has(s, F_RACK)) {
                px(g, darker, gx, yRf1 + 1, gw, 1);
                px(g, darker, gx, yRf0 - 1, gw, 1);
            }
        }
        if (has(s, F_BED)) {
            px(g, body, bx, yRw + 1, w, yTail - yRw);
            px(g, new Color(40, 40, 46), bx + 2, yRw + 3, w - 4, yTail - yRw - 4);
            for (int y = yRw + 5; y < yTail - 2; y += 4) px(g, new Color(70, 70, 78), bx + 2, y, w - 4, 1);
        }

        // stripes, scoops, spoilers
        if (has(s, F_STRIPE) && s.accent != null) {
            px(g, s.accent, 10, yNose + 1, 2, yWs - yNose - 1);
            px(g, s.accent, 10, yRw, 2, yTail - yRw - 1);
            if (!has(s, F_TARGA)) px(g, s.accent, 10, yRf1, 2, Math.max(1, yRf0 - yRf1));
        }
        if (has(s, F_WOOD) && s.accent != null) {
            px(g, s.accent, bx, yRf1, 1, yRw - yRf1);
            px(g, s.accent, bx + w - 1, yRf1, 1, yRw - yRf1);
        }
        if (has(s, F_SCOOP)) {
            px(g, BLACK, 9, yNose + 3, 4, 3);
            px(g, darker, 9, yNose + 2, 4, 1);
        }
        if (has(s, F_INTAKE)) {
            px(g, BLACK, bx, yRf1 + 1, 2, 3);
            px(g, BLACK, bx + w - 2, yRf1 + 1, 2, 3);
        }
        if (has(s, F_SPOILER)) {
            px(g, darker, bx + 1, yTail - 3, w - 2, 2);
        }
        if (has(s, F_WING)) {
            px(g, darker, bx - 1, yTail - 3, w + 2, 2);
            px(g, BLACK, bx + 2, yTail - 5, 1, 3);
            px(g, BLACK, bx + w - 3, yTail - 5, 1, 3);
        }
        if (has(s, F_BULL) || has(s, F_BUMPER)) {
            px(g, iron, bx + 1, yNose - 1 < 0 ? 0 : yNose - 1, w - 2, 2);
            px(g, new Color(170, 172, 182), bx + 1, 0, w - 2, 1);
        }
        if (has(s, F_ARMOR)) {
            px(g, iron, bx, yRf1 + 2, 1, Math.max(1, yRw - yRf1 - 2));
            px(g, iron, bx + w - 1, yRf1 + 2, 1, Math.max(1, yRw - yRf1 - 2));
        }
        if (has(s, F_PANDA)) px(g, darker, bx, yRf0 + 2, w, yTail - yRf0 - 2);

        // wheels
        int fy = yOf[s.fw], ry = yOf[s.rw];
        int tw = s.wr >= 7 ? 3 : 2, th = s.wr >= 10 ? 8 : s.wr >= 7 ? 6 : 5;
        for (int i = 0; i < 2; i++) {
            int cy = i == 0 ? fy : ry;
            px(g, tire, bx - tw + 1, cy - th / 2, tw, th);
            px(g, tire, bx + w - 1, cy - th / 2, tw, th);
        }

        // lights
        if (has(s, F_ROUNDL) || has(s, F_POPUP)) {
            px(g, new Color(255, 240, 160), bx + 1, 1, 2, 2);
            px(g, new Color(255, 240, 160), bx + w - 3, 1, 2, 2);
        } else {
            px(g, new Color(255, 240, 160), bx + 1, 1, 3, 1);
            px(g, new Color(255, 240, 160), bx + w - 4, 1, 3, 1);
        }
        px(g, RED, bx + 1, 38, 3, 1);
        px(g, RED, bx + w - 4, 38, 3, 1);
        if (has(s, F_ROUNDT)) {
            px(g, RED, bx + 1, 37, 2, 2);
            px(g, RED, bx + w - 3, 37, 2, 2);
        }
        // mirrors
        px(g, dark, bx - 1, yWs + 1, 1, 2);
        px(g, dark, bx + w, yWs + 1, 1, 2);

        // derby wear and number
        if (has(s, F_DENT)) {
            for (int i = 0; i < 9; i++) {
                px(g, i % 3 == 0 ? rust : darker, bx + 1 + rnd.nextInt(w - 2), 2 + rnd.nextInt(36), 1 + rnd.nextInt(2), 1 + rnd.nextInt(3));
            }
        }
        if (has(s, F_NUMBER)) {
            int ry0 = Math.max(yRf1 + 1, 17);
            px(g, WHITE, 7, ry0 - 1, 9, 7);
            digits(g, numberFor(name), numberFor(name) < 10 ? 10 : 8, ry0, BLACK);
        }
        g.dispose();
        return img;
    }
}
