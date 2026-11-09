package racing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The 2D pixel-art city. This IS the game's main menu: the player clicks on
 * buildings instead of pressing menu buttons.
 *
 *  - unlocked buildings are bright, locked ones are dark with a padlock
 *  - hovering a building shows its name and a short description
 *  - clicking an unlocked building opens its screen
 *
 * The city is drawn on a small 240 x 160 grid and scaled up 4x, which gives
 * the chunky pixel-art look.
 */
public class CityMap extends JPanel implements GameWindow.Screen {

    public static final int GARAGE = 0;
    public static final int CAR_SHOP = 1;
    public static final int REPAIR_SHOP = 2;
    public static final int PAINT_SHOP = 3;
    public static final int RACE_HQ = 4;
    public static final int RACE_TRACK = 5;
    public static final int CHAMPIONSHIP = 6;
    public static final int PRACTICE = 7;
    public static final int KNOCKOUT = 8;
    public static final int STREET = 9;
    public static final int POLICE = 10;
    public static final int DERBY = 11;
    public static final int BUILDING_COUNT = 12;

    /** Full names (used on the building screens and in the tooltip). */
    public static final String[] BUILDING_NAMES = {
        "GARAGE", "CAR SHOP", "REPAIR SHOP", "PAINT SHOP", "RACE HQ", "RACE TRACK",
        "CHAMPIONSHIP HALL", "PRACTICE GARAGE", "KNOCKOUT ARENA", "STREET RACING SPOT", "POLICE CHASE", "DEMOLITION DERBY"
    };
    /** Short names that fit on the signs in the city. */
    private static final String[] SIGN_NAMES = {
        "GARAGE", "CAR SHOP", "REPAIR SHOP", "PAINT SHOP", "RACE HQ", "RACE TRACK",
        "CHAMPIONSHIP", "PRACTICE", "KNOCKOUT", "STREET RACE", "POLICE CHASE", "DERBY"
    };
    private static final String[] DESCRIPTIONS = {
        "Your car collection. Choose your racing car and upgrade its speed, acceleration and handling.",
        "Buy new cars as you rank up, or sell the ones you don't need.",
        "Fix crash damage so your car drives at its best.",
        "Give your car a new paint job. Cool colours, small price.",
        "Career headquarters: your stats and the SPONSOR challenges for bonus cash.",
        "Drive the 25 career races. Win a race to unlock the next one!",
        "Join a cup: 4 races, points add up, and the best driver takes the trophy.",
        "Time trials on races you've done. Earn medals to train your driving skills.",
        "Knockout: the last car is out every few seconds. Checkpoint Rush: beat the clock!",
        "Night races with traffic. Bet money on yourself for a big payout.",
        "The cops are after you! Outrun them to the finish line. Get caught and you pay a fine.",
        "Buy a beat-up car for ONE contest and be the last car standing in a smash-up arena!"
    };

    private static final int S = 4;                  // scale factor
    private static final int BW = 50, BH = 32;       // building size on the grid

    private final GameWindow window;
    private final BufferedImage background;
    private final Rectangle[] bounds = new Rectangle[BUILDING_COUNT];   // in screen pixels
    private int hover = -1;
    private Point mouse = new Point();
    private int tipIndex = 0;
    private int tipTicks = 0;

    private static final String[] TIPS = {
        "TIP: Hover over a building to see what it does.",
        "TIP: Win a race to unlock the next one in the Race Track.",
        "TIP: Hold SHIFT while steering to drift and fill your boost meter.",
        "TIP: Press SPACE for NITRO. It needs time to recharge.",
        "TIP: Drive close behind another car for a slipstream speed boost.",
        "TIP: Wet roads, gravel and ice change how your car grips.",
        "TIP: Visit the Repair Shop when your car condition gets low.",
        "TIP: Time trials in the Practice Garage train your driving skills.",
        "TIP: Sponsor challenges at Race HQ pay bonus cash and fame.",
        "TIP: Your game saves by itself when you move between buildings.",
        "TIP: In the Demolition Derby, hit enemies from the side or the back!"
    };

    // little cars driving along the streets
    private final double[] carX = { 40, 300, 560, 820, 150, 640, 420, 760 };
    private final double[] carSpeed = { 2.2, 1.6, -2.0, -2.8, 2.6, 1.9, -1.5, -2.3 };
    private final int[] carStreet = { 0, 0, 1, 1, 2, 2, 1, 0 };
    private static final int[] STREET_Y = { 240, 408, 598 };
    private final Color[] carColor = {
        new Color(200, 40, 40), new Color(50, 100, 210), new Color(235, 190, 40),
        new Color(60, 170, 90), new Color(240, 240, 240), new Color(150, 70, 190),
        new Color(240, 130, 40), new Color(40, 190, 200)
    };
    private final Timer timer;

