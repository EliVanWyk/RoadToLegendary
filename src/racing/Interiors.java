package racing;

import static racing.GameWindow.fill;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/** Pixel-art interiors (240 x 160 grid) for the new buildings. */
final class Interiors {

    private Interiors() { }

    static void paint(Graphics2D g, int id) {
        switch (id) {
            case CityMap.CHAMPIONSHIP: championship(g); break;
            case CityMap.PRACTICE: practice(g); break;
            case CityMap.KNOCKOUT: knockout(g); break;
            case CityMap.STREET: street(g); break;
            case CityMap.POLICE: police(g); break;
            case CityMap.DERBY: derby(g); break;
            default: fill(g, new Color(40, 44, 70), 0, 0, 240, 160);
        }
    }

    private static void championship(Graphics2D g) {
        for (int i = 0; i < 16; i++) fill(g, new Color(86 + i * 2, 24 + i, 44 + i), 0, i * 8, 240, 8);
        // gold trim and banners
        fill(g, new Color(214, 170, 60), 0, 18, 240, 2);
        Color[] b = { new Color(60, 90, 200), new Color(240, 200, 60), new Color(60, 170, 90), new Color(220, 60, 60) };
        for (int i = 0; i < 8; i++) {
            fill(g, b[i % 4], 8 + i * 30, 0, 12, 14);
            fill(g, new Color(0, 0, 0, 50), 8 + i * 30 + 10, 0, 2, 14);
        }
        // marble floor with a red carpet
        for (int y = 112; y < 160; y += 8) for (int x = 0; x < 240; x += 8) {
            fill(g, ((x + y) / 8) % 2 == 0 ? new Color(224, 216, 200) : new Color(196, 188, 170), x, y, 8, 8);
        }
        fill(g, new Color(150, 30, 46), 90, 112, 60, 48);
        fill(g, new Color(230, 190, 70), 90, 112, 2, 48);
        fill(g, new Color(230, 190, 70), 148, 112, 2, 48);
        // winners' podium and trophies on the sides
        fill(g, new Color(230, 200, 110), 8, 124, 18, 36);
        fill(g, new Color(250, 220, 80), 12, 108, 10, 12);
        fill(g, new Color(250, 220, 80), 16, 120, 2, 6);
        fill(g, new Color(230, 200, 110), 214, 124, 18, 36);
        fill(g, new Color(250, 220, 80), 218, 108, 10, 12);
        fill(g, new Color(250, 220, 80), 222, 120, 2, 6);
    }

    private static void practice(Graphics2D g) {
        for (int i = 0; i < 16; i++) fill(g, new Color(88 + i, 92 + i, 104 + i), 0, i * 8, 240, 8);
        fill(g, new Color(230, 120, 40), 0, 18, 240, 3);
        for (int i = 0; i < 12; i++) fill(g, i % 2 == 0 ? Color.BLACK : Color.WHITE, i * 20, 21, 20, 2);
        // timing board with LED digits
        fill(g, new Color(20, 22, 30), 84, 2, 72, 14);
        for (int i = 0; i < 5; i++) fill(g, new Color(240, 60, 60), 90 + i * 12, 5, 6, 8);
        fill(g, new Color(240, 60, 60), 118, 8, 2, 2);
        // concrete floor, painted lanes
        fill(g, new Color(110, 112, 122), 0, 110, 240, 50);
        for (int x = 0; x < 240; x += 24) fill(g, new Color(240, 230, 120), x, 134, 12, 2);
        // tyre stacks
        for (int k = 0; k < 2; k++) {
            int x = k == 0 ? 8 : 214;
            for (int i = 0; i < 4; i++) {
                fill(g, new Color(30, 30, 34), x, 130 - i * 6, 18, 6);
                fill(g, new Color(60, 60, 66), x + 6, 132 - i * 6, 6, 2);
            }
        }
    }

    private static void knockout(Graphics2D g) {
        for (int i = 0; i < 16; i++) fill(g, new Color(30 + i, 24 + i, 40 + i), 0, i * 8, 240, 8);
        // crowd in the stands
        Random r = new Random(3);
        Color[] shirts = { new Color(220, 60, 60), new Color(60, 110, 220), new Color(240, 200, 50), Color.WHITE, new Color(70, 180, 100) };
        for (int row = 0; row < 5; row++) {
            fill(g, new Color(60, 54, 74), 0, 24 + row * 8, 240, 1);
            for (int x = 2; x < 238; x += 5) fill(g, shirts[r.nextInt(5)], x, 26 + row * 8, 3, 5);
        }
        // spotlights
        for (int i = 0; i < 6; i++) {
            fill(g, new Color(255, 245, 190), 20 + i * 40, 2, 6, 4);
            fill(g, new Color(255, 245, 190, 40), 14 + i * 40, 6, 18, 14);
        }
        // arena floor with red/white stripes
        fill(g, new Color(54, 54, 66), 0, 108, 240, 52);
        for (int x = 0; x < 240; x += 12) fill(g, (x / 12) % 2 == 0 ? new Color(200, 50, 60) : Color.WHITE, x, 108, 12, 3);
        fill(g, new Color(250, 240, 120), 0, 136, 240, 1);
        // scoreboard
        fill(g, new Color(20, 20, 26), 88, 4, 64, 14);
        fill(g, new Color(240, 60, 60), 96, 8, 6, 6);
        fill(g, new Color(240, 60, 60), 106, 8, 6, 6);
        fill(g, new Color(250, 230, 100), 120, 8, 24, 6);
    }

