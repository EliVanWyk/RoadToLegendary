package racing;

import static racing.GameWindow.box;
import static racing.GameWindow.fill;
import static racing.GameWindow.font;
import static racing.GameWindow.text;
import static racing.GameWindow.textCenter;
import static racing.GameWindow.wrap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;

/**
 * The "FIRST TIME HERE" tutorial: a few pages with pictures that explain the
 * city, the controls, the driving tricks and the money. It can be opened again
 * any time from the HOW TO PLAY buttons.
 */
class TutorialScreen extends JPanel implements GameWindow.Screen {

    private static final int PAGES = 6;
    private final GameWindow window;
    private int page = 0;
    private boolean firstTime = true;
    private final GameWindow.PixelButton next, back, skip;

    TutorialScreen(GameWindow window) {
        this.window = window;
        setLayout(null);
        setPreferredSize(new Dimension(GameWindow.W, GameWindow.H));
        back = new GameWindow.PixelButton("< BACK", new Color(70, 90, 160));
        back.setBounds(150, 536, 170, 46);
        back.addActionListener(e -> { page--; refresh(); });
        add(back);
        skip = new GameWindow.PixelButton("SKIP TUTORIAL", new Color(190, 70, 60));
        skip.setBounds(390, 536, 180, 46);
        skip.addActionListener(e -> window.finishTutorial());
        add(skip);
        next = new GameWindow.PixelButton("NEXT >", new Color(60, 170, 80));
        next.setBounds(640, 536, 170, 46);
        next.addActionListener(e -> {
            if (page >= PAGES - 1) window.finishTutorial();
            else { page++; refresh(); }
        });
        add(next);
    }

    void setFirstTime(boolean first) { firstTime = first; }

    public void onShow() {
        page = 0;
        Sound.playMusic(Sound.MUSIC_MENU);
        refresh();
    }

    private void refresh() {
        back.setEnabled(page > 0);
        next.setText(page >= PAGES - 1 ? (firstTime ? "LET'S GO!" : "DONE") : "NEXT >");
        skip.setText(firstTime ? "SKIP TUTORIAL" : "CLOSE");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < 20; i++) fill(g2, new Color(18 + i * 2, 24 + i * 2, 56 + i * 4), 0, i * 32, GameWindow.W, 32);
        Random r = new Random(4);
        for (int i = 0; i < 60; i++) fill(g2, new Color(220, 225, 255, 150), r.nextInt(GameWindow.W), r.nextInt(GameWindow.H), 3, 3);
        box(g2, 120, 40, 720, 560, new Color(18, 22, 46, 244), new Color(255, 230, 120));

        String name = window.getDriver() == null ? "RACER" : window.getDriver().getName().toUpperCase();
        String[] titles = { "WELCOME, " + name + "!", "THE CITY", "YOUR FIRST RACE", "DRIVING TRICKS", "MONEY AND CARS", "MORE WAYS TO PLAY" };
        String[] body = {
            "Your dream: become a LEGENDARY racer! Win races, earn money, buy and upgrade cars, and climb through 5 ranks:\nBeginner, Amateur, Professional, Expert and Legendary.",
            "The city is your main menu. Move the mouse over a building to see what it does, then click to go inside.\nBuildings with a padlock open up when you win races and become famous.",
            "Go to the RACE TRACK and choose race #1. You must WIN a race to unlock the next one. Win all 5 races of a rank to move up.\nDrive with the keys below (ESC pauses the race).",
            "SHIFT = DRIFT: hold it while steering. The blue boost bar fills up. Let go for a burst of speed!\nSPACE = NITRO: a big boost, then it needs time to recharge.\nStay close behind another car for a SLIPSTREAM speed boost. Watch out for WET, GRAVEL and ICY roads: they have less grip.",
            "Prize money from races lets you shop. Buy faster cars in the CAR SHOP, upgrade them in the GARAGE and fix crash damage at the REPAIR SHOP.\nA damaged car is slower and harder to steer, so keep an eye on the CAR % number!",
            "CHAMPIONSHIP HALL: cups with 4 races and a points table.\nPRACTICE GARAGE: time trials. Medals give skill points to train your driving.\nKNOCKOUT ARENA: don't finish last! Or beat the clock in Checkpoint Rush.\nSTREET RACING: night races with traffic. Bet money on yourself!\nPOLICE CHASE: outrun the cops before the BUSTED meter fills!\nDEMOLITION DERBY: buy a junker for one contest and be the last car standing!\nRACE HQ: sponsor challenges for bonus cash and fame.\nThe game saves by itself. Change sound, music and more in SETTINGS."
        };
        textCenter(g2, titles[page], 480, 92, 32, new Color(255, 240, 170));
        drawArt(g2, page);

