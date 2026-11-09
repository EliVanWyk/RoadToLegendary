package racing;

import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * The main window. It owns the screens and switches between them
 * (title -> city -> building screens -> race -> back to the city).
 *
 * It also holds the small "pixel UI toolkit" (pixel buttons, boxes, text,
 * dialogs) so every screen looks like it belongs to the same game.
 */
public class GameWindow extends JFrame {

    public static final int W = 960;
    public static final int H = 640;

    /** Every screen implements this so the window can tell it "you are now visible". */
    public interface Screen {
        void onShow();
    }

    private Career career;
    private Driver driver;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Map<String, Screen> screens = new HashMap<String, Screen>();
    private RaceScreen raceScreen;
    private SettingsScreen settingsScreen;
    private TutorialScreen tutorialScreen;
    private DerbyPlayScreen derbyScreen;
    private int currentSlot = 0;
    private String currentScreen = "title";
    private String settingsReturn = "title";
    private String tutorialReturn = "city";

    public GameWindow(Career career) {
        super("Road to Legendary");
        this.career = career;
        Settings.load();
        Sound.init();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        root.setPreferredSize(new Dimension(W, H));
        add(root);
        addScreen("title", new TitleScreen());
        settingsScreen = new SettingsScreen(this);
        addScreen("settings", settingsScreen);
        tutorialScreen = new TutorialScreen(this);
        addScreen("tutorial", tutorialScreen);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) { autosave(); }
        });
        pack();
        setLocationRelativeTo(null);
        show("title");
    }

    public Driver getDriver() { return driver; }
    public Career getCareer() { return career; }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------
    private <T extends JPanel & Screen> void addScreen(String name, T panel) {
        root.add(panel, name);
        screens.put(name, panel);
    }

    private void show(String name) {
        String prev = currentScreen;
        boolean skipSave = "race".equals(name) || "derby".equals(name) || "title".equals(name)
                || (("race".equals(prev) || "derby".equals(prev)) && "settings".equals(name));
        if (!skipSave) autosave();
        currentScreen = name;
        cards.show(root, name);
        Screen s = screens.get(name);
        if (s != null) s.onShow();
    }

    /** Opens any screen by name ("city", "b6" for building 6...). */
    public void showScreen(String name) { show(name); }

    /** Saves the game into the slot being played (called automatically when moving between screens). */
    private void autosave() {
        if (driver == null || currentSlot < 1 || "race".equals(currentScreen) || "derby".equals(currentScreen)) return;
        SaveGame.save(currentSlot, career, driver);
    }

    /** Saves right now. Returns true if it worked. */
    public boolean saveNow() {
        return driver != null && currentSlot >= 1 && SaveGame.save(currentSlot, career, driver);
    }

    /** A brand-new career in a slot. */
    private void startGame(int slot, String name, boolean firstTime) {
        currentSlot = slot;
        career = new Career();
        driver = new Driver(name);
        enterGame(firstTime);
        SaveGame.save(currentSlot, career, driver);
    }

    /** Loads the saved game of a slot. */
    public void loadGame(int slot) {
        java.util.Properties p = SaveGame.read(slot);
        if (p == null) {
            message("LOAD FAILED", "Save slot " + slot + " could not be read.");
            return;
        }
        currentSlot = slot;
        career = new Career();
        career.readFrom(p);
        driver = Driver.readFrom(p);
        enterGame(false);
    }

    /** Creates all the game screens for the current driver and opens the city (or the tutorial). */
    private void enterGame(boolean firstTime) {
        for (Component c : root.getComponents()) {
            if (!(c instanceof TitleScreen) && !(c instanceof SettingsScreen) && !(c instanceof TutorialScreen)
                    ) root.remove(c);
        }
        screens.clear();
        screens.put("title", (Screen) findTitle());
        screens.put("settings", settingsScreen);
        screens.put("tutorial", tutorialScreen);
        addScreen("city", new CityMap(this));
        addScreen("b" + CityMap.GARAGE, new GarageScreen());
        addScreen("b" + CityMap.CAR_SHOP, new ShopScreen());
        addScreen("b" + CityMap.REPAIR_SHOP, new RepairScreen());
        addScreen("b" + CityMap.PAINT_SHOP, new PaintScreen());
        addScreen("b" + CityMap.RACE_HQ, new RaceSelectScreen(true));
        addScreen("b" + CityMap.RACE_TRACK, new RaceSelectScreen(false));
        addScreen("b" + CityMap.CHAMPIONSHIP, new ChampionshipScreen(this));
        addScreen("b" + CityMap.PRACTICE, new PracticeScreen(this));
        addScreen("b" + CityMap.KNOCKOUT, new KnockoutScreen(this));
        addScreen("b" + CityMap.STREET, new StreetScreen(this));
        addScreen("b" + CityMap.POLICE, new PoliceScreen(this));
        addScreen("b" + CityMap.DERBY, new DerbyShopScreen(this));
        derbyScreen = new DerbyPlayScreen(this);
        addScreen("derby", derbyScreen);
        addScreen("sponsors", new SponsorScreen(this));
        raceScreen = new RaceScreen(this);
        addScreen("race", raceScreen);
        if (firstTime) showTutorial(true);
        else showCity();
    }

    private Component findTitle() {
        for (Component c : root.getComponents()) if (c instanceof TitleScreen) return c;
        return null;
    }

    public void showCity() { show("city"); }
    public void showBuilding(int id) { show("b" + id); }

    /** Starts a race. "origin" is the screen to come back to afterwards. */
    public void startRace(Race race, int wager, String origin) {
        raceScreen.begin(race, wager, origin);
        show("race");
    }

    /** Starts a demolition derby with the car the player just bought. */
    public void startDerby(DerbyCar car, String origin) {
        derbyScreen.begin(car, origin);
        show("derby");
    }

    public void showSettings() {
        if (!"settings".equals(currentScreen)) settingsReturn = currentScreen;
        show("settings");
    }

    public void closeSettings() {
        Settings.save();
        show(settingsReturn);
    }

    /** firstTime = the tutorial that comes right after choosing FIRST TIME HERE. */
    public void showTutorial(boolean firstTime) {
        if (!"tutorial".equals(currentScreen)) tutorialReturn = currentScreen;
        tutorialScreen.setFirstTime(firstTime);
        if (firstTime) tutorialReturn = "city";
        show("tutorial");
    }

    public void finishTutorial() {
        show(tutorialReturn);
    }

    /** Deletes all progress in the current slot and starts a brand-new career in it. */
    public void resetCareer() {
        if (driver == null) return;
        String name = driver.getName();
        int slot = currentSlot;
        SaveGame.delete(slot);
        startGame(slot, name, false);
    }

    /** Saves and goes back to the three save slots. */
    public void backToSlots() {
        autosave();
        career = new Career();
        driver = null;
        currentSlot = 0;
        Sound.playMusic(Sound.MUSIC_MENU);
        Component t = findTitle();
        if (t instanceof TitleScreen) ((TitleScreen) t).reset();
        show("title");
    }

    // ------------------------------------------------------------------
    // Pixel UI toolkit (static helpers used by every screen)
    // ------------------------------------------------------------------
    public static void fill(Graphics2D g, Color c, int x, int y, int w, int h) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    public static Font font(int size) {
        return new Font(Font.MONOSPACED, Font.BOLD, size);
    }

    /** A chunky pixel panel: black outline, coloured border, dark fill. */
    public static void box(Graphics2D g, int x, int y, int w, int h, Color fillColor, Color border) {
        g.setColor(Color.BLACK);
        g.fillRect(x, y, w, h);
        g.setColor(border);
        g.fillRect(x + 3, y + 3, w - 6, h - 6);
        g.setColor(fillColor);
        g.fillRect(x + 6, y + 6, w - 12, h - 12);
        g.setColor(new Color(255, 255, 255, 28));
        g.fillRect(x + 6, y + 6, w - 12, 3);
    }

    public static void text(Graphics2D g, String s, int x, int y, int size, Color c, boolean shadow) {
        g.setFont(font(size));
        if (shadow) {
            g.setColor(new Color(0, 0, 0, 170));
            g.drawString(s, x + 2, y + 2);
        }
        g.setColor(c);
        g.drawString(s, x, y);
    }

    public static void textCenter(Graphics2D g, String s, int cx, int y, int size, Color c) {
        FontMetrics fm = g.getFontMetrics(font(size));
        text(g, s, cx - fm.stringWidth(s) / 2, y, size, c, true);
    }

    public static void textRight(Graphics2D g, String s, int rx, int y, int size, Color c) {
        FontMetrics fm = g.getFontMetrics(font(size));
        text(g, s, rx - fm.stringWidth(s), y, size, c, true);
    }

    public static String money(int v) {
        return String.format("$%,d", v);
    }

    public static String ordinal(int n) {
        if (n == 1) return "1ST";
        if (n == 2) return "2ND";
        if (n == 3) return "3RD";
        return n + "TH";
    }

    /** Splits text into lines that fit a width (also respects \n). */
    public static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<String>();
        for (String para : text.split("\n")) {
            String line = "";
            for (String word : para.split(" ")) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                    lines.add(line);
                    line = word;
                } else {
                    line = test;
                }
            }
            lines.add(line);
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // Pixel button
    // ------------------------------------------------------------------
    public static class PixelButton extends JButton {
        private Color base;

        public PixelButton(String text, Color base) {
            super(text);
            this.base = base;
            setFocusable(false);               // keeps the arrow keys free for racing
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(font(16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> Sound.play(Sound.CLICK));
        }

        public void setBase(Color c) { base = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            boolean pressed = getModel().isArmed() && getModel().isPressed();
            Color c = on ? base : new Color(92, 94, 106);
            if (on && getModel().isRollover()) c = c.brighter();
            Color light = pressed ? c.darker() : c.brighter();
            Color dark = pressed ? c.brighter() : c.darker();

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);
            g2.setColor(c);
            g2.fillRect(3, 3, w - 6, h - 6);
            g2.setColor(light);
            g2.fillRect(3, 3, w - 6, 3);
            g2.fillRect(3, 3, 3, h - 6);
            g2.setColor(dark);
            g2.fillRect(3, h - 6, w - 6, 3);
            g2.fillRect(w - 6, 3, 3, h - 6);

            // shrink the text if it is too long for the button
            int size = getFont().getSize();
            FontMetrics fm = g2.getFontMetrics(font(size));
            while (fm.stringWidth(getText()) > w - 16 && size > 9) {
                size--;
                fm = g2.getFontMetrics(font(size));
            }
            int lum = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
            Color textColor = lum > 170 ? new Color(20, 20, 30) : Color.WHITE;
            if (!on) textColor = new Color(170, 172, 184);
            g2.setFont(font(size));
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2 + (pressed ? 2 : 0);
            if (lum <= 170 && on) {
                g2.setColor(new Color(0, 0, 0, 140));
                g2.drawString(getText(), tx + 2, ty + 2);
            }
            g2.setColor(textColor);
            g2.drawString(getText(), tx, ty);
        }
    }

    // ------------------------------------------------------------------
    // Pixel dialogs (message boxes)
    // ------------------------------------------------------------------
    private int showDialog(String title, String msg, String... labels) {
        final JDialog dlg = new JDialog(this, true);
        dlg.setUndecorated(true);
        final int[] result = { -1 };
        final int pw = 540;
        FontMetrics fm = getFontMetrics(font(16));
        final List<String> lines = wrap(msg, fm, pw - 64);
        final int ph = 120 + lines.size() * 22 + 64;
        final String dialogTitle = title;

        JPanel p = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                box(g2, 0, 0, pw, ph, new Color(22, 26, 52), new Color(255, 220, 90));
                textCenter(g2, dialogTitle, pw / 2, 56, 26, new Color(255, 230, 120));
                for (int i = 0; i < lines.size(); i++) {
                    text(g2, lines.get(i), 32, 100 + i * 22, 16, Color.WHITE, false);
                }
            }
        };
        p.setPreferredSize(new Dimension(pw, ph));
        int n = labels.length;
        int bw = (pw - 64 - (n - 1) * 12) / n;
        for (int i = 0; i < n; i++) {
            final int index = i;
            Color c = (i == 0) ? new Color(60, 160, 80) : new Color(190, 70, 60);
            PixelButton b = new PixelButton(labels[i], c);
            b.setBounds(32 + i * (bw + 12), ph - 70, bw, 46);
            b.addActionListener(e -> {
                result[0] = index;
                dlg.dispose();
            });
            p.add(b);
        }
        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        return result[0];
    }

    /** Asks for a name in a pixel-style dialog. Returns null if the player cancels. */
    public String askName(String title, String msg, String initial) {
        final JDialog dlg = new JDialog(this, true);
        dlg.setUndecorated(true);
        final String[] result = { null };
        final int pw = 520;
        FontMetrics fm = getFontMetrics(font(16));
        final List<String> lines = wrap(msg, fm, pw - 64);
        final int ph = 120 + lines.size() * 22 + 110;
        final String dialogTitle = title;
        JPanel p = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                box(g2, 0, 0, pw, ph, new Color(22, 26, 52), new Color(255, 220, 90));
                textCenter(g2, dialogTitle, pw / 2, 56, 26, new Color(255, 230, 120));
                for (int i = 0; i < lines.size(); i++) text(g2, lines.get(i), 32, 100 + i * 22, 16, Color.WHITE, false);
            }
        };
        p.setPreferredSize(new Dimension(pw, ph));
        final JTextField field = new JTextField(initial);
        field.setDocument(new PlainDocument() {
            @Override
            public void insertString(int off, String s, AttributeSet a) throws BadLocationException {
                if (s != null && getLength() + s.length() <= 14) super.insertString(off, s, a);
            }
        });
        field.setText(initial);
        field.setBounds(32, 100 + lines.size() * 22 + 6, pw - 64, 46);
        field.setFont(font(24));
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setBackground(new Color(18, 22, 46));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.BLACK, 3), new LineBorder(new Color(255, 214, 60), 3)));
        p.add(field);
        final Runnable ok = () -> {
            String t = field.getText().trim();
            if (!t.isEmpty()) {
                result[0] = t;
                dlg.dispose();
            }
        };
        field.addActionListener(e -> ok.run());
        PixelButton go = new PixelButton("OK", new Color(60, 160, 80));
        go.setBounds(32, ph - 70, (pw - 76) / 2, 46);
        go.addActionListener(e -> ok.run());
        p.add(go);
        PixelButton cancel = new PixelButton("CANCEL", new Color(190, 70, 60));
        cancel.setBounds(44 + (pw - 76) / 2, ph - 70, (pw - 76) / 2, 46);
        cancel.addActionListener(e -> dlg.dispose());
        p.add(cancel);
        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(() -> field.requestFocusInWindow());
        dlg.setVisible(true);
        return result[0];
    }

    /** A dialog with any number of buttons. Returns the index of the clicked button. */
    public int choose(String title, String msg, String... labels) {
        return showDialog(title, msg, labels);
    }

    public void message(String title, String msg) {
        showDialog(title, msg, "OK");
    }

    public boolean confirm(String title, String msg, String yes, String no) {
        return showDialog(title, msg, yes, no) == 0;
    }

    // ==================================================================
    // TITLE SCREEN (3 save slots)
    // ==================================================================
    private class TitleScreen extends JPanel implements Screen {
        private static final int CARD_W = 280, CARD_H = 452, CARD_Y = 138;
        private final PixelButton[] select = new PixelButton[SaveGame.SLOTS];
        private final PixelButton[] erase = new PixelButton[SaveGame.SLOTS];
        private final PixelButton settings;
        private final SaveGame.Info[] infos = new SaveGame.Info[SaveGame.SLOTS];

        TitleScreen() {
            setLayout(null);
            setPreferredSize(new Dimension(W, H));
            for (int i = 0; i < SaveGame.SLOTS; i++) {
                final int slot = i + 1;
                int x = cardX(i);
                select[i] = new PixelButton("SELECT", new Color(60, 170, 80));
                select[i].setFont(font(22));
                select[i].setBounds(x + 28, CARD_Y + CARD_H - 54, CARD_W - 56, 44);
                select[i].addActionListener(e -> chooseSlot(slot));
                add(select[i]);
                erase[i] = new PixelButton("X ERASE GAME X", new Color(170, 70, 70));
                erase[i].setFont(font(12));
                erase[i].setBounds(x + 28, CARD_Y + 96, CARD_W - 56, 26);
                erase[i].addActionListener(e -> eraseSlot(slot));
                add(erase[i]);
            }
            settings = new PixelButton("SETTINGS", new Color(70, 90, 160));
            settings.setFont(font(14));
            settings.setBounds(796, 18, 148, 38);
            settings.addActionListener(e -> showSettings());
            add(settings);
            refreshSlots();
        }

        private int cardX(int i) { return 30 + i * 310; }

        void refreshSlots() {
            for (int i = 0; i < SaveGame.SLOTS; i++) {
                infos[i] = SaveGame.info(i + 1);
                boolean used = infos[i] != null;
                select[i].setText(used ? "SELECT" : "NEW GAME");
                select[i].setBase(used ? new Color(60, 170, 80) : new Color(70, 110, 200));
                erase[i].setVisible(used);
            }
            repaint();
        }

        void reset() { refreshSlots(); }

        private void chooseSlot(int slot) {
            if (infos[slot - 1] != null) {
                loadGame(slot);
                return;
            }
            String name = askName("NEW GAME", "Welcome, new racer! What is your name?", "");
            if (name == null) return;
            startGame(slot, name, true);          // first time here: show the tutorial
        }

        private void eraseSlot(int slot) {
            SaveGame.Info in = infos[slot - 1];
            if (in == null) return;
            if (confirm("ERASE GAME?", "Erase the saved game of " + in.name.toUpperCase() + " in slot " + slot
                    + "?\nAll its money, cars and ranks will be gone for good.", "ERASE", "KEEP IT")) {
                SaveGame.delete(slot);
                refreshSlots();
            }
        }

        public void onShow() {
            Sound.playMusic(Sound.MUSIC_MENU);
            refreshSlots();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Graphics2D low = (Graphics2D) g2.create();
            low.scale(4, 4);
            paintArt(low);
            low.dispose();
            // darken the city a little so the cards pop
            g2.setColor(new Color(8, 10, 30, 120));
            g2.fillRect(0, 0, W, H);

            textCenter(g2, "ROAD TO LEGENDARY", 480 + 4, 66 + 4, 54, new Color(120, 30, 30));
            textCenter(g2, "ROAD TO LEGENDARY", 480, 66, 54, new Color(255, 214, 60));
            textCenter(g2, "C H O O S E   Y O U R   S A V E   S L O T", 480, 108, 18, new Color(150, 200, 255));
            for (int i = 0; i < SaveGame.SLOTS; i++) drawCard(g2, i);
        }

        /** Text without the dark shadow (looks cleaner on the white card). */
        private void plainCenter(Graphics2D g, String t, int cx, int y, int size, Color c) {
            g.setFont(font(size));
            g.setColor(c);
            g.drawString(t, cx - g.getFontMetrics().stringWidth(t) / 2, y);
        }

        private void drawCard(Graphics2D g, int i) {
            int x = cardX(i), y = CARD_Y;
            SaveGame.Info in = infos[i];
            // the card: paper-white inside a dark neon frame
            fill(g, Color.BLACK, x - 4, y - 4, CARD_W + 8, CARD_H + 8);
            fill(g, in != null ? new Color(255, 214, 60) : new Color(110, 130, 200), x - 2, y - 2, CARD_W + 4, CARD_H + 4);
            fill(g, new Color(246, 244, 250), x, y, CARD_W, CARD_H);
            // header with a little checkered flag strip
            fill(g, new Color(26, 30, 64), x, y, CARD_W, 40);
            for (int k = 0; k < CARD_W / 10; k++) fill(g, k % 2 == 0 ? Color.WHITE : Color.BLACK, x + k * 10, y + 36, 10, 4);
            textCenter(g, "SAVE SLOT " + (i + 1), x + CARD_W / 2, y + 28, 20, new Color(255, 214, 60));
            // dashed lines like a ticket
            g.setColor(new Color(220, 130, 130));
            for (int dx = 8; dx < CARD_W - 8; dx += 8) g.fillRect(x + dx, y + 126, 4, 1);

            if (in == null) {
                textCenter(g, "EMPTY SLOT", x + CARD_W / 2, y + 86, 30, new Color(90, 90, 100));
                for (int r = 0; r < 6; r++) {
                    fill(g, r % 2 == 0 ? new Color(248, 214, 220) : new Color(246, 244, 250), x + 6, y + 110 + r * 44, CARD_W - 12, 44);
                }
                g.drawImage(CarShop.createStarterCar().getLargeImage(), x + 65, y + 198, 150, 62, null);
                plainCenter(g, "Start a new career!", x + CARD_W / 2, y + 300, 14, new Color(110, 110, 130));
                return;
            }

            String nm = in.name.toUpperCase();
            if (nm.length() > 11) nm = nm.substring(0, 11);
            textCenter(g, nm, x + CARD_W / 2, y + 82, 28, new Color(80, 80, 92));
            // the big RANK badge with rays behind the number
            int cx = x + CARD_W / 2, cy = y + 216;
            for (int r = 0; r < 16; r++) {
                double a0 = r * Math.PI / 8, a1 = a0 + Math.PI / 16;
                int[] px = { cx, (int) (cx + Math.cos(a0) * 100), (int) (cx + Math.cos(a1) * 100) };
                int[] py = { cy, (int) (cy + Math.sin(a0) * 100), (int) (cy + Math.sin(a1) * 100) };
                g.setColor(new Color(248, 200, 205));
                g.fillPolygon(px, py, 3);
            }
            fill(g, new Color(122, 24, 24), cx - 64, y + 130, 128, 40);
            fill(g, new Color(214, 50, 44), cx - 60, y + 134, 120, 32);
            textCenter(g, "RANK", cx, y + 161, 30, Color.WHITE);
            String num = String.valueOf(in.rank + 1);
            for (int ox = -3; ox <= 3; ox += 3) for (int oy = -3; oy <= 3; oy += 3) textCenter(g, num, cx + ox, y + 262 + oy, 88, new Color(122, 24, 24));
            textCenter(g, num, cx, y + 262, 88, new Color(214, 50, 44));
            textCenter(g, in.rankName.toUpperCase(), cx, y + 290, 18, new Color(196, 44, 40));
            // the car this player drives (our special touch)
            g.drawImage(in.car.getLargeImage(), x + 72, y + 298, 136, 57, null);
            plainCenter(g, String.format("%,d", in.fame) + " FAME POINTS", cx, y + 376, 15, new Color(80, 80, 92));
            plainCenter(g, "WON " + in.won + "/25   " + money(in.money), cx, y + 394, 12, new Color(110, 110, 130));
        }

        /** Night-time city skyline with three cars on the road (240 x 160 grid). */
        private void paintArt(Graphics2D g) {
            for (int i = 0; i < 18; i++) fill(g, new Color(8 + i * 3, 10 + i * 3, 38 + i * 6), 0, i * 6, 240, 6);
            Random r = new Random(5);
            for (int i = 0; i < 80; i++) {
                int x = r.nextInt(240), y = r.nextInt(70);
                fill(g, new Color(220, 225, 255), x, y, 1, 1);
                if (i % 7 == 0) fill(g, new Color(180, 190, 255), x - 1, y, 3, 1);
            }
            // moon
            fill(g, new Color(250, 244, 200), 200, 14, 12, 12);
            fill(g, new Color(250, 244, 200), 198, 16, 16, 8);
            fill(g, new Color(250, 244, 200), 202, 12, 8, 16);
            fill(g, new Color(225, 218, 170), 204, 18, 4, 4);
            // skyline
            Random b = new Random(9);
            int x = 0;
            while (x < 240) {
                int w = 10 + b.nextInt(14);
                int h = 34 + b.nextInt(52);
                fill(g, new Color(22, 26, 62), x, 112 - h, w, h);
                fill(g, new Color(34, 40, 86), x, 112 - h, w, 2);
                for (int wy = 112 - h + 5; wy < 108; wy += 5) {
                    for (int wx = x + 2; wx < x + w - 2; wx += 4) {
                        if (b.nextInt(3) != 0) fill(g, new Color(250, 218, 110), wx, wy, 2, 2);
                    }
                }
                x += w + 1;
            }
            // road
            fill(g, new Color(36, 38, 52), 0, 112, 240, 48);
            fill(g, new Color(90, 92, 110), 0, 112, 240, 2);
            for (int dx = 0; dx < 240; dx += 16) fill(g, new Color(236, 220, 100), dx, 138, 8, 2);
            // cars
            ArrayList<Car> cat = CarShop.getCatalog();
            g.drawImage(cat.get(13).getLargeImage(), 18, 118, 48, 20, null);
            g.drawImage(cat.get(6).getLargeImage(), 98, 124, 48, 20, null);
            g.drawImage(cat.get(14).getLargeImage(), 176, 118, 48, 20, null);
            g.setColor(new Color(255, 240, 150, 40));
            g.fillRect(66, 124, 22, 6);
            g.fillRect(224, 124, 16, 6);
        }
    }

    // ==================================================================
    // BASE CLASS FOR BUILDING SCREENS
    // ==================================================================
    private abstract class BuildingScreen extends JPanel implements Screen {
        protected final int id;
        protected String status = "";

        BuildingScreen(int id) {
            this.id = id;
            setLayout(null);
            setPreferredSize(new Dimension(W, H));
        }

        protected PixelButton addButton(String text, Color color, int x, int y, int w, int h, final Runnable action) {
            PixelButton b = new PixelButton(text, color);
            b.setBounds(x, y, w, h);
            b.addActionListener(e -> action.run());
            add(b);
            return b;
        }

        protected PixelButton addBackButton(int x, int y, int w, int h) {
            return addButton("BACK TO CITY", new Color(190, 70, 60), x, y, w, h, () -> showCity());
        }

        public void onShow() {
            status = "";
            Sound.playMusic(Sound.MUSIC_MENU);
            refresh();
            repaint();
        }

        /** Re-reads the game data and updates the buttons. */
        protected void refresh() { }

        protected void setStatus(String s) {
            status = s;
            refresh();
            repaint();
        }

        protected Rectangle statusBounds() { return new Rectangle(130, 544, 700, 60); }

        protected void paintContent(Graphics2D g) { }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // pixel-art interior background
            Graphics2D low = (Graphics2D) g2.create();
            low.scale(4, 4);
            switch (id) {
                case CityMap.GARAGE: Garage.paintInterior(low); break;
                case CityMap.CAR_SHOP: CarShop.paintInterior(low); break;
                case CityMap.REPAIR_SHOP: RepairShop.paintInterior(low); break;
                case CityMap.PAINT_SHOP: PaintShop.paintInterior(low); break;
                case CityMap.RACE_HQ: Career.paintHeadquarters(low); break;
                default: Race.paintTrackLobby(low); break;
            }
            low.dispose();

            // sign with the building name
            Color[] signColors = { new Color(120, 60, 46), new Color(40, 70, 160), new Color(70, 74, 88),
                                   new Color(110, 62, 150), new Color(28, 38, 80), new Color(190, 50, 50) };
            box(g2, 300, 10, 360, 56, signColors[id], new Color(255, 230, 120));
            textCenter(g2, CityMap.BUILDING_NAMES[id], 480, 48, 30, new Color(255, 240, 170));

            // player + money bars
            Driver d = driver;
            box(g2, 12, 10, 270, 56, new Color(22, 26, 54), new Color(120, 160, 255));
            text(g2, d.getName().toUpperCase(), 28, 36, 20, new Color(255, 240, 170), true);
            text(g2, d.getRankName(), 28, 56, 14, Color.WHITE, false);
            box(g2, 678, 10, 270, 56, new Color(22, 26, 54), new Color(120, 160, 255));
            textRight(g2, money(d.getMoney()), 932, 48, 30, new Color(120, 240, 130));

            paintContent(g2);

            if (status != null && !status.isEmpty()) {
                Rectangle r = statusBounds();
                box(g2, r.x, r.y, r.width, r.height, new Color(22, 26, 54), new Color(255, 220, 90));
                List<String> lines = wrap(status, g2.getFontMetrics(font(16)), r.width - 36);
                int ty = r.y + 30 - (lines.size() > 1 ? 9 : 0);
                for (int i = 0; i < lines.size() && i < 2; i++) text(g2, lines.get(i), r.x + 18, ty + i * 20, 16, Color.WHITE, false);
            }
        }
    }

    // ==================================================================
    // CAR BROWSER (Garage / Car Shop / Repair Shop / Paint Shop)
    // ==================================================================
    private abstract class CarBrowser extends BuildingScreen {
        protected int idx = 0;

        CarBrowser(int id) {
            super(id);
            PixelButton prev = addButton("<", new Color(70, 90, 160), 40, 210, 76, 76, () -> step(-1));
            PixelButton next = addButton(">", new Color(70, 90, 160), 844, 210, 76, 76, () -> step(1));
            prev.setFont(font(40));
            next.setFont(font(40));
        }

        protected abstract List<Car> getCars();

        protected void onSelectionChanged() { }

        protected Car current() {
            List<Car> list = getCars();
            if (list.isEmpty()) return null;
            idx = Math.max(0, Math.min(idx, list.size() - 1));
            return list.get(idx);
        }

        protected void step(int delta) {
            int n = getCars().size();
            idx = (idx + delta + n) % n;
            status = "";
            onSelectionChanged();
            refresh();
            repaint();
        }

        @Override
        protected void paintContent(Graphics2D g) {
            Car c = current();
            if (c != null) drawCarCard(g, c, -1);
        }

        /** The big card with the car picture and its stat bars. */
        protected void drawCarCard(Graphics2D g, Car car, int previewPaint) {
            box(g, 130, 84, 700, 330, new Color(18, 22, 46, 232), new Color(120, 160, 255));
            text(g, car.getName().toUpperCase(), 150, 118, 24, new Color(255, 240, 170), true);
            textRight(g, (idx + 1) + " / " + getCars().size(), 810, 116, 16, new Color(170, 185, 225));

            // picture on a little platform
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(156, 268, 276, 10);
            BufferedImage img = previewPaint >= 0 ? car.getLargeImage(previewPaint) : car.getLargeImage();
            g.drawImage(img, 150, 140, 48 * 6, 20 * 6, null);
            String paintName = previewPaint >= 0 ? Car.PAINT_NAMES[previewPaint] : car.getPaintName();
            text(g, "Paint: " + paintName, 150, 304, 16, Color.WHITE, false);

            // stat bars
            for (int i = 0; i < 3; i++) {
                int ly = 140 + i * 48;
                int base = car.getBaseStat(i);
                int bonus = car.getStat(i) - base;
                text(g, Car.STAT_NAMES[i].toUpperCase(), 470, ly, 14, new Color(190, 200, 230), false);
                textRight(g, car.getStat(i) + (bonus > 0 ? " (+" + bonus + ")" : ""), 810, ly, 14, Color.WHITE);
                g.setColor(Color.BLACK);
                g.fillRect(470, ly + 6, 340, 20);
                g.setColor(new Color(50, 56, 90));
                g.fillRect(473, ly + 9, 334, 14);
                int bw = (int) (334 * Math.min(1.0, base / 125.0));
                int uw = (int) (334 * Math.min(1.0, car.getStat(i) / 125.0)) - bw;
                g.setColor(new Color(70, 190, 240));
                g.fillRect(473, ly + 9, bw, 14);
                g.setColor(new Color(255, 200, 50));
                g.fillRect(473 + bw, ly + 9, Math.max(0, uw), 14);
                g.setColor(new Color(255, 255, 255, 60));
                g.fillRect(473, ly + 9, bw + Math.max(0, uw), 3);
            }
            // condition bar
            int cond = car.getCondition();
            text(g, "CONDITION", 470, 300, 14, new Color(190, 200, 230), false);
            textRight(g, cond + "%", 810, 300, 14, Color.WHITE);
            g.setColor(Color.BLACK);
            g.fillRect(470, 306, 340, 20);
            g.setColor(new Color(50, 56, 90));
            g.fillRect(473, 309, 334, 14);
            g.setColor(cond > 60 ? new Color(90, 210, 110) : cond > 30 ? new Color(240, 190, 50) : new Color(230, 70, 60));
            g.fillRect(473, 309, 334 * cond / 100, 14);
            text(g, "RATING " + car.getOverallRating(), 470, 356, 18, new Color(255, 240, 170), true);
            if (car.isRetro()) text(g, "RETRO CLASSIC", 640, 356, 18, new Color(255, 160, 90), true);
        }
    }

    // ==================================================================
    // GARAGE
    // ==================================================================
    private class GarageScreen extends CarBrowser {
        private final PixelButton[] upgradeButtons = new PixelButton[3];
        private final PixelButton selectButton;
        private final PixelButton sellButton;
        private final String[] shortNames = { "SPEED", "ACCEL", "HANDLING" };

        GarageScreen() {
            super(CityMap.GARAGE);
            for (int i = 0; i < 3; i++) {
                final int cat = i;
                upgradeButtons[i] = addButton("", new Color(210, 150, 30), 130 + i * 238, 424, 224, 48, () -> upgrade(cat));
            }
            selectButton = addButton("SELECT CAR", new Color(60, 160, 80), 130, 484, 224, 48, () -> selectCar());
            sellButton = addButton("SELL CAR", new Color(160, 80, 160), 368, 484, 224, 48, () -> sellCar());
            addBackButton(606, 484, 224, 48);
        }

        protected List<Car> getCars() { return driver.getGarage().getCars(); }

        @Override
        protected void refresh() {
            Car c = current();
            if (c == null) return;
            for (int i = 0; i < 3; i++) {
                if (c.isMaxed(i)) {
                    upgradeButtons[i].setText(shortNames[i] + " MAXED");
                    upgradeButtons[i].setEnabled(false);
                } else {
                    upgradeButtons[i].setText(shortNames[i] + " UP  " + money(c.getUpgradeCost(i)));
                    upgradeButtons[i].setEnabled(true);
                }
            }
            selectButton.setEnabled(c != driver.getGarage().getCurrentCar());
            selectButton.setText(c == driver.getGarage().getCurrentCar() ? "IN USE" : "SELECT CAR");
            sellButton.setText("SELL  " + money(c.getSellValue()));
            sellButton.setEnabled(driver.getGarage().canSell(c));
        }

        private void upgrade(int cat) {
            Car c = current();
            int before = driver.getMoney();
            String result = driver.getGarage().upgradeCar(driver, c, cat);
            Sound.play(driver.getMoney() < before ? Sound.BUY : Sound.ERROR);
            setStatus(result);
        }

        private void selectCar() {
            Car c = current();
            driver.getGarage().setCurrentCar(c);
            setStatus("The " + c.getName() + " is now your racing car!");
        }

        private void sellCar() {
            Car c = current();
            Garage garage = driver.getGarage();
            String warn = (c == garage.getCurrentCar())
                    ? "\n\nThis is your current racing car - your next car in the garage will be used instead." : "";
            if (!confirm("SELL CAR?", "Sell the " + c.getName() + " for " + money(c.getSellValue()) + "?" + warn, "SELL IT", "KEEP IT")) return;
            int got = garage.sellCar(driver, c);
            if (got < 0) {
                setStatus("You can't sell your last car!");
            } else {
                idx = Math.max(0, idx - 1);
                Sound.play(Sound.CASH);
                setStatus("Sold for " + money(got) + ".");
            }
        }

        @Override
        protected void paintContent(Graphics2D g) {
            super.paintContent(g);
            Car c = current();
            if (c == null) return;
            if (c == driver.getGarage().getCurrentCar()) {
                text(g, "* CURRENT RACING CAR *", 150, 338, 16, new Color(120, 240, 130), true);
            }
            text(g, "Upgrades  SPD " + c.getUpgradeLevel(0) + "/5   ACC " + c.getUpgradeLevel(1) + "/5   HDL " + c.getUpgradeLevel(2) + "/5",
                    150, 362, 14, Color.WHITE, false);
            text(g, "Sell value: " + money(c.getSellValue()), 150, 386, 14, Color.WHITE, false);
        }
    }

    // ==================================================================
    // CAR SHOP
    // ==================================================================
    private class ShopScreen extends CarBrowser {
        private final PixelButton buyButton;

        ShopScreen() {
            super(CityMap.CAR_SHOP);
            buyButton = addButton("BUY CAR", new Color(60, 160, 80), 130, 484, 224, 48, () -> buy());
            addButton("NEXT RANK >>", new Color(70, 110, 190), 368, 484, 224, 48, () -> nextRank());
            addBackButton(606, 484, 224, 48);
        }

        protected List<Car> getCars() { return CarShop.getCatalog(); }

        @Override
        protected void refresh() {
            Car c = current();
            if (c == null) return;
            buyButton.setText("BUY CAR  " + money(c.getPrice()));
            buyButton.setBase(CarShop.isRankUnlocked(driver, c) ? new Color(60, 160, 80) : new Color(120, 120, 130));
        }

        private void buy() {
            int before = driver.getMoney();
            String result = CarShop.buyCar(driver, current());
            Sound.play(driver.getMoney() < before ? Sound.BUY : Sound.ERROR);
            setStatus(result);
        }

        private void nextRank() {
            List<Car> list = getCars();
            int rank = list.get(idx).getRankRequired();
            for (int i = 1; i <= list.size(); i++) {
                int j = (idx + i) % list.size();
                if (list.get(j).getRankRequired() != rank) {
                    idx = j;
                    break;
                }
            }
            status = "";
            refresh();
            repaint();
        }

        @Override
        protected void paintContent(Graphics2D g) {
            super.paintContent(g);
            Car c = current();
            if (c == null) return;
            text(g, "PRICE: " + money(c.getPrice()), 150, 338, 20, new Color(120, 240, 130), true);
            boolean ok = CarShop.isRankUnlocked(driver, c);
            text(g, "Rank needed: " + Career.RANK_NAMES[c.getRankRequired()] + (ok ? "" : "  (LOCKED)"),
                    150, 362, 14, ok ? Color.WHITE : new Color(255, 120, 110), false);
            int owned = CarShop.countOwned(driver, c);
            text(g, owned > 0 ? "You own " + owned + " of these" : "Not in your garage yet", 150, 386, 14, new Color(190, 200, 230), false);
        }
    }

    // ==================================================================
    // REPAIR SHOP
    // ==================================================================
    private class RepairScreen extends CarBrowser {
        private final PixelButton repairButton;

        RepairScreen() {
            super(CityMap.REPAIR_SHOP);
            repairButton = addButton("REPAIR", new Color(60, 160, 80), 130, 484, 224, 48, () -> repair());
            addBackButton(606, 484, 224, 48);
        }

        protected List<Car> getCars() { return driver.getGarage().getCars(); }

        @Override
        protected void refresh() {
            Car c = current();
            if (c == null) return;
            if (RepairShop.needsRepair(c)) {
                repairButton.setText("REPAIR  " + money(RepairShop.getRepairCost(c)));
                repairButton.setEnabled(true);
            } else {
                repairButton.setText("NO REPAIR NEEDED");
                repairButton.setEnabled(false);
            }
        }

        private void repair() {
            int before = driver.getMoney();
            String result = RepairShop.repairCar(driver, current());
            Sound.play(driver.getMoney() < before ? Sound.BUY : Sound.ERROR);
            setStatus(result);
        }

        @Override
        protected void paintContent(Graphics2D g) {
            super.paintContent(g);
            Car c = current();
            if (c == null) return;
            text(g, "Condition: " + c.getCondition() + "%", 150, 338, 18, Color.WHITE, true);
            if (RepairShop.needsRepair(c)) {
                text(g, "Repair cost: " + money(RepairShop.getRepairCost(c)), 150, 362, 16, new Color(255, 200, 80), true);
                text(g, "Repairing restores 100%", 150, 386, 14, new Color(190, 200, 230), false);
            } else {
                text(g, "Perfect condition!", 150, 362, 16, new Color(120, 240, 130), true);
            }
        }
    }

    // ==================================================================
    // PAINT SHOP
    // ==================================================================
    private class PaintScreen extends CarBrowser {
        private int selectedPaint = 0;
        private final PixelButton[] swatches = new PixelButton[7];
        private final PixelButton paintButton;

        PaintScreen() {
            super(CityMap.PAINT_SHOP);
            for (int i = 0; i < 7; i++) {
                final int p = i;
                swatches[i] = addButton("", Color.GRAY, 130 + i * 101, 424, 94, 48, () -> {
                    selectedPaint = p;
                    refresh();
                    repaint();
                });
                swatches[i].setFont(font(13));
            }
            paintButton = addButton("PAINT CAR", new Color(60, 160, 80), 130, 484, 224, 48, () -> paint());
            addBackButton(606, 484, 224, 48);
            onSelectionChanged();
        }

        protected List<Car> getCars() { return driver.getGarage().getCars(); }

        @Override
        protected void onSelectionChanged() {
            Car c = current();
            if (c != null) selectedPaint = c.getPaintIndex();
        }

        @Override
        public void onShow() {
            onSelectionChanged();
            super.onShow();
        }

        @Override
        protected void refresh() {
            Car c = current();
            if (c == null) return;
            for (int i = 0; i < 7; i++) {
                swatches[i].setBase(c.getBodyColor(i));
                swatches[i].setText(Car.PAINT_NAMES[i].toUpperCase() + " " + (Car.PAINT_PRICES[i] == 0 ? "FREE" : "$" + Car.PAINT_PRICES[i]));
            }
            int cost = Car.PAINT_PRICES[selectedPaint];
            paintButton.setText("PAINT  " + (cost == 0 ? "FREE" : money(cost)));
            paintButton.setEnabled(selectedPaint != c.getPaintIndex());
        }

        private void paint() {
            int before = driver.getMoney();
            String result = PaintShop.paintCar(driver, current(), selectedPaint);
            Sound.play(driver.getMoney() < before ? Sound.BUY : Sound.ERROR);
            setStatus(result);
            onSelectionChanged();
            refresh();
        }

        @Override
        protected void paintContent(Graphics2D g) {
            Car c = current();
            if (c == null) return;
            drawCarCard(g, c, selectedPaint);
            text(g, "Paint is only cosmetic - it never changes", 150, 350, 14, new Color(190, 200, 230), false);
            text(g, "speed, acceleration or handling.", 150, 370, 14, new Color(190, 200, 230), false);
            text(g, "Pick a colour below, then press PAINT CAR.", 150, 394, 14, Color.WHITE, false);
            Rectangle r = swatches[selectedPaint].getBounds();
            g.setColor(new Color(255, 240, 120));
            for (int t = 0; t < 4; t++) g.drawRect(r.x - 4 + t, r.y - 4 + t, r.width + 8 - 2 * t - 1, r.height + 8 - 2 * t - 1);
        }
    }

    // ==================================================================
    // RACE HQ and RACE TRACK (race selection)
    // ==================================================================
    private class RaceSelectScreen extends BuildingScreen {
        private final boolean hq;
        private int viewRank;
        private Race selected;
        private final PixelButton[] rows = new PixelButton[5];
        private final PixelButton startButton;
        private final PixelButton prevRank;
        private final PixelButton nextRank;

        RaceSelectScreen(boolean hq) {
            super(hq ? CityMap.RACE_HQ : CityMap.RACE_TRACK);
            this.hq = hq;
            prevRank = addButton("<", new Color(70, 90, 160), 416, 92, 44, 34, () -> changeRank(-1));
            nextRank = addButton(">", new Color(70, 90, 160), 884, 92, 44, 34, () -> changeRank(1));
            for (int i = 0; i < 5; i++) {
                final int n = i;
                rows[i] = addButton("", new Color(60, 80, 140), 416, 134 + i * 50, 504, 44, () -> {
                    selected = career.getRaceInRank(viewRank, n);
                    status = "";
                    refresh();
                    repaint();
                });
                rows[i].setFont(font(15));
            }
            startButton = addButton("START RACE", new Color(60, 170, 80), 400, 548, 260, 50, () -> start());
            addButton("BACK TO CITY", new Color(190, 70, 60), 676, 548, 260, 50, () -> showCity());
            if (hq) addButton("SPONSOR CHALLENGES", new Color(200, 130, 40), 44, 452, 324, 36, () -> showScreen("sponsors"));
        }

        @Override
        protected Rectangle statusBounds() { return new Rectangle(24, 544, 364, 60); }

        @Override
        public void onShow() {
            viewRank = driver.getRank();
            selected = null;
            for (int i = 0; i < 5 && selected == null; i++) {
                Race r = career.getRaceInRank(viewRank, i);
                if (career.isRaceUnlocked(driver, r) && !r.isWon()) selected = r;
            }
            if (selected == null) selected = career.getRaceInRank(viewRank, 0);
            super.onShow();
        }

        private void changeRank(int d) {
            viewRank = Math.max(0, Math.min(4, viewRank + d));
            selected = career.getRaceInRank(viewRank, 0);
            for (int i = 0; i < 5; i++) {
                Race r = career.getRaceInRank(viewRank, i);
                if (career.isRaceUnlocked(driver, r) && !r.isWon()) { selected = r; break; }
            }
            status = "";
            refresh();
            repaint();
        }

        @Override
        protected void refresh() {
            prevRank.setEnabled(viewRank > 0);
            nextRank.setEnabled(viewRank < 4);
            for (int i = 0; i < 5; i++) {
                Race r = career.getRaceInRank(viewRank, i);
                boolean open = career.isRaceUnlocked(driver, r);
                String result = !open ? "LOCKED" : r.isCompleted() ? "BEST " + ordinal(r.getBestPlace()) : "NEW";
                String name = r.getName();
                if (name.length() > 24) name = name.substring(0, 24);
                rows[i].setText(String.format("%02d %-24s %s", r.getNumber(), name, result));
                rows[i].setEnabled(open);
                Color c = r.isWon() ? new Color(60, 140, 90) : r.isCompleted() ? new Color(150, 120, 50) : new Color(60, 80, 140);
                rows[i].setBase(c);
            }
            startButton.setEnabled(selected != null && career.isRaceUnlocked(driver, selected));
        }

        private void start() {
            if (selected == null || !career.isRaceUnlocked(driver, selected)) return;
            Car c = driver.getCurrentCar();
            if (c.getCondition() < 40) {
                if (!confirm("DAMAGED CAR", "Your " + c.getName() + " is only at " + c.getCondition()
                        + "% condition and will perform worse. Race anyway?", "RACE", "WAIT")) return;
            }
            startRace(selected, 0, "b" + id);
        }

        @Override
        protected void paintContent(Graphics2D g) {
            // ---- left panel ----
            box(g, 24, 84, 364, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
            if (hq) paintCareerStats(g); else paintGarageCar(g);

            // ---- right panel ----
            box(g, 400, 84, 536, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
            textCenter(g, Career.RANK_NAMES[viewRank].toUpperCase() + "  " + career.getCompletedInRank(viewRank) + "/5 done  "
                    + career.getWinsInRank(viewRank) + "/5 won", 672, 116, 16, new Color(255, 240, 170));
            if (selected != null) {
                Rectangle r = rows[selected.getIndexInRank()].getBounds();
                g.setColor(new Color(255, 240, 120));
                for (int t = 0; t < 3; t++) g.drawRect(r.x - 3 + t, r.y - 3 + t, r.width + 6 - 2 * t - 1, r.height + 6 - 2 * t - 1);
                paintRaceInfo(g, selected);
                if (!career.isRaceUnlocked(driver, selected)) {
                    text(g, "Win the race before it to unlock this one.", 428, 514, 13, new Color(255, 190, 120), true);
                }
            }
        }

        private void paintCareerStats(Graphics2D g) {
            text(g, "CAREER STATS", 44, 120, 22, new Color(255, 240, 170), true);
            int rank = driver.getRank();
            String[][] lines = {
                { "Current rank", driver.getRankName() },
                { "Rank progress", career.getCompletedInRank(rank) + "/5 races" },
                { "Wins this rank", career.getWinsInRank(rank) + "/" + Career.WINS_NEEDED + " (need all)" },
                { "Races completed", String.valueOf(driver.getRacesCompleted()) },
                { "Wins / Losses", driver.getWins() + " / " + driver.getLosses() },
                { "Money earned", money(driver.getMoneyEarned()) },
                { "Cars owned", String.valueOf(driver.getCarsOwned()) },
                { "Fame", "Lv" + driver.getRepLevel() + " " + driver.getRepName() },
                { "Sponsor deals", driver.getChallengesDone() + "/" + Career.CHALLENGES.length },
                { "Racing car", driver.getCurrentCar().getName() }
            };
            for (int i = 0; i < lines.length; i++) {
                int y = 160 + i * 31;
                text(g, lines[i][0], 44, y, 15, new Color(190, 200, 230), false);
                textRight(g, lines[i][1], 366, y, 15, Color.WHITE);
                g.setColor(new Color(255, 255, 255, 30));
                g.fillRect(44, y + 8, 322, 1);
            }
            text(g, "Career: won " + career.getTotalWon() + "/25 races", 44, 512, 16, new Color(120, 240, 130), true);
            text(g, "Win a race to unlock the next one!", 44, 528, 11, new Color(190, 200, 230), false);
        }

        private void paintGarageCar(Graphics2D g) {
            Car c = driver.getCurrentCar();
            text(g, "YOUR RACING CAR", 44, 120, 22, new Color(255, 240, 170), true);
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(70, 252, 252, 8);
            g.drawImage(c.getLargeImage(), 62, 150, 240, 100, null);
            text(g, c.getName(), 44, 296, 18, Color.WHITE, true);
            String[] labels = { "SPEED", "ACCEL", "HANDLING" };
            for (int i = 0; i < 3; i++) {
                int y = 330 + i * 28;
                text(g, labels[i], 44, y, 14, new Color(190, 200, 230), false);
                g.setColor(Color.BLACK);
                g.fillRect(150, y - 13, 200, 16);
                g.setColor(new Color(70, 190, 240));
                g.fillRect(152, y - 11, (int) (196 * Math.min(1.0, c.getStat(i) / 125.0)), 12);
            }
            text(g, "Condition: " + c.getCondition() + "%", 44, 440, 16, c.getCondition() > 60 ? new Color(120, 240, 130) : new Color(255, 150, 90), true);
            if (c.getCondition() < 60) text(g, "Visit the Repair Shop!", 44, 466, 14, new Color(255, 200, 80), false);
            text(g, "Pick a race on the right,", 44, 500, 13, new Color(190, 200, 230), false);
            text(g, "then press START RACE.", 44, 518, 13, new Color(190, 200, 230), false);
        }

        private void paintRaceInfo(Graphics2D g, Race r) {
            box(g, 412, 388, 512, 138, new Color(12, 16, 36), new Color(90, 120, 200));
            text(g, "#" + r.getNumber() + "  " + r.getName(), 428, 414, 16, new Color(255, 240, 170), true);
            text(g, "Track: " + r.getThemeName() + (r.isNight() ? " (night)" : ""), 428, 438, 13, Color.WHITE, false);
            text(g, "Length: " + String.format("%,d", (int) r.getLength()) + " m   Difficulty: " + r.getDifficulty() + "/25", 428, 458, 13, Color.WHITE, false);
            text(g, "1st " + money(r.getPrize(1)) + "  2nd " + money(r.getPrize(2)) + "  3rd " + money(r.getPrize(3)), 428, 478, 13, new Color(120, 240, 130), false);
            if (r.hasRetroReward() && !r.isRewardClaimed()) {
                text(g, "SPECIAL REWARD: a retro car!", 428, 500, 13, new Color(255, 170, 90), true);
            }
            text(g, "Opponents", 700, 414, 13, new Color(190, 200, 230), false);
            for (int i = 0; i < r.getOpponents().size(); i++) {
                Opponent o = r.getOpponents().get(i);
                String line = o.getName() + " (" + o.getCar().getName() + ")";
                if (line.length() > 28) line = line.substring(0, 28);
                text(g, line, 700, 434 + i * 17, 11, Color.WHITE, false);
            }
        }
    }

}