    private static void police(Graphics2D g) {
        for (int i = 0; i < 16; i++) fill(g, new Color(26 + i, 40 + i * 2, 90 + i * 3), 0, i * 8, 240, 8);
        // red / blue light strip along the ceiling
        for (int x = 0; x < 240; x += 8) fill(g, ((x / 8) & 1) == 0 ? new Color(230, 50, 50) : new Color(60, 110, 255), x, 18, 8, 3);
        fill(g, new Color(255, 255, 255, 40), 0, 21, 240, 3);
        // wanted posters
        for (int i = 0; i < 5; i++) {
            int x = 14 + i * 48;
            fill(g, new Color(230, 214, 170), x, 30, 20, 26);
            fill(g, new Color(70, 60, 50), x + 6, 36, 8, 8);
            fill(g, new Color(70, 60, 50), x + 3, 46, 14, 2);
            fill(g, new Color(200, 50, 50), x + 3, 50, 14, 2);
        }
        // tiled floor with a blue stripe
        for (int y = 112; y < 160; y += 8) for (int x = 0; x < 240; x += 8) {
            fill(g, ((x + y) / 8) % 2 == 0 ? new Color(70, 78, 110) : new Color(56, 62, 92), x, y, 8, 8);
        }
        fill(g, new Color(250, 214, 60), 0, 110, 240, 2);
        // patrol car lights on both sides
        fill(g, new Color(230, 50, 50, 70), 0, 60, 30, 50);
        fill(g, new Color(60, 110, 255, 70), 210, 60, 30, 50);
    }

    private static void derby(Graphics2D g) {
        for (int i = 0; i < 16; i++) fill(g, new Color(46 + i, 34 + i, 30 + i), 0, i * 8, 240, 8);
        // scrap-metal wall with painted flames
        for (int y = 0; y < 100; y += 6) for (int x = (y / 6 % 2) * 6; x < 240; x += 12) fill(g, new Color(86, 66, 50), x, y, 11, 5);
        for (int i = 0; i < 14; i++) {
            int x = 8 + i * 17;
            fill(g, new Color(250, 150, 30), x, 12, 8, 6);
            fill(g, new Color(250, 220, 70), x + 2, 14, 4, 4);
            fill(g, new Color(230, 70, 30), x + 3, 8, 2, 4);
        }
        // hanging tyres and warning lights
        for (int i = 0; i < 5; i++) {
            fill(g, new Color(20, 20, 24), 20 + i * 46, 24, 14, 14);
            fill(g, new Color(90, 80, 74), 24 + i * 46, 28, 6, 6);
        }
        // dirt floor with tyre tracks
        fill(g, new Color(150, 112, 72), 0, 108, 240, 52);
        for (int i = 0; i < 40; i++) fill(g, i % 2 == 0 ? new Color(132, 98, 62) : new Color(168, 128, 84), (i * 53) % 236, 112 + (i * 31) % 44, 4, 2);
        for (int x = 0; x < 240; x += 6) { fill(g, new Color(40, 28, 20), x, 130 + (int) (4 * Math.sin(x / 14.0)), 3, 2); }
        // red and white barrier along the bottom
        for (int x = 0; x < 240; x += 12) fill(g, (x / 12) % 2 == 0 ? new Color(214, 54, 54) : new Color(240, 240, 240), x, 152, 12, 8);
    }

    private static void street(Graphics2D g) {
        for (int i = 0; i < 16; i++) fill(g, new Color(26 + i, 20 + i, 44 + i), 0, i * 8, 240, 8);
        // brick wall
        for (int y = 0; y < 100; y += 6) for (int x = (y / 6 % 2) * 6; x < 240; x += 12) {
            fill(g, new Color(58, 38, 60), x, y, 11, 5);
        }
        // neon signs
        Color[] neon = { new Color(255, 70, 180), new Color(60, 230, 240), new Color(250, 230, 70) };
        for (int i = 0; i < 4; i++) {
            fill(g, neon[i % 3], 12 + i * 58, 8, 40, 3);
            fill(g, new Color(neon[i % 3].getRed(), neon[i % 3].getGreen(), neon[i % 3].getBlue(), 60), 8 + i * 58, 4, 48, 12);
        }
        // string lights
        for (int x = 0; x < 240; x += 8) fill(g, ((x / 8) & 1) == 0 ? new Color(255, 230, 120) : new Color(255, 120, 200), x, 24 + (int) (3 * Math.sin(x / 12.0)), 3, 3);
        // wet street with neon reflections
        fill(g, new Color(34, 34, 46), 0, 104, 240, 56);
        for (int i = 0; i < 6; i++) fill(g, new Color(neon[i % 3].getRed(), neon[i % 3].getGreen(), neon[i % 3].getBlue(), 70), 10 + i * 40, 128 + (i % 2) * 10, 24, 3);
        for (int x = 0; x < 240; x += 20) fill(g, new Color(230, 220, 120), x, 150, 10, 2);
    }
}