    public CityMap(GameWindow window) {
        this.window = window;
        setLayout(null);
        setPreferredSize(new java.awt.Dimension(GameWindow.W, GameWindow.H));

        for (int i = 0; i < BUILDING_COUNT; i++) {
            bounds[i] = new Rectangle(buildingX(i) * S, buildingY(i) * S, BW * S, BH * S);
        }
        background = buildBackground();

        GameWindow.PixelButton settings = new GameWindow.PixelButton("SETTINGS", new Color(70, 90, 160));
        settings.setBounds(352, 16, 126, 36);
        settings.addActionListener(e -> window.showSettings());
        add(settings);
        GameWindow.PixelButton help = new GameWindow.PixelButton("HOW TO PLAY", new Color(60, 140, 100));
        help.setBounds(484, 16, 126, 36);
        help.addActionListener(e -> window.showTutorial(false));
        add(help);

        MouseAdapter mouseHandler = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
                int old = hover;
                hover = buildingAt(e.getPoint());
                boolean ok = hover >= 0 && isEnterable(hover) && isUnlocked(hover);
                setCursor(ok ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                if (old != hover || hover >= 0) repaint();
            }
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
            public void mouseClicked(MouseEvent e) {
                int id = buildingAt(e.getPoint());
                if (id < 0) return;
                if (!isEnterable(id)) {
                    Sound.play(Sound.CLICK);
                    return;
                }
                if (isUnlocked(id)) {
                    Sound.play(Sound.CLICK);
                    window.showBuilding(id);
                } else {
                    Sound.play(Sound.ERROR);
                    window.message("LOCKED", BUILDING_NAMES[id] + " is still locked.\n"
                            + window.getCareer().getBuildingLockText(id) + ".");
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // animate the traffic only while the city is on screen
        timer = new Timer(40, e -> {
            for (int i = 0; i < carX.length; i++) {
                carX[i] += carSpeed[i];
                if (carX[i] > GameWindow.W + 60) carX[i] = -80;
                if (carX[i] < -80) carX[i] = GameWindow.W + 60;
            }
            tipTicks++;
            if (tipTicks > 200) {
                tipTicks = 0;
                tipIndex = (tipIndex + 1) % TIPS.length;
            }
            repaint();
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) timer.start(); else timer.stop();
            }
        });
    }

    public void onShow() {
        hover = -1;
        Sound.playMusic(Sound.MUSIC_MENU);
        repaint();
    }

    private static int buildingX(int id) { return 8 + (id % 4) * 58; }
    private static int buildingY(int id) { return 26 + (id / 4) * 42; }

