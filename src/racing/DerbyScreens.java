package racing;

import static racing.GameWindow.box;
import static racing.GameWindow.fill;
import static racing.GameWindow.font;
import static racing.GameWindow.money;
import static racing.GameWindow.ordinal;
import static racing.GameWindow.text;
import static racing.GameWindow.textCenter;
import static racing.GameWindow.textRight;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

// ======================================================================
// DEMOLITION DERBY - the shop where you pick your car for ONE derby
// ======================================================================
class DerbyShopScreen extends ScreenBase {

    private int selected = 0;
    private final GameWindow.PixelButton enter;
    private final Rectangle[] cards = new Rectangle[8];

    DerbyShopScreen(GameWindow window) {
        super(window, CityMap.DERBY);
        for (int i = 0; i < 8; i++) {
            cards[i] = new Rectangle(412 + (i % 2) * 260, 96 + (i / 2) * 106, 254, 98);
        }
        enter = addButton("BUY & ENTER", new Color(60, 170, 80), 400, 548, 300, 50, this::enterDerby);
        addBackButton(716, 548, 220, 50);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < cards.length; i++) {
                    if (cards[i].contains(e.getPoint())) {
                        selected = i;
                        Sound.play(Sound.CLICK);
                        setStatus("");
                        return;
                    }
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean over = false;
                for (Rectangle r : cards) if (r.contains(e.getPoint())) over = true;
                setCursor(over ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    @Override
    protected void refresh() {
        DerbyCar c = DerbyCar.catalog().get(selected);
        boolean afford = driver().canAfford(c.price);
        enter.setText(afford ? "BUY & ENTER  -  " + money(c.price) : "NEED " + money(c.price));
        enter.setEnabled(afford);
    }

    private void enterDerby() {
        DerbyCar c = DerbyCar.catalog().get(selected);
        if (!driver().canAfford(c.price)) { setStatus("You can't afford the " + c.name + " yet."); return; }
        boolean yes = window.confirm("ENTER THE DERBY?", "Pay " + money(c.price) + " for the " + c.name + "?\n"
                + "You only use this car for ONE derby.\nLast car standing wins " + money(c.prize(1)) + "!", "LET'S SMASH", "NOT YET");
        if (!yes) return;
        driver().spend(c.price);
        window.startDerby(c, "b" + id);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        DerbyCar sel = DerbyCar.catalog().get(selected);
        box(g, 24, 84, 364, 450, new Color(18, 22, 46, 236), new Color(120, 160, 255));
        text(g, sel.name.toUpperCase(), 44, 118, 20, new Color(255, 240, 170), true);
        g.drawImage(sel.sidePicture(), 44, 130, 324, 135, null);
        ScreenUtil.drawWrapped(g, sel.blurb, 44, 288, 330, 13, new Color(190, 200, 230), 18);
        String[] labels = { "ARMOR", "SPEED", "POWER", "WEIGHT", "HANDLING" };
        double[] bars = { sel.armorBar(), sel.speedBar(), sel.powerBar(), sel.weightBar(), sel.handlingBar() };
        for (int i = 0; i < 5; i++) {
            int y = 328 + i * 24;
            text(g, labels[i], 44, y + 12, 13, Color.WHITE, false);
            fill(g, Color.BLACK, 148, y, 220, 16);
            fill(g, new Color(46, 50, 76), 150, y + 2, 216, 12);
            fill(g, i == 0 ? new Color(90, 220, 120) : i == 3 ? new Color(250, 190, 70) : new Color(90, 190, 255), 150, y + 2, (int) (216 * Math.max(0.05, bars[i])), 12);
        }
        text(g, "1st " + money(sel.prize(1)) + "   2nd " + money(sel.prize(2)) + "   3rd " + money(sel.prize(3)), 44, 470, 13, new Color(120, 240, 130), false);
        text(g, "+" + money(75) + " for every car you wreck", 44, 490, 13, new Color(255, 200, 100), false);
        text(g, "Derby wins: " + driver().getDerbyWins(), 44, 516, 14, new Color(190, 170, 255), true);

        box(g, 400, 84, 536, 450, new Color(18, 22, 46, 236), new Color(120, 160, 255));
        ArrayList<DerbyCar> all = DerbyCar.catalog();
        for (int i = 0; i < all.size(); i++) {
            DerbyCar c = all.get(i);
            Rectangle r = cards[i];
            boolean afford = driver().canAfford(c.price);
            g.setColor(i == selected ? new Color(70, 80, 140) : new Color(30, 34, 66));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(i == selected ? new Color(255, 240, 120) : new Color(80, 100, 170));
            for (int t = 0; t < 3; t++) g.drawRect(r.x + t, r.y + t, r.width - 2 * t - 1, r.height - 2 * t - 1);
            g.drawImage(c.sidePicture(), r.x + 8, r.y + 8, 150, 62, null);
            text(g, c.name, r.x + 8, r.y + 86, 12, Color.WHITE, true);
            textRight(g, money(c.price), r.x + r.width - 10, r.y + 24, 17, afford ? new Color(120, 240, 130) : new Color(255, 130, 110));
            text(g, "ARMOR", r.x + 166, r.y + 44, 10, new Color(150, 165, 200), false);
            fill(g, Color.BLACK, r.x + 166, r.y + 48, 78, 9);
            fill(g, new Color(90, 220, 120), r.x + 167, r.y + 49, (int) (76 * c.armorBar()), 7);
            text(g, "SPEED", r.x + 166, r.y + 70, 10, new Color(150, 165, 200), false);
            fill(g, Color.BLACK, r.x + 166, r.y + 74, 78, 9);
            fill(g, new Color(90, 190, 255), r.x + 167, r.y + 75, (int) (76 * c.speedBar()), 7);
        }
    }
}

// ======================================================================
// DEMOLITION DERBY - the arena
// ======================================================================
class DerbyPlayScreen extends JPanel implements GameWindow.Screen, KeyListener {

    private static final int W = GameWindow.W, H = GameWindow.H;
    private static final int CX = 480, CY = 322;

    private static class Particle {
        double x, y, vx, vy, life, maxLife, size;
        int kind;            // 0 smoke, 1 spark, 2 fire, 3 debris
        Color color;
    }

    private final GameWindow window;
    private DerbyArena arena;
    private DerbyCar myCar;
    private String origin = "city";
    private final Timer timer;
    private long last;
    private boolean up, down, left, right, boost, brake, paused;
    private BufferedImage floor, marks;
    private final Map<DerbyArena.DCar, BufferedImage> images = new HashMap<DerbyArena.DCar, BufferedImage>();
    private final Map<DerbyArena.DCar, BufferedImage> wreckImages = new HashMap<DerbyArena.DCar, BufferedImage>();
    private final ArrayList<Particle> particles = new ArrayList<Particle>();
    private final Random fx = new Random();
    private Career.DerbyOutcome outcome;
    private final GameWindow.PixelButton continueButton;
    private double shake, clock, bannerTime;
    private String banner = "";
    private Color bannerColor = Color.WHITE;
    private boolean skidding;

    DerbyPlayScreen(GameWindow window) {
        this.window = window;
        setLayout(null);
        setFocusable(true);
        setPreferredSize(new Dimension(W, H));
        addKeyListener(this);
        continueButton = new GameWindow.PixelButton("CONTINUE", new Color(60, 170, 80));
        continueButton.setFont(font(22));
        continueButton.setBounds(360, 522, 240, 54);
        continueButton.setVisible(false);
        continueButton.addActionListener(e -> leave());
        add(continueButton);
        timer = new Timer(16, e -> tick());
        buildFloor();
    }

    void begin(DerbyCar car, String origin) {
        myCar = car;
        this.origin = origin;
        arena = new DerbyArena(car);
        outcome = null;
        paused = false;
        up = down = left = right = boost = brake = false;
        particles.clear();
        images.clear();
        wreckImages.clear();
        for (DerbyArena.DCar c : arena.cars) images.put(c, c.spec.topPicture(c.color));
        marks = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        shake = 0;
        bannerTime = 0;
        continueButton.setVisible(false);
        last = System.nanoTime();
        Sound.playMusic(Sound.MUSIC_RACE);
        Sound.engineStart();
        timer.start();
    }

    public void onShow() {
        requestFocusInWindow();
        if (paused && arena != null && outcome == null) {
            paused = false;
            last = System.nanoTime();
            up = down = left = right = boost = brake = false;
            Sound.playMusic(Sound.MUSIC_RACE);
            Sound.engineStart();
            timer.start();
        }
    }

    // ------------------------------------------------------------------
    private void buildFloor() {
        floor = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = floor.createGraphics();
        Random r = new Random(5);
        fill(g, new Color(30, 28, 38), 0, 0, W, H);
        // the stands: rings of concrete with crowds on them
        for (int ring = 0; ring < 10; ring++) {
            int rad = 330 + ring * 16;
            g.setColor(ring % 2 == 0 ? new Color(52, 50, 64) : new Color(44, 42, 56));
            g.setStroke(new BasicStroke(16));
            g.drawOval(CX - rad, CY - rad, 2 * rad, 2 * rad);
        }
        Color[] shirts = { new Color(220, 60, 60), new Color(60, 110, 220), new Color(240, 200, 50), Color.WHITE, new Color(70, 180, 100), new Color(230, 120, 40) };
        for (int i = 0; i < 1500; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double rad = 330 + r.nextInt(150);
            int x = (int) (CX + Math.cos(ang) * rad), y = (int) (CY + Math.sin(ang) * rad);
            if (x < 0 || x > W - 4 || y < 0 || y > H - 5) continue;
            fill(g, shirts[r.nextInt(shirts.length)], x, y, 3, 4);
            fill(g, new Color(235, 200, 170), x, y - 2, 3, 2);
        }
        // the dirt
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(150, 112, 72));
        g.fillOval(CX - 300, CY - 300, 600, 600);
        for (int i = 0; i < 2500; i++) {
            double ang = r.nextDouble() * Math.PI * 2, rad = Math.sqrt(r.nextDouble()) * 296;
            g.setColor(r.nextBoolean() ? new Color(132, 98, 62) : new Color(168, 128, 84));
            g.fillRect((int) (CX + Math.cos(ang) * rad), (int) (CY + Math.sin(ang) * rad), 2 + r.nextInt(3), 2);
        }
        g.dispose();
    }

    // ------------------------------------------------------------------
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - last) / 1e9);
        last = now;
        clock += dt;
        int steer = (right ? 1 : 0) - (left ? 1 : 0);
        int throttle = (up ? 1 : 0) - (down ? 1 : 0);
        arena.update(dt, steer, throttle, boost, brake);
        handleEvents();
        effects(dt);
        DerbyArena.DCar me = arena.player;
        Sound.engineSet(Math.min(1, me.speed() / (me.spec.top * 1.2)) * 0.8 + 0.2, up ? 1 : 0.2);
        if (arena.over && outcome == null) finish();
        repaint();
    }

    private void say(String s, Color c) {
        banner = s;
        bannerColor = c;
        bannerTime = 1.8;
    }

    private void handleEvents() {
        for (DerbyArena.Ev e : arena.events) {
            switch (e.type) {
                case DerbyArena.EV_COUNT: Sound.play(Sound.COUNT); break;
                case DerbyArena.EV_GO: Sound.play(Sound.GO); break;
                case DerbyArena.EV_HIT:
                    Sound.play(e.value > 220 ? Sound.CRASH : Sound.BUMP);
                    burst(e.x, e.y, (int) Math.min(18, 4 + e.value / 25), new Color(255, 210, 90), 1);
                    if (e.car != null && e.car.isPlayer) shake = Math.max(shake, Math.min(9, e.value / 50));
                    break;
                case DerbyArena.EV_WALL:
                    if (e.car != null && e.car.isPlayer) { Sound.play(Sound.BUMP); shake = Math.max(shake, 3); }
                    burst(e.x, e.y, 6, new Color(230, 230, 240), 1);
                    break;
                case DerbyArena.EV_WRECK:
                    Sound.play(Sound.CRASH);
                    burst(e.x, e.y, 30, new Color(255, 150, 50), 2);
                    burst(e.x, e.y, 14, new Color(90, 90, 96), 3);
                    shake = Math.max(shake, 7);
                    if (e.car.isPlayer) say("YOU ARE WRECKED!", new Color(255, 90, 80));
                    else if (e.car.lastHit != null && e.car.lastHit.isPlayer) say("KO!  +" + money(75), new Color(120, 255, 140));
                    else say(e.car.label.toUpperCase() + " IS OUT!", new Color(255, 200, 100));
                    break;
                case DerbyArena.EV_BOOST:
                    if (e.car != null && e.car.isPlayer) { Sound.play(Sound.NITRO); }
                    break;
                case DerbyArena.EV_SHRINK:
                    Sound.play(Sound.WARNING);
                    say("THE WALL IS CLOSING IN!", new Color(255, 120, 90));
                    break;
                default: break;
            }
        }
        arena.events.clear();
    }

    private void burst(double x, double y, int n, Color c, int kind) {
        if (!Settings.effects) return;
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            double a = fx.nextDouble() * Math.PI * 2, s = 40 + fx.nextDouble() * (kind == 1 ? 260 : 120);
            p.vx = Math.cos(a) * s;
            p.vy = Math.sin(a) * s;
            p.life = p.maxLife = (kind == 2 ? 0.7 : kind == 3 ? 1.0 : 0.35) + fx.nextDouble() * 0.3;
            p.size = kind == 3 ? 5 : kind == 2 ? 7 : 3;
            p.kind = kind;
            p.color = c;
            particles.add(p);
        }
    }

    private void effects(double dt) {
        if (bannerTime > 0) bannerTime -= dt;
        shake = Math.max(0, shake - 20 * dt);
        skidding = false;
        for (DerbyArena.DCar c : arena.cars) {
            double sp = c.speed();
            double lat = Math.abs(c.vx * Math.cos(c.h) + c.vy * Math.sin(c.h));
            boolean sliding = !c.wrecked && lat > 60;
            if (sliding && Settings.effects) {
                markTyre(c, -8, -12);
                markTyre(c, 8, -12);
                if (fx.nextInt(3) == 0) particle(c.x - c.fx() * 14, c.y - c.fy() * 14, 0, 8);
                if (c.isPlayer) skidding = true;
            }
            if (!c.wrecked && Settings.effects) {
                double ratio = c.hp / c.maxHp;
                if (ratio < 0.35 && fx.nextInt(4) == 0) particle(c.x + c.fx() * 14, c.y + c.fy() * 14, 2, 9);
                if (ratio < 0.18 && fx.nextInt(3) == 0) particle(c.x + c.fx() * 10, c.y + c.fy() * 10, 3, 7);
                if (c.boostT > 0) particle(c.x - c.fx() * 24, c.y - c.fy() * 24, 3, 6);
            }
            if (c.wrecked && Settings.effects && arena.time - c.wreckedAt < 12 && fx.nextInt(5) == 0) {
                particle(c.x, c.y, 2, 10);
            }
        }
        Sound.skid(skidding && Settings.sfxOn);
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) { particles.remove(i); continue; }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= Math.max(0, 1 - 2 * dt);
            p.vy *= Math.max(0, 1 - 2 * dt);
            if (p.kind == 0 || p.kind == 2) p.y -= 12 * dt;
        }
        while (particles.size() > 500) particles.remove(0);
    }

    private void particle(double x, double y, int kind, double size) {
        Particle p = new Particle();
        p.x = x + (fx.nextDouble() - 0.5) * 6;
        p.y = y + (fx.nextDouble() - 0.5) * 6;
        p.vx = (fx.nextDouble() - 0.5) * 20;
        p.vy = (fx.nextDouble() - 0.5) * 20;
        p.life = p.maxLife = 0.6 + fx.nextDouble() * 0.5;
        p.size = size;
        p.kind = kind == 3 ? 2 : kind;
        p.color = kind == 3 ? new Color(255, 160, 50) : kind == 2 ? new Color(50, 50, 56) : Color.WHITE;
        if (kind == 2) p.kind = 0;
        particles.add(p);
    }

    private void markTyre(DerbyArena.DCar c, double side, double back) {
        double rx = Math.cos(c.h), ry = Math.sin(c.h);
        double x = c.x + rx * side + c.fx() * back;
        double y = c.y + ry * side + c.fy() * back;
        Graphics2D g = marks.createGraphics();
        g.setColor(new Color(40, 28, 20, 70));
        g.fillRect((int) (CX + x) - 1, (int) (CY + y) - 1, 3, 3);
        g.dispose();
    }

    // ------------------------------------------------------------------
    private void finish() {
        timer.stop();
        Sound.engineStop();
        Sound.skid(false);
        DerbyArena.DCar me = arena.player;
        outcome = window.getCareer().finishDerby(window.getDriver(), myCar, me.place, me.kos);
        Sound.play(me.place == 1 ? Sound.FINISH_WIN : Sound.FINISH_LOSE);
        Sound.playMusic(Sound.MUSIC_MENU);
        continueButton.setVisible(true);
        repaint();
    }

    private void leave() {
        Career.DerbyOutcome o = outcome;
        continueButton.setVisible(false);
        if (o.prize + o.koBonus > 0) Sound.play(Sound.CASH);
        for (Career.Challenge c : o.challenges) {
            Sound.play(Sound.CASH);
            window.message("SPONSOR DEAL!", c.sponsor + " is happy!\n\"" + c.title + "\" complete: " + c.text
                    + ".\n\nYou earn " + money(c.money) + " and +" + c.rep + " fame.");
        }
        window.showScreen(origin);
    }

    private void pause() {
        timer.stop();
        paused = true;
        Sound.engineStop();
        Sound.skid(false);
        int c = window.choose("PAUSED", "Take a breath! What would you like to do?", "RESUME", "SETTINGS", "GIVE UP");
        if (c == 1) {
            window.showSettings();
            return;
        }
        if (c == 2 && window.confirm("GIVE UP?", "Your car is scrapped and you finish last with no prize.", "GIVE UP", "KEEP SMASHING")) {
            paused = false;
            arena.player.hp = 0;
            arena.player.place = arena.alive();
            arena.over = true;
            arena.player.wrecked = true;
            finish();
            return;
        }
        paused = false;
        last = System.nanoTime();
        up = down = left = right = boost = brake = false;
        Sound.engineStart();
        timer.start();
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    private boolean arrows() { return Settings.controls != Settings.CONTROLS_WASD; }
    private boolean wasd() { return Settings.controls != Settings.CONTROLS_ARROWS; }

    private void setKey(int k, boolean on) {
        if ((arrows() && k == KeyEvent.VK_UP) || (wasd() && k == KeyEvent.VK_W)) up = on;
        else if ((arrows() && k == KeyEvent.VK_DOWN) || (wasd() && k == KeyEvent.VK_S)) down = on;
        else if ((arrows() && k == KeyEvent.VK_LEFT) || (wasd() && k == KeyEvent.VK_A)) left = on;
        else if ((arrows() && k == KeyEvent.VK_RIGHT) || (wasd() && k == KeyEvent.VK_D)) right = on;
        else if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_N) boost = on;
        else if (k == KeyEvent.VK_SHIFT || k == KeyEvent.VK_X) brake = on;
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if ((k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_P) && outcome == null && !paused) {
            pause();
            return;
        }
        setKey(k, true);
    }

    public void keyReleased(KeyEvent e) { setKey(e.getKeyCode(), false); }

    public void keyTyped(KeyEvent e) { }

    // ------------------------------------------------------------------
    // drawing
    // ------------------------------------------------------------------
    private BufferedImage wreckImage(DerbyArena.DCar c) {
        BufferedImage w = wreckImages.get(c);
        if (w == null) {
            BufferedImage src = images.get(c);
            w = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int p = src.getRGB(x, y);
                    int a = (p >>> 24) & 0xFF;
                    int r = (int) (((p >> 16) & 0xFF) * 0.32), gg = (int) (((p >> 8) & 0xFF) * 0.3), b = (int) ((p & 0xFF) * 0.3);
                    w.setRGB(x, y, (a << 24) | (r << 16) | (gg << 8) | b);
                }
            }
            wreckImages.put(c, w);
        }
        return w;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (arena == null) return;
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform base = g2.getTransform();
        if (shake > 0.5) g2.translate((fx.nextDouble() - 0.5) * shake, (fx.nextDouble() - 0.5) * shake);
        g2.drawImage(floor, 0, 0, null);
        g2.drawImage(marks, 0, 0, null);
        drawWall(g2);
        // wrecks first, then living cars
        for (DerbyArena.DCar c : arena.cars) if (c.wrecked) drawCar(g2, c);
        for (DerbyArena.DCar c : arena.cars) if (!c.wrecked) drawCar(g2, c);
        drawParticles(g2);
        g2.setTransform(base);
        drawHud(g2);
        if (outcome != null) drawResults(g2);
    }

    private void drawWall(Graphics2D g) {
        double r = arena.radius;
        if (r < DerbyArena.R0 - 1) {
            // the part of the arena the wall has already taken
            g.setColor(new Color(120, 20, 20, 120));
            g.setStroke(new BasicStroke((float) (DerbyArena.R0 - r + 14)));
            double mid = (DerbyArena.R0 + r) / 2 + 7;
            g.drawOval((int) (CX - mid), (int) (CY - mid), (int) (2 * mid), (int) (2 * mid));
        }
        g.setStroke(new BasicStroke(14, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        int segs = 80;
        for (int i = 0; i < segs; i++) {
            g.setColor(i % 2 == 0 ? new Color(214, 54, 54) : new Color(240, 240, 240));
            g.drawArc((int) (CX - r - 6), (int) (CY - r - 6), (int) (2 * r + 12), (int) (2 * r + 12), i * 360 / segs, 360 / segs + 1);
        }
        g.setStroke(new BasicStroke(1));
        // a row of tyres along the wall
        for (int i = 0; i < 60; i++) {
            double a = i * Math.PI * 2 / 60;
            int x = (int) (CX + Math.cos(a) * (r + 18)), y = (int) (CY + Math.sin(a) * (r + 18));
            fill(g, new Color(20, 20, 24), x - 6, y - 6, 12, 12);
            fill(g, new Color(70, 70, 78), x - 2, y - 2, 4, 4);
        }
    }

    private void drawCar(Graphics2D g, DerbyArena.DCar c) {
        int x = (int) Math.round(CX + c.x), y = (int) Math.round(CY + c.y);
        BufferedImage img = c.wrecked ? wreckImage(c) : images.get(c);
        double scale = 1.7;
        int w = (int) (img.getWidth() * scale), h = (int) (img.getHeight() * scale);
        if (c.isPlayer && !c.wrecked) {
            g.setColor(new Color(255, 235, 90, (int) (110 + 60 * Math.sin(clock * 6))));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - 30, y - 30, 60, 60);
            g.setStroke(new BasicStroke(1));
        }
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x - 22, y - 24, 44, 52);
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(c.h);
        g.drawImage(img, -w / 2, -h / 2, w, h, null);
        g.setTransform(old);
        if (!c.wrecked) {
            // damage bar and name
            int bw = 34;
            fill(g, Color.BLACK, x - bw / 2 - 1, y - 38, bw + 2, 6);
            double ratio = Math.max(0, c.hp / c.maxHp);
            fill(g, ratio > 0.6 ? new Color(90, 220, 110) : ratio > 0.3 ? new Color(240, 190, 50) : new Color(230, 60, 60), x - bw / 2, y - 37, (int) (bw * ratio), 4);
            textCenter(g, c.label, x, y - 42, 10, c.isPlayer ? new Color(255, 240, 120) : Color.WHITE);
        }
    }

    private void drawParticles(Graphics2D g) {
        if (!Settings.effects) return;
        for (Particle p : particles) {
            double t = p.life / p.maxLife;
            int s = (int) Math.max(2, p.size * (p.kind == 0 ? 1.5 - t * 0.5 : 1));
            Color c;
            if (p.kind == 0) c = new Color(60, 60, 66, (int) (170 * t));
            else if (p.kind == 2) c = t > 0.5 ? new Color(255, 220, 90, 230) : new Color(255, 110, 30, (int) (230 * t * 2));
            else if (p.kind == 3) c = new Color(60, 60, 66, 240);
            else c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (255 * Math.min(1, t * 1.5)));
            g.setColor(c);
            g.fillRect((int) (CX + p.x) - s / 2, (int) (CY + p.y) - s / 2, s, s);
        }
    }

    private void drawHud(Graphics2D g) {
        Color panel = new Color(20, 24, 48, 215), edge = new Color(120, 160, 255);
        DerbyArena.DCar me = arena.player;
        box(g, 12, 10, 230, 92, panel, edge);
        text(g, arena.alive() + " LEFT", 24, 48, 32, new Color(255, 240, 170), true);
        int t = (int) arena.time;
        text(g, String.format("TIME %d:%02d     KOs %d", t / 60, t % 60, me.kos), 24, 72, 15, Color.WHITE, false);
        text(g, "DEMOLITION DERBY", 24, 92, 11, new Color(190, 200, 230), false);

        box(g, 738, 10, 210, 92, panel, edge);
        text(g, me.spec.name.toUpperCase(), 750, 32, 13, Color.WHITE, true);
        double ratio = Math.max(0, me.hp / me.maxHp);
        fill(g, Color.BLACK, 750, 42, 186, 18);
        fill(g, ratio > 0.6 ? new Color(90, 220, 110) : ratio > 0.3 ? new Color(240, 190, 50) : new Color(230, 60, 60), 752, 44, (int) (182 * ratio), 14);
        text(g, "ARMOR " + (int) Math.round(ratio * 100) + "%", 750, 80, 14, Color.WHITE, false);
        text(g, "Shrinking wall at 0:45", 750, 96, 10, new Color(150, 165, 200), false);

        box(g, 738, 108, 210, 40, panel, edge);
        double frac;
        String label;
        Color col;
        if (me.boostT > 0) { frac = me.boostT / 1.2; label = "NITRO!"; col = new Color(255, 190, 70); }
        else if (me.boostCool > 0) { frac = 1 - me.boostCool / 6.0; label = "NITRO RECHARGING"; col = new Color(80, 130, 220); }
        else { frac = 1; label = "NITRO READY [SPACE]"; col = new Color(100, 230, 120); }
        text(g, label, 750, 124, 12, Color.WHITE, false);
        fill(g, new Color(50, 54, 70), 750, 130, 186, 10);
        fill(g, col, 751, 131, (int) (184 * Math.max(0, Math.min(1, frac))), 8);

        box(g, 170, 604, 620, 28, panel, edge);
        textCenter(g, "SHIFT slide    SPACE nitro    Hit from the side or back!    ESC pause", 480, 623, 11, new Color(190, 200, 230));

        if (bannerTime > 0 && outcome == null) {
            int a = (int) Math.min(255, bannerTime * 400);
            textCenter(g, banner, 480, 150, 34, new Color(bannerColor.getRed(), bannerColor.getGreen(), bannerColor.getBlue(), Math.max(0, a)));
        }
        if (arena.countdown > 0) {
            double cd = arena.countdown;
            String s = cd > 2.5 ? "3" : cd > 1.5 ? "2" : cd > 0.5 ? "1" : "SMASH!";
            textCenter(g, s, 480, 330, 100, cd > 0.5 ? new Color(255, 220, 100) : new Color(120, 255, 140));
            box(g, 220, 380, 520, 124, new Color(20, 24, 48, 225), new Color(255, 230, 120));
            textCenter(g, "BE THE LAST CAR STANDING!", 480, 410, 20, new Color(255, 240, 170));
            textCenter(g, "Heavy cars hit harder. The nose is tough, the sides and back are weak.", 480, 436, 13, Color.WHITE);
            textCenter(g, "Dodge, run away, then strike!  The wall closes in after 45 seconds.", 480, 458, 13, Color.WHITE);
            textCenter(g, (arrows() ? "ARROWS" : "WASD") + " drive    SHIFT handbrake    SPACE nitro", 480, 486, 14, new Color(190, 220, 255));
        }
    }

    private void drawResults(Graphics2D g) {
        Career.DerbyOutcome o = outcome;
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, W, H);
        box(g, 170, 24, 620, 486, new Color(24, 28, 56), new Color(255, 230, 120));
        textCenter(g, "DEMOLITION DERBY", 480, 62, 26, new Color(255, 240, 170));
        textCenter(g, o.car.name.toUpperCase(), 480, 84, 14, new Color(150, 165, 200));
        g.drawImage(o.car.sidePicture(), 380, 92, 200, 83, null);
        String head = o.place == 1 ? "LAST CAR STANDING!" : ordinal(o.place) + " PLACE";
        Color hc = o.place == 1 ? new Color(120, 255, 140) : o.place <= 3 ? new Color(255, 220, 100) : new Color(255, 130, 110);
        textCenter(g, head, 480, 232, o.place == 1 ? 40 : 50, hc);
        int y = 290;
        text(g, "Cars wrecked", 214, y, 17, Color.WHITE, false);
        textRight(g, String.valueOf(o.kos), 746, y, 17, Color.WHITE);
        y += 28;
        text(g, "Prize money", 214, y, 17, Color.WHITE, false);
        textRight(g, money(o.prize), 746, y, 17, o.prize > 0 ? new Color(120, 240, 130) : new Color(150, 150, 160));
        y += 28;
        text(g, "KO bonus", 214, y, 17, Color.WHITE, false);
        textRight(g, money(o.koBonus), 746, y, 17, o.koBonus > 0 ? new Color(120, 240, 130) : new Color(150, 150, 160));
        y += 28;
        text(g, "Fame", 214, y, 17, Color.WHITE, false);
        textRight(g, "+" + o.repGain, 746, y, 17, new Color(190, 170, 255));
        y += 34;
        text(g, "The " + o.car.name + " was scrapped. You can buy another one!", 214, y, 14, new Color(190, 200, 230), false);
        if (!o.challenges.isEmpty()) text(g, "Sponsor deals done: " + o.challenges.size() + "!", 214, y + 26, 16, new Color(255, 200, 90), false);
    }
}