        FontMetrics fm = g2.getFontMetrics(font(16));
        int y = page == 5 ? 150 : 320;
        List<String> lines = wrap(body[page], fm, 650);
        for (String s : lines) {
            text(g2, s, 150, y, 16, Color.WHITE, false);
            y += 24;
        }
        for (int i = 0; i < PAGES; i++) fill(g2, i == page ? new Color(255, 240, 170) : new Color(80, 90, 130), 440 + i * 16, 512, 10, 10);
    }

    // ---- little pictures for each page ----
    private void key(Graphics2D g, String label, int x, int y, int w) {
        fill(g, Color.BLACK, x, y, w, 40);
        fill(g, new Color(210, 214, 226), x + 3, y + 3, w - 6, 30);
        fill(g, new Color(150, 154, 170), x + 3, y + 30, w - 6, 7);
        FontMetrics fm = g.getFontMetrics(font(13));
        text(g, label, x + (w - fm.stringWidth(label)) / 2, y + 25, 13, new Color(30, 30, 40), false);
    }

    private void drawArt(Graphics2D g, int p) {
        switch (p) {
            case 0: {
                java.util.ArrayList<Car> cat = CarShop.getCatalog();
                g.drawImage(cat.get(0).getLargeImage(), 170, 150, 170, 72, null);
                g.drawImage(cat.get(9).getLargeImage(), 395, 130, 170, 72, null);
                g.drawImage(cat.get(14).getLargeImage(), 620, 150, 170, 72, null);
                fill(g, new Color(250, 214, 60), 450, 220, 60, 8);
                fill(g, new Color(250, 214, 60), 466, 232, 28, 20);
                fill(g, new Color(250, 214, 60), 456, 254, 48, 10);
                text(g, "BEGINNER", 190, 250, 13, new Color(190, 200, 230), false);
                text(g, "EXPERT", 435, 292, 13, new Color(190, 200, 230), false);
                text(g, "LEGENDARY", 640, 250, 13, new Color(255, 214, 60), false);
                break;
            }
            case 1: {
                Color[] cols = { new Color(168, 88, 66), new Color(40, 70, 160), new Color(232, 176, 50), new Color(110, 62, 150),
                                 new Color(66, 86, 150), new Color(190, 50, 50) };
                for (int i = 0; i < 6; i++) {
                    int x = 170 + i * 100, yb = 250;
                    fill(g, Color.BLACK, x - 2, yb - 62, 84, 66);
                    fill(g, cols[i], x, yb - 60, 80, 62);
                    fill(g, new Color(255, 255, 255, 40), x, yb - 60, 80, 14);
                    fill(g, new Color(50, 50, 62), x + 28, yb - 28, 24, 30);
                    fill(g, new Color(160, 200, 240), x + 8, yb - 38, 14, 12);
                    fill(g, new Color(160, 200, 240), x + 58, yb - 38, 14, 12);
                }
                // mouse pointer and tooltip
                fill(g, Color.BLACK, 556, 192, 4, 30);
                fill(g, Color.WHITE, 558, 196, 4, 24);
                box(g, 580, 168, 190, 64, new Color(24, 28, 56), new Color(255, 230, 120));
                text(g, "RACE TRACK", 592, 192, 13, new Color(255, 240, 170), true);
                text(g, "Click to enter!", 592, 214, 12, new Color(120, 240, 130), false);
                break;
            }
            case 2: {
                key(g, "UP", 292, 130, 76);  text(g, "GAS", 380, 156, 15, Color.WHITE, false);
                key(g, "DOWN", 292, 176, 76); text(g, "BRAKE", 380, 202, 15, Color.WHITE, false);
                key(g, "LEFT", 150, 176, 76);
                key(g, "RIGHT", 440, 176, 90);
                text(g, "STEER", 560, 202, 15, Color.WHITE, false);
                key(g, "W", 640, 130, 40); key(g, "A", 592, 176, 40); key(g, "S", 640, 176, 40); key(g, "D", 688, 176, 40);
                text(g, "or WASD", 740, 202, 13, new Color(190, 200, 230), false);
                text(g, "ESC = pause", 640, 250, 14, new Color(190, 200, 230), false);
                break;
            }
            case 3: {
                key(g, "SHIFT", 160, 130, 110);
                fill(g, Color.BLACK, 160, 184, 200, 18);
                fill(g, new Color(90, 220, 255), 162, 186, 130, 14);
                text(g, "DRIFT BOOST", 160, 224, 14, new Color(90, 220, 255), false);
                key(g, "SPACE", 470, 130, 130);
                fill(g, Color.BLACK, 470, 184, 200, 18);
                fill(g, new Color(255, 160, 40), 472, 186, 180, 14);
                text(g, "NITRO!", 470, 224, 14, new Color(255, 170, 60), false);
                // slipstream: two cars
                java.util.ArrayList<Car> cat = CarShop.getCatalog();
                g.drawImage(cat.get(6).getRaceImage(), 760, 112, 33, 60, null);
                g.drawImage(cat.get(3).getRaceImage(), 760, 184, 33, 60, null);
                for (int i = 0; i < 3; i++) fill(g, new Color(255, 255, 255, 180), 748 + i * 20, 190 + (i % 2) * 8, 2, 20);
                text(g, "SLIPSTREAM", 690, 270, 13, new Color(255, 255, 255), false);
                break;
            }
            case 4: {
                text(g, "$", 190, 235, 80, new Color(120, 240, 130), true);
                java.util.ArrayList<Car> cat = CarShop.getCatalog();
                g.drawImage(cat.get(2).getLargeImage(), 300, 150, 150, 62, null);
                g.drawImage(cat.get(8).getLargeImage(), 480, 150, 150, 62, null);
                fill(g, Color.BLACK, 666, 182, 120, 16);
                fill(g, new Color(80, 210, 100), 668, 184, 90, 12);
                text(g, "CAR 75%", 666, 226, 14, Color.WHITE, false);
                break;
            }
            default: break;
        }
    }
}