    private int buildingAt(Point p) {
        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i].contains(p)) return i;
        }
        return -1;
    }

    private static boolean isEnterable(int id) { return true; }

    private boolean isUnlocked(int id) {
        return window.getCareer().isBuildingUnlocked(window.getDriver(), id);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(background, 0, 0, GameWindow.W, GameWindow.H, null);

        // traffic on the streets
        for (int i = 0; i < carX.length; i++) drawTrafficCar(g2, i);

        // building signs, lock overlays, hover outline
        for (int i = 0; i < BUILDING_COUNT; i++) {
            Rectangle b = bounds[i];
            boolean unlocked = isUnlocked(i);
            String sign = SIGN_NAMES[i];
            int size = sign.length() > 11 ? 17 : 19;
            GameWindow.textCenter(g2, sign, b.x + b.width / 2, b.y + 27, size,
                    unlocked ? new Color(255, 240, 170) : new Color(150, 150, 150));
            if (!unlocked) {
                g2.setColor(new Color(10, 10, 30, 170));
                g2.fillRect(b.x, b.y + 36, b.width, b.height - 36);
                drawPadlock(g2, b.x + b.width / 2, b.y + b.height / 2 + 20);
            }
            if (i == hover) {
                outline(g2, b, unlocked ? Color.WHITE : new Color(255, 120, 120));
                if (unlocked && isEnterable(i)) {
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillRect(b.x, b.y, b.width, b.height);
                }
            }
        }

        drawHud(g2);
        drawTip(g2);
        if (hover >= 0) drawTooltip(g2);
    }

    private void outline(Graphics2D g2, Rectangle b, Color c) {
        g2.setColor(c);
        for (int t = 0; t < 4; t++) g2.drawRect(b.x - 4 + t, b.y - 4 + t, b.width + 8 - 2 * t - 1, b.height + 8 - 2 * t - 1);
    }

    private String dynamicLine(int id) {
        Driver d = window.getDriver();
        Career c = window.getCareer();
        switch (id) {
            case GARAGE: return "You own " + d.getCarsOwned() + " car" + (d.getCarsOwned() == 1 ? "" : "s") + ".";
            case CAR_SHOP: return "Your cash: " + GameWindow.money(d.getMoney());
            case REPAIR_SHOP: return "Your car: " + d.getCurrentCar().getCondition() + "% condition.";
            case RACE_HQ: return "Races won: " + c.getTotalWon() + "/25   Sponsors: " + d.getChallengesDone() + "/" + Career.CHALLENGES.length;
            case RACE_TRACK: return "Rank: " + d.getRankName() + " (" + c.getWinsInRank(d.getRank()) + "/5 wins)";
            case CHAMPIONSHIP: return "Cups won: " + d.getCupsWon();
            case PRACTICE: return "Skill points to spend: " + d.getSkillPoints();
            case KNOCKOUT: return "Knockout wins: " + d.getKnockoutWins() + "   Rush wins: " + d.getCheckpointWins();
            case STREET: return "Street wins: " + d.getStreetWins();
            case POLICE: return "Escapes: " + d.getPoliceWins();
            case DERBY: return "Derby wins: " + d.getDerbyWins();
            default: return "";
        }
    }

    private void drawTooltip(Graphics2D g2) {
        int id = hover;
        boolean enter = isEnterable(id);
        boolean unlocked = isUnlocked(id);
        int w = 380;
        FontMetrics fm = g2.getFontMetrics(GameWindow.font(14));
        List<String> lines = GameWindow.wrap(DESCRIPTIONS[id], fm, w - 28);
        String extra = unlocked ? dynamicLine(id) : "";
        String status;
        Color statusColor;
        if (unlocked) { status = "Click to enter"; statusColor = new Color(120, 240, 130); }
        else { status = "LOCKED: " + window.getCareer().getBuildingLockText(id); statusColor = new Color(255, 130, 110); }
        List<String> statusLines = GameWindow.wrap(status, fm, w - 28);
        int h = 52 + lines.size() * 18 + (extra.isEmpty() ? 0 : 20) + statusLines.size() * 18 + (status.isEmpty() ? 0 : 6);
        int tx = Math.max(8, Math.min(mouse.x + 18, GameWindow.W - w - 8));
        int ty = mouse.y + 22;
        if (ty + h > GameWindow.H - 8) ty = mouse.y - h - 12;
        ty = Math.max(8, ty);
        GameWindow.box(g2, tx, ty, w, h, new Color(24, 28, 56), new Color(255, 230, 120));
        GameWindow.text(g2, BUILDING_NAMES[id], tx + 14, ty + 30, 18, new Color(255, 240, 170), true);
        int y = ty + 52;
        for (String s : lines) {
            GameWindow.text(g2, s, tx + 14, y, 14, Color.WHITE, false);
            y += 18;
        }
        if (!extra.isEmpty()) {
            GameWindow.text(g2, extra, tx + 14, y + 2, 14, new Color(150, 200, 255), false);
            y += 20;
        }
        for (String s : statusLines) {
            if (s.isEmpty()) continue;
            GameWindow.text(g2, s, tx + 14, y + 4, 14, statusColor, false);
            y += 18;
        }
    }

    private void drawHud(Graphics2D g2) {
        Driver d = window.getDriver();
        GameWindow.box(g2, 12, 8, 330, 82, new Color(24, 28, 56), new Color(120, 160, 255));
        GameWindow.text(g2, d.getName().toUpperCase(), 28, 36, 22, new Color(255, 240, 170), true);
        GameWindow.text(g2, "Rank: " + d.getRankName(), 28, 56, 15, Color.WHITE, false);
        GameWindow.text(g2, "Fame Lv" + d.getRepLevel() + ": " + d.getRepName(), 28, 74, 14, new Color(190, 200, 230), false);
        GameWindow.textRight(g2, "Won " + window.getCareer().getTotalWon() + "/25", 326, 74, 14, new Color(190, 200, 230));

        GameWindow.box(g2, 618, 8, 330, 82, new Color(24, 28, 56), new Color(120, 160, 255));
        GameWindow.textRight(g2, GameWindow.money(d.getMoney()), 934, 40, 28, new Color(120, 240, 130));
        Car c = d.getCurrentCar();
        GameWindow.textRight(g2, c.getName() + "  " + c.getCondition() + "%", 934, 62, 16, Color.WHITE);
        GameWindow.textRight(g2, "Skill points: " + d.getSkillPoints(), 934, 79, 12, new Color(190, 200, 230));
    }

    private void drawTip(Graphics2D g2) {
        String tip = Settings.hints ? TIPS[tipIndex] : "Click a building to enter it";
        GameWindow.box(g2, 190, 596, 580, 34, new Color(24, 28, 56, 235), new Color(120, 160, 255));
        GameWindow.textCenter(g2, tip, 480, 619, 15, Color.WHITE);
    }

    private void drawTrafficCar(Graphics2D g, int i) {
        boolean east = carSpeed[i] > 0;
        int x = ((int) carX[i]) / 4 * 4;
        int y = STREET_Y[carStreet[i]];
        Color c = carColor[i];
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRect(x, y + 20, 44, 4);
        g.setColor(c);
        g.fillRect(x, y + 6, 44, 14);
        g.fillRect(x + 8, y, 24, 8);
        g.setColor(c.brighter());
        g.fillRect(x + 2, y + 8, 40, 2);
        g.setColor(new Color(150, 205, 235));
        g.fillRect(x + 10, y + 2, 20, 5);
        g.setColor(new Color(25, 25, 30));
        g.fillRect(x + 6, y + 16, 10, 8);
        g.fillRect(x + 28, y + 16, 10, 8);
        g.setColor(new Color(255, 240, 150));
        g.fillRect(east ? x + 42 : x, y + 9, 2, 4);
        g.setColor(new Color(220, 30, 30));
        g.fillRect(east ? x : x + 42, y + 9, 2, 4);
    }

    private void drawPadlock(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(220, 190, 60));
        g.fillRect(cx - 16, cy - 8, 32, 24);
        g.setColor(new Color(150, 120, 30));
        g.fillRect(cx - 16, cy + 12, 32, 4);
        g.setColor(new Color(190, 195, 205));
        g.fillRect(cx - 10, cy - 24, 4, 18);
        g.fillRect(cx + 6, cy - 24, 4, 18);
        g.fillRect(cx - 10, cy - 28, 20, 4);
        g.setColor(new Color(40, 30, 10));
        g.fillRect(cx - 3, cy, 6, 8);
    }

    // ------------------------------------------------------------------
    // Background: drawn once on a 240 x 160 grid
    // ------------------------------------------------------------------
    private BufferedImage buildBackground() {
        BufferedImage img = new BufferedImage(240, 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // sky
        for (int i = 0; i < 6; i++) GameWindow.fill(g, new Color(118 + i * 14, 184 + i * 9, 245), 0, i * 5, 240, 5);
        // clouds
        int[][] clouds = { {24, 6, 28}, {92, 12, 34}, {150, 5, 26}, {206, 13, 30} };
        for (int[] c : clouds) {
            GameWindow.fill(g, Color.WHITE, c[0], c[1], c[2], 5);
            GameWindow.fill(g, Color.WHITE, c[0] + 4, c[1] - 3, c[2] - 10, 4);
            GameWindow.fill(g, new Color(225, 235, 250), c[0], c[1] + 4, c[2], 1);
        }
        // far skyline
        int[] heights = { 14, 20, 11, 24, 16, 28, 12, 22, 18, 26, 13, 21, 15, 24, 10, 19, 23, 12, 17, 25 };
        for (int i = 0; i < heights.length; i++) {
            int x = i * 12;
            GameWindow.fill(g, new Color(112, 138, 190), x, 30 - heights[i], 11, heights[i]);
            for (int wy = 30 - heights[i] + 2; wy < 28; wy += 4) {
                GameWindow.fill(g, new Color(150, 176, 222), x + 2, wy, 2, 2);
                GameWindow.fill(g, new Color(150, 176, 222), x + 6, wy, 2, 2);
            }
        }

        // grass everywhere below the skyline
        GameWindow.fill(g, new Color(112, 176, 98), 0, 28, 240, 132);
        for (int y = 28; y < 160; y += 4) {
            for (int x = (y / 4 % 2) * 4; x < 240; x += 8) GameWindow.fill(g, new Color(104, 168, 92), x, y, 4, 4);
        }

        Color asphalt = new Color(72, 74, 84);
        Color curb = new Color(196, 196, 190);
        Color dash = new Color(236, 224, 120);

        // three streets
        int[][] streets = { {57, 12, 60, 6}, {99, 12, 102, 6}, {143, 17, 146, 14} };
        for (int[] s : streets) {
            GameWindow.fill(g, curb, 0, s[0], 240, s[1]);
            GameWindow.fill(g, asphalt, 0, s[2], 240, s[3]);
            int cy = s[2] + s[3] / 2;
            for (int x = 0; x < 240; x += 8) GameWindow.fill(g, dash, x, cy, 4, 1);
        }

        // plazas under each building
        for (int id = 0; id < BUILDING_COUNT; id++) {
            int x = buildingX(id), y = buildingY(id);
            GameWindow.fill(g, curb, x - 2, y - 1, BW + 4, BH + 3);
        }
        // buildings
        for (int id = 0; id < BUILDING_COUNT; id++) drawBuilding(g, id, buildingX(id), buildingY(id), BW, BH);

        // trees and lamp posts between the buildings
        int[][] trees = { {58, 32}, {116, 32}, {174, 32}, {58, 74}, {116, 74}, {174, 74}, {58, 116}, {116, 116}, {174, 116},
                          {0, 32}, {232, 32}, {0, 74}, {232, 74}, {0, 116}, {232, 116} };
        for (int[] t : trees) drawTree(g, t[0], t[1]);
        for (int x = 20; x < 240; x += 40) {
            GameWindow.fill(g, new Color(50, 50, 60), x, 58, 2, 3);
            GameWindow.fill(g, new Color(255, 235, 140), x - 1, 57, 4, 2);
            GameWindow.fill(g, new Color(50, 50, 60), x + 20, 100, 2, 3);
            GameWindow.fill(g, new Color(255, 235, 140), x + 19, 99, 4, 2);
        }
        g.dispose();
        return img;
    }

    private void drawTree(Graphics2D g, int x, int y) {
        GameWindow.fill(g, new Color(0, 0, 0, 40), x + 1, y + 7, 8, 2);
        GameWindow.fill(g, new Color(90, 60, 40), x + 3, y + 5, 2, 4);
        GameWindow.fill(g, new Color(40, 120, 50), x, y + 1, 8, 6);
        GameWindow.fill(g, new Color(40, 120, 50), x + 1, y, 6, 8);
        GameWindow.fill(g, new Color(70, 160, 70), x + 2, y + 1, 3, 3);
    }

    private static void f(Graphics2D g, Color c, int x, int y, int w, int h) { GameWindow.fill(g, c, x, y, w, h); }

    /** Draws one building facade on the low-res grid. The top 9 rows are the sign. */
    private void drawBuilding(Graphics2D g, int id, int x, int y, int w, int h) {
        Color shadow = new Color(0, 0, 0, 50);
        switch (id) {
            case GARAGE: {
                f(g, new Color(168, 88, 66), x, y + 9, w, h - 9);
                for (int yy = y + 11; yy < y + h; yy += 4) f(g, new Color(140, 70, 54), x, yy, w, 1);
                f(g, new Color(70, 40, 36), x - 1, y, w + 2, 9);
                f(g, new Color(100, 60, 50), x - 1, y, w + 2, 2);
                f(g, new Color(52, 52, 62), x + 13, y + 12, 24, h - 12);
                f(g, new Color(190, 194, 206), x + 14, y + 13, 22, h - 13);
                for (int yy = y + 13; yy < y + h; yy += 3) f(g, new Color(150, 154, 168), x + 14, yy, 22, 1);
                f(g, new Color(150, 200, 235), x + 3, y + 15, 7, 6);
                f(g, new Color(150, 200, 235), x + w - 10, y + 15, 7, 6);
                break;
            }
            case CAR_SHOP: {
                f(g, new Color(230, 232, 242), x, y + 9, w, h - 9);
                f(g, new Color(40, 70, 160), x - 1, y, w + 2, 9);
                f(g, new Color(70, 100, 190), x - 1, y, w + 2, 2);
                for (int xx = x; xx < x + w; xx += 5) {
                    f(g, ((xx - x) / 5) % 2 == 0 ? new Color(210, 50, 50) : Color.WHITE, xx, y + 10, 5, 3);
                }
                f(g, new Color(60, 70, 100), x + 3, y + 14, w - 6, h - 14);
                f(g, new Color(120, 190, 236), x + 4, y + 15, w - 8, h - 16);
                f(g, new Color(180, 222, 250), x + 4, y + 15, w - 8, 3);
                f(g, new Color(200, 40, 50), x + 7, y + 25, 16, 4);
                f(g, new Color(200, 40, 50), x + 10, y + 22, 9, 4);
                f(g, new Color(30, 30, 36), x + 9, y + 28, 3, 2);
                f(g, new Color(30, 30, 36), x + 18, y + 28, 3, 2);
                f(g, new Color(50, 100, 220), x + 28, y + 25, 16, 4);
                f(g, new Color(50, 100, 220), x + 31, y + 22, 9, 4);
                f(g, new Color(30, 30, 36), x + 30, y + 28, 3, 2);
                f(g, new Color(30, 30, 36), x + 39, y + 28, 3, 2);
                break;
            }
            case REPAIR_SHOP: {
                f(g, new Color(232, 176, 50), x, y + 9, w, h - 9);
                f(g, new Color(70, 74, 88), x - 1, y, w + 2, 9);
                f(g, new Color(100, 104, 120), x - 1, y, w + 2, 2);
                for (int b = 0; b < 2; b++) {
                    int bx = x + 4 + b * 24;
                    f(g, new Color(52, 56, 70), bx, y + 13, 18, h - 13);
                    for (int yy = y + 15; yy < y + h; yy += 3) f(g, new Color(84, 90, 108), bx + 1, yy, 16, 1);
                }
                f(g, new Color(30, 30, 34), x + 22, y + h - 6, 4, 6);
                f(g, new Color(60, 60, 66), x + 23, y + h - 5, 2, 4);
                break;
            }
            case PAINT_SHOP: {
                f(g, new Color(72, 162, 172), x, y + 9, w, h - 9);
                f(g, new Color(110, 62, 150), x - 1, y, w + 2, 9);
                f(g, new Color(150, 100, 190), x - 1, y, w + 2, 2);
                Color[] cols = { new Color(220, 50, 50), new Color(240, 190, 40), new Color(50, 110, 220), new Color(60, 170, 90), new Color(240, 120, 40) };
                for (int i = 0; i < 8; i++) {
                    int bx = x + 3 + i * 6;
                    f(g, cols[i % 5], bx, y + 11, 5, 6 + (i * 5) % 6);
                }
                f(g, new Color(60, 70, 100), x + 17, y + 20, 16, h - 20);
                f(g, new Color(160, 215, 240), x + 18, y + 21, 14, h - 21);
                f(g, new Color(60, 70, 100), x + 24, y + 21, 2, h - 21);
                break;
            }
            case RACE_HQ: {
                f(g, new Color(66, 86, 150), x, y + 9, w, h - 9);
                f(g, new Color(28, 38, 80), x - 1, y, w + 2, 9);
                f(g, new Color(60, 76, 130), x - 1, y, w + 2, 2);
                for (int row = 0; row < 2; row++) {
                    for (int col = 0; col < 7; col++) {
                        boolean lit = (row * 7 + col) % 3 != 1;
                        f(g, lit ? new Color(250, 222, 120) : new Color(130, 176, 232), x + 3 + col * 6, y + 12 + row * 6, 4, 4);
                    }
                }
                f(g, new Color(40, 50, 90), x + 20, y + h - 9, 10, 9);
                f(g, new Color(160, 200, 240), x + 21, y + h - 8, 3, 7);
                f(g, new Color(160, 200, 240), x + 26, y + h - 8, 3, 7);
                f(g, new Color(180, 180, 190), x + w - 8, y - 8, 1, 9);
                for (int fy = 0; fy < 2; fy++) for (int fx = 0; fx < 3; fx++) {
                    f(g, (fx + fy) % 2 == 0 ? Color.BLACK : Color.WHITE, x + w - 7 + fx * 2, y - 8 + fy * 2, 2, 2);
                }
                break;
            }
            case RACE_TRACK: {
                f(g, new Color(132, 136, 150), x, y + 9, w, h - 9);
                f(g, new Color(190, 50, 50), x - 1, y, w + 2, 9);
                f(g, new Color(230, 90, 90), x - 1, y, w + 2, 2);
                Color[] shirts = { new Color(220, 60, 60), new Color(60, 110, 220), new Color(240, 200, 50), Color.WHITE, new Color(70, 180, 100) };
                for (int row = 0; row < 2; row++) {
                    f(g, new Color(104, 108, 124), x, y + 11 + row * 6, w, 1);
                    for (int i = 0; i < 12; i++) f(g, shirts[(i * 3 + row * 2) % 5], x + 3 + i * 4, y + 12 + row * 6, 2, 3);
                }
                f(g, new Color(30, 30, 36), x + 15, y + h - 10, 20, 10);
                for (int i = 0; i < 10; i++) {
                    f(g, i % 2 == 0 ? Color.BLACK : Color.WHITE, x + 15 + i * 2, y + h - 13, 2, 3);
                    f(g, i % 2 == 0 ? Color.WHITE : Color.BLACK, x + 15 + i * 2, y + h - 16, 2, 3);
                }
                break;
            }
            case CHAMPIONSHIP: {
                f(g, new Color(224, 214, 190), x, y + 9, w, h - 9);
                f(g, new Color(120, 30, 50), x - 1, y, w + 2, 9);
                f(g, new Color(200, 160, 50), x - 1, y, w + 2, 2);
                for (int i = 0; i < 5; i++) {              // columns
                    f(g, new Color(250, 246, 234), x + 4 + i * 9, y + 11, 4, h - 11);
                    f(g, new Color(190, 182, 160), x + 7 + i * 9, y + 11, 1, h - 11);
                }
                f(g, new Color(120, 30, 50), x + 20, y + h - 10, 10, 10);       // red carpet door
                f(g, new Color(240, 200, 60), x + 22, y + h - 8, 6, 8);
                // golden trophy on the roof
                f(g, new Color(240, 200, 50), x + 22, y - 6, 6, 4);
                f(g, new Color(240, 200, 50), x + 24, y - 2, 2, 3);
                f(g, new Color(255, 235, 140), x + 23, y - 5, 1, 2);
                break;
            }
            case PRACTICE: {
                f(g, new Color(96, 100, 112), x, y + 9, w, h - 9);
                f(g, new Color(230, 120, 40), x - 1, y, w + 2, 9);
                f(g, new Color(255, 170, 90), x - 1, y, w + 2, 2);
                // big stopwatch
                f(g, new Color(250, 250, 250), x + 4, y + 12, 14, 14);
                f(g, new Color(40, 40, 50), x + 5, y + 13, 12, 12);
                f(g, new Color(250, 250, 250), x + 6, y + 14, 10, 10);
                f(g, new Color(220, 50, 50), x + 11, y + 15, 1, 5);
                f(g, new Color(40, 40, 50), x + 11, y + 19, 4, 1);
                f(g, new Color(200, 200, 210), x + 10, y + 10, 4, 2);
                // garage door
                f(g, new Color(52, 52, 62), x + 24, y + 13, 22, h - 13);
                for (int yy = y + 14; yy < y + h; yy += 3) f(g, new Color(190, 194, 206), x + 25, yy, 20, 2);
                // checkered strip
                for (int i = 0; i < 12; i++) f(g, i % 2 == 0 ? Color.BLACK : Color.WHITE, x + 2 + i * 4, y + h - 3, 4, 3);
                break;
            }
            case KNOCKOUT: {
                f(g, new Color(46, 40, 56), x, y + 9, w, h - 9);
                f(g, new Color(180, 30, 40), x - 1, y, w + 2, 9);
                f(g, new Color(250, 90, 90), x - 1, y, w + 2, 2);
                // arena arch with spotlights
                f(g, new Color(22, 18, 28), x + 12, y + 12, 26, h - 12);
                f(g, new Color(255, 230, 120), x + 14, y + 14, 22, 3);
                for (int i = 0; i < 4; i++) f(g, new Color(255, 245, 190, 200), x + 5 + i * 13, y + 11, 3, 3);
                // red X on both sides
                for (int k = 0; k < 6; k++) {
                    f(g, new Color(230, 50, 60), x + 3 + k, y + 14 + k, 2, 2);
                    f(g, new Color(230, 50, 60), x + 8 - k, y + 14 + k, 2, 2);
                    f(g, new Color(230, 50, 60), x + w - 10 + k, y + 14 + k, 2, 2);
                    f(g, new Color(230, 50, 60), x + w - 5 - k, y + 14 + k, 2, 2);
                }
                f(g, new Color(255, 255, 255), x + 18, y + h - 8, 14, 1);
                break;
            }
            case STREET: {
                f(g, new Color(52, 40, 70), x, y + 9, w, h - 9);
                f(g, new Color(30, 22, 44), x - 1, y, w + 2, 9);
                f(g, new Color(255, 70, 180), x - 1, y, w + 2, 1);
                Color[] neon = { new Color(255, 70, 180), new Color(60, 230, 240), new Color(250, 230, 70) };
                for (int i = 0; i < 6; i++) f(g, neon[i % 3], x + 3 + i * 8, y + 11, 6, 1);
                // graffiti
                f(g, new Color(255, 70, 180), x + 3, y + 15, 12, 2);
                f(g, new Color(60, 230, 240), x + 5, y + 19, 9, 2);
                f(g, new Color(250, 230, 70), x + 3, y + 23, 7, 2);
                // half-open shutter with a car peeking out
                f(g, new Color(70, 72, 86), x + 22, y + 13, 24, h - 13);
                for (int yy = y + 13; yy < y + 21; yy += 2) f(g, new Color(110, 112, 130), x + 23, yy, 22, 1);
                f(g, new Color(20, 20, 26), x + 23, y + 21, 22, h - 21);
                f(g, new Color(230, 50, 60), x + 27, y + 25, 14, 4);
                f(g, new Color(250, 240, 170), x + 40, y + 26, 2, 2);
                break;
            }
            case POLICE: {
                f(g, new Color(218, 224, 238), x, y + 9, w, h - 9);
                f(g, new Color(26, 44, 110), x - 1, y, w + 2, 9);
                f(g, new Color(70, 100, 200), x - 1, y, w + 2, 2);
                // light bar on the roof
                f(g, new Color(230, 50, 50), x + 18, y - 4, 7, 4);
                f(g, new Color(60, 110, 255), x + 25, y - 4, 7, 4);
                f(g, new Color(255, 255, 255, 90), x + 16, y - 6, 18, 2);
                // windows
                for (int i = 0; i < 4; i++) f(g, new Color(110, 150, 220), x + 4 + i * 7, y + 12, 5, 5);
                for (int i = 0; i < 4; i++) f(g, new Color(110, 150, 220), x + 33 + i * 4, y + 12, 3, 5);
                // gold badge
                f(g, new Color(240, 200, 60), x + 6, y + 20, 10, 10);
                f(g, new Color(240, 200, 60), x + 8, y + 18, 6, 2);
                f(g, new Color(150, 110, 20), x + 9, y + 23, 4, 4);
                // double door and steps
                f(g, new Color(40, 50, 90), x + 20, y + h - 13, 14, 13);
                f(g, new Color(150, 190, 240), x + 21, y + h - 12, 5, 11);
                f(g, new Color(150, 190, 240), x + 28, y + h - 12, 5, 11);
                f(g, new Color(150, 156, 170), x + 18, y + h - 2, 18, 2);
                // parked patrol car
                f(g, new Color(240, 240, 245), x + 37, y + 22, 11, 7);
                f(g, new Color(30, 30, 38), x + 37, y + 25, 11, 2);
                f(g, new Color(230, 50, 50), x + 40, y + 20, 3, 2);
                f(g, new Color(60, 110, 255), x + 43, y + 20, 3, 2);
                break;
            }
            default: { // DEMOLITION DERBY
                f(g, new Color(96, 78, 62), x, y + 9, w, h - 9);
                f(g, new Color(150, 50, 20), x - 1, y, w + 2, 9);
                f(g, new Color(240, 130, 50), x - 1, y, w + 2, 2);
                // flames painted along the wall
                for (int i = 0; i < 6; i++) {
                    f(g, new Color(250, 150, 30), x + 3 + i * 8, y + 11, 5, 5);
                    f(g, new Color(250, 220, 70), x + 4 + i * 8, y + 13, 3, 3);
                    f(g, new Color(230, 70, 30), x + 5 + i * 8, y + 9, 1, 3);
                }
                // arena gate with a crashed car on top
                f(g, new Color(26, 20, 18), x + 14, y + 19, 22, h - 19);
                f(g, new Color(150, 112, 72), x + 15, y + 22, 20, h - 22);
                for (int i = 0; i < 5; i++) f(g, i % 2 == 0 ? new Color(214, 54, 54) : Color.WHITE, x + 14 + i * 5, y + 18, 5, 2);
                f(g, new Color(200, 170, 60), x + 30, y - 5, 12, 5);
                f(g, new Color(150, 120, 40), x + 32, y - 8, 7, 3);
                f(g, new Color(30, 30, 38), x + 31, y - 1, 3, 2);
                f(g, new Color(30, 30, 38), x + 38, y - 1, 3, 2);
                f(g, new Color(250, 140, 40), x + 36, y - 10, 2, 3);
                // tyre stacks
                for (int k = 0; k < 2; k++) {
                    int tx = k == 0 ? x + 2 : x + w - 11;
                    for (int i = 0; i < 3; i++) {
                        f(g, new Color(20, 20, 24), tx, y + h - 6 - i * 5, 9, 5);
                        f(g, new Color(80, 80, 88), tx + 3, y + h - 5 - i * 5, 3, 2);
                    }
                }
                break;
            }
        }
        // soft shadow along the bottom
        f(g, shadow, x, y + h, w, 2);
    }
}
