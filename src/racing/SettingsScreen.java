package racing;

import static racing.GameWindow.box;
import static racing.GameWindow.fill;
import static racing.GameWindow.font;
import static racing.GameWindow.text;
import static racing.GameWindow.textCenter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The SETTINGS screen: sound, music, HUD, controls, difficulty and more.
 * Every change is applied straight away and saved to a small file.
 */
class SettingsScreen extends JPanel implements GameWindow.Screen {

    /** A pixel-style volume slider (click or drag). */
    private static class PixelSlider extends JComponent {
        private int value;
        private final java.util.function.IntConsumer onChange;
        private final Runnable onRelease;

        PixelSlider(int value, java.util.function.IntConsumer onChange, Runnable onRelease) {
            this.value = value;
            this.onChange = onChange;
            this.onRelease = onRelease;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                public void mousePressed(MouseEvent e) { set(e.getX()); }
                public void mouseDragged(MouseEvent e) { set(e.getX()); }
                public void mouseReleased(MouseEvent e) { PixelSlider.this.onRelease.run(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void setValue(int v) { value = v; repaint(); }

        private void set(int x) {
            int v = Math.round((x - 10) * 20f / Math.max(1, getWidth() - 20)) * 5;
            value = Math.max(0, Math.min(100, v));
            onChange.accept(value);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();
            fill(g2, Color.BLACK, 0, h / 2 - 8, w, 16);
            fill(g2, new Color(40, 44, 70), 3, h / 2 - 5, w - 6, 10);
            int px = 10 + (int) ((w - 20) * value / 100.0);
            fill(g2, new Color(90, 200, 255), 3, h / 2 - 5, Math.max(0, px - 3), 10);
            fill(g2, Color.BLACK, px - 8, h / 2 - 14, 16, 28);
            fill(g2, new Color(255, 240, 170), px - 6, h / 2 - 12, 12, 24);
            text(g2, value + "%", w - 46, h / 2 - 12, 11, Color.WHITE, false);
        }
    }

    private final GameWindow window;
    private final GameWindow.PixelButton sfxToggle, musicToggle, minimapToggle, speedoToggle, unitButton,
            controlsButton, difficultyButton, hintsToggle, effectsToggle, resetButton, saveButton, logoutButton;
    private final PixelSlider sfxSlider, musicSlider;

    SettingsScreen(GameWindow window) {
        this.window = window;
        setLayout(null);
        setPreferredSize(new Dimension(GameWindow.W, GameWindow.H));
        int tx = 520, y = 124;

        sfxToggle = toggle(tx, y, () -> { Settings.sfxOn = !Settings.sfxOn; Sound.applySfxSettings(); changed(); Sound.play(Sound.CLICK); });
        sfxSlider = new PixelSlider(Settings.sfxVolume, v -> Settings.sfxVolume = v, () -> { Sound.play(Sound.BUY); changed(); });
        sfxSlider.setBounds(tx + 100, y - 4, 190, 40);
        add(sfxSlider);
        y += 42;
        musicToggle = toggle(tx, y, () -> { Settings.musicOn = !Settings.musicOn; Sound.applyMusicSettings(); changed(); });
        musicSlider = new PixelSlider(Settings.musicVolume, v -> { Settings.musicVolume = v; Sound.applyMusicSettings(); }, this::changed);
        musicSlider.setBounds(tx + 100, y - 4, 190, 40);
        add(musicSlider);
        y += 42;
        minimapToggle = toggle(tx, y, () -> { Settings.showMinimap = !Settings.showMinimap; changed(); });
        y += 42;
        speedoToggle = toggle(tx, y, () -> { Settings.showSpeedo = !Settings.showSpeedo; changed(); });
        y += 42;
        unitButton = wide(tx, y, () -> { Settings.kph = !Settings.kph; changed(); });
        y += 42;
        controlsButton = wide(tx, y, () -> { Settings.controls = (Settings.controls + 1) % 3; changed(); });
        y += 42;
        difficultyButton = wide(tx, y, () -> { Settings.difficulty = (Settings.difficulty + 1) % 3; changed(); });
        y += 42;
        hintsToggle = toggle(tx, y, () -> { Settings.hints = !Settings.hints; changed(); });
        y += 42;
        effectsToggle = toggle(tx, y, () -> { Settings.effects = !Settings.effects; changed(); });

        GameWindow.PixelButton help = new GameWindow.PixelButton("HOW TO PLAY", new Color(60, 140, 100));
        help.setBounds(156, 526, 140, 44);
        help.addActionListener(e -> window.showTutorial(false));
        add(help);
        resetButton = new GameWindow.PixelButton("RESET CAREER", new Color(200, 110, 40));
        saveButton = new GameWindow.PixelButton("SAVE GAME", new Color(70, 110, 200));
        saveButton.setBounds(304, 526, 120, 44);
        saveButton.addActionListener(e -> window.message(window.saveNow() ? "GAME SAVED" : "SAVE FAILED",
                window.saveNow() ? "Your game is saved. It also saves by itself when you move around the city." : "The game could not be saved."));
        add(saveButton);
        logoutButton = new GameWindow.PixelButton("SAVE SLOTS", new Color(120, 90, 170));
        logoutButton.setBounds(428, 526, 118, 44);
        logoutButton.addActionListener(e -> {
            if (window.confirm("SAVE SLOTS", "Your game is saved. Go back to the 3 save slots?", "GO", "STAY")) window.backToSlots();
        });
        add(logoutButton);
        resetButton.setBounds(552, 526, 140, 44);
        resetButton.addActionListener(e -> {
            if (window.confirm("RESET CAREER?", "This erases ALL progress in this save slot (money, cars, ranks, medals) and starts a brand-new career in it.",
                    "RESET", "CANCEL")) window.resetCareer();
        });
        add(resetButton);
        GameWindow.PixelButton back = new GameWindow.PixelButton("BACK", new Color(190, 70, 60));
        back.setBounds(700, 526, 100, 44);
        back.addActionListener(e -> window.closeSettings());
        add(back);
        refresh();
    }

    private GameWindow.PixelButton toggle(int x, int y, final Runnable action) {
        GameWindow.PixelButton b = new GameWindow.PixelButton("", new Color(60, 170, 80));
        b.setBounds(x, y, 92, 34);
        b.addActionListener(e -> { action.run(); refresh(); });
        add(b);
        return b;
    }

    private GameWindow.PixelButton wide(int x, int y, final Runnable action) {
        GameWindow.PixelButton b = new GameWindow.PixelButton("", new Color(70, 90, 160));
        b.setBounds(x, y, 290, 34);
        b.addActionListener(e -> { action.run(); refresh(); });
        add(b);
        return b;
    }

    private void changed() {
        Settings.save();
        repaint();
    }

    private void setToggle(GameWindow.PixelButton b, boolean on) {
        b.setText(on ? "ON" : "OFF");
        b.setBase(on ? new Color(60, 170, 80) : new Color(170, 70, 70));
    }

    private void refresh() {
        setToggle(sfxToggle, Settings.sfxOn);
        setToggle(musicToggle, Settings.musicOn);
        setToggle(minimapToggle, Settings.showMinimap);
        setToggle(speedoToggle, Settings.showSpeedo);
        setToggle(hintsToggle, Settings.hints);
        setToggle(effectsToggle, Settings.effects);
        effectsToggle.setText(Settings.effects ? "FULL" : "LOW");
        unitButton.setText("SPEED UNITS:  " + Settings.speedUnit());
        controlsButton.setText("CONTROLS:  " + Settings.CONTROL_NAMES[Settings.controls]);
        difficultyButton.setText("DIFFICULTY:  " + Settings.DIFFICULTY_NAMES[Settings.difficulty]);
        sfxSlider.setValue(Settings.sfxVolume);
        musicSlider.setValue(Settings.musicVolume);
        sfxSlider.setEnabled(Settings.sfxOn);
        musicSlider.setEnabled(Settings.musicOn);
        resetButton.setEnabled(window.getDriver() != null);
        saveButton.setEnabled(window.getDriver() != null);
        logoutButton.setEnabled(window.getDriver() != null);
        repaint();
    }

    public void onShow() {
        refresh();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < 20; i++) fill(g2, new Color(16 + i * 2, 20 + i * 2, 50 + i * 4), 0, i * 32, GameWindow.W, 32);
        Random r = new Random(11);
        for (int i = 0; i < 70; i++) fill(g2, new Color(220, 225, 255, 160), r.nextInt(GameWindow.W), r.nextInt(GameWindow.H), 3, 3);

        box(g2, 140, 80, 680, 500, new Color(18, 22, 46, 242), new Color(255, 230, 120));
        textCenter(g2, "SETTINGS", 480, 114, 30, new Color(255, 240, 170));
        String[] labels = { "SOUND EFFECTS", "MUSIC", "MINIMAP", "SPEEDOMETER", "UNITS", "CONTROLS", "DIFFICULTY", "TUTORIAL HINTS", "VISUAL EFFECTS" };
        String[] notes = { "", "", "", "", "", "", "How tight the opponents race", "Tips in the city", "Skid marks, smoke, rain" };
        for (int i = 0; i < labels.length; i++) {
            int y = 146 + i * 42;
            text(g2, labels[i], 170, y, 17, Color.WHITE, true);
            if (!notes[i].isEmpty()) text(g2, notes[i], 170, y + 14, 10, new Color(150, 165, 200), false);
        }
        if (window.getDriver() == null) text(g2, "(Pick a save slot to use SAVE GAME and RESET CAREER)", 300, 590, 11, new Color(150, 165, 200), false);
    }
}
