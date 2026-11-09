package racing;

import static racing.GameWindow.box;
import static racing.GameWindow.font;
import static racing.GameWindow.money;
import static racing.GameWindow.text;
import static racing.GameWindow.textCenter;
import static racing.GameWindow.textRight;
import static racing.GameWindow.wrap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;

/**
 * Base class for the new building screens (Championship Hall, Practice Garage,
 * Knockout Arena, Street Racing Spot and the Sponsor board).
 * It draws the pixel-art interior, the name sign, the player and money bars
 * and a message area - just like the original building screens do.
 */
abstract class ScreenBase extends JPanel implements GameWindow.Screen {

    protected final GameWindow window;
    protected final int id;
    protected String status = "";

    ScreenBase(GameWindow window, int id) {
        this.window = window;
        this.id = id;
        setLayout(null);
        setPreferredSize(new Dimension(GameWindow.W, GameWindow.H));
    }

    protected Driver driver() { return window.getDriver(); }
    protected Career career() { return window.getCareer(); }

    protected GameWindow.PixelButton addButton(String text, Color color, int x, int y, int w, int h, final Runnable action) {
        GameWindow.PixelButton b = new GameWindow.PixelButton(text, color);
        b.setBounds(x, y, w, h);
        b.addActionListener(e -> action.run());
        add(b);
        return b;
    }

    protected GameWindow.PixelButton addBackButton(int x, int y, int w, int h) {
        return addButton("BACK TO CITY", new Color(190, 70, 60), x, y, w, h, () -> window.showCity());
    }

    protected String title() { return CityMap.BUILDING_NAMES[id]; }

    public void onShow() {
        status = "";
        Sound.playMusic(Sound.MUSIC_MENU);
        refresh();
        repaint();
    }

    protected void refresh() { }

    protected void setStatus(String s) {
        status = s;
        refresh();
        repaint();
    }

    protected Rectangle statusBounds() { return new Rectangle(24, 544, 364, 60); }

    protected void paintContent(Graphics2D g) { }

    /** Warns when the car is badly damaged. Returns true if the race should go ahead. */
    protected boolean checkCar() {
        Car c = driver().getCurrentCar();
        if (c.getCondition() < 40) {
            return window.confirm("DAMAGED CAR", "Your " + c.getName() + " is only at " + c.getCondition()
                    + "% condition and will perform worse. Race anyway?", "RACE", "WAIT");
        }
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        Graphics2D low = (Graphics2D) g2.create();
        low.scale(4, 4);
        switch (id) {
            case CityMap.RACE_HQ: Career.paintHeadquarters(low); break;
            default: Interiors.paint(low, id); break;
        }
        low.dispose();

        Color[] signColors = new Color[CityMap.BUILDING_COUNT];
        for (int i = 0; i < signColors.length; i++) signColors[i] = new Color(60, 64, 90);
        signColors[CityMap.RACE_HQ] = new Color(28, 38, 80);
        signColors[CityMap.CHAMPIONSHIP] = new Color(120, 30, 50);
        signColors[CityMap.PRACTICE] = new Color(170, 90, 30);
        signColors[CityMap.KNOCKOUT] = new Color(160, 30, 40);
        signColors[CityMap.STREET] = new Color(70, 30, 100);
        signColors[CityMap.POLICE] = new Color(26, 44, 110);
        signColors[CityMap.DERBY] = new Color(140, 60, 20);
        String t = title();
        int size = t.length() > 15 ? 24 : 30;
        box(g2, 270, 10, 420, 56, signColors[id], new Color(255, 230, 120));
        textCenter(g2, t, 480, 48, size, new Color(255, 240, 170));

        Driver d = driver();
        box(g2, 12, 10, 250, 56, new Color(22, 26, 54), new Color(120, 160, 255));
        text(g2, d.getName().toUpperCase(), 28, 36, 20, new Color(255, 240, 170), true);
        text(g2, d.getRankName(), 28, 56, 14, Color.WHITE, false);
        box(g2, 698, 10, 250, 56, new Color(22, 26, 54), new Color(120, 160, 255));
        textRight(g2, money(d.getMoney()), 932, 48, 30, new Color(120, 240, 130));

        paintContent(g2);

        if (status != null && !status.isEmpty()) {
            Rectangle r = statusBounds();
            box(g2, r.x, r.y, r.width, r.height, new Color(22, 26, 54), new Color(255, 220, 90));
            FontMetrics fm = g2.getFontMetrics(font(16));
            List<String> lines = wrap(status, fm, r.width - 36);
            int ty = r.y + 30 - (lines.size() > 1 ? 9 : 0);
            for (int i = 0; i < lines.size() && i < 2; i++) text(g2, lines.get(i), r.x + 18, ty + i * 20, 16, Color.WHITE, false);
        }
    }
}
