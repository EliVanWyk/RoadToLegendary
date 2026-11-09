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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * THE PLAYABLE RACE: the screen that drives and draws every kind of race.
 * Race.java does the driving rules; this class draws the road, the cars,
 * the HUD (speedometer, minimap, boost bars...), the effects (skid marks,
 * smoke, sparks, rain) and plays the sounds.
 */
class RaceScreen extends JPanel implements GameWindow.Screen, KeyListener {

    private static final int W = GameWindow.W, H = GameWindow.H;
    private static final int STEP = 2;          // road is drawn in 2-pixel strips
    private static final int PLAYER_Y = 470;    // where the player's car sits on screen

    private final GameWindow window;
    private Race race;
    private final Timer timer;
    private long last;
    private boolean up, down, left, right, drift, nitro;
    private boolean paused;
    private TexturePaint groundLeft, groundRight;
    private Career.RaceOutcome outcome;
    private final GameWindow.PixelButton continueButton;
    private int wager;
    private String origin = "city";
    private final Random fx = new Random();

    // ---- effects ----
    private static class Particle {
        double dist, x, vd, vx, life, maxLife, size;
        int kind;               // 0 smoke, 1 spark, 2 dark smoke, 3 flame
        Color color;
    }
    private final ArrayList<Particle> particles = new ArrayList<Particle>();
    private final ArrayList<double[]> skids = new ArrayList<double[]>();     // {dist, offset}
    private final double[][] rain = new double[80][2];
    private double shake;
    private String banner = "";
    private double bannerTime;
    private Color bannerColor = Color.WHITE;
    private double clock;                // animation clock
    private boolean skidding;

    RaceScreen(GameWindow window) {
        this.window = window;
        setLayout(null);
        setFocusable(true);
        setPreferredSize(new Dimension(W, H));
        addKeyListener(this);
        continueButton = new GameWindow.PixelButton("CONTINUE", new Color(60, 170, 80));
        continueButton.setFont(font(22));
        continueButton.setBounds(360, 522, 240, 54);
        continueButton.setVisible(false);
        continueButton.addActionListener(e -> finishAndLeave());
        add(continueButton);
        timer = new Timer(16, e -> tick());
        for (double[] d : rain) {
            d[0] = fx.nextInt(W);
            d[1] = fx.nextInt(H);
        }
    }

    void begin(Race r, int wager, String origin) {
        this.race = r;
        this.wager = wager;
        this.origin = origin;
        Driver d = window.getDriver();
        race.setPlayerSkills(d.getSkill(Driver.SKILL_DRIFT), d.getSkill(Driver.SKILL_NITRO), d.getSkill(Driver.SKILL_DRAFT));
        race.prepare(d.getCurrentCar(), d.getName());
        outcome = null;
        paused = false;
        up = down = left = right = drift = nitro = false;
        particles.clear();
        skids.clear();
        shake = 0;
        banner = "";
        bannerTime = 0;
        skidding = false;
        continueButton.setVisible(false);
        groundLeft = makeGround(Race.getGroundColors(r.getTheme(), -1));
        groundRight = makeGround(Race.getGroundColors(r.getTheme(), 1));
        last = System.nanoTime();
        Sound.playMusic(Sound.MUSIC_RACE);
        Sound.engineStart();
        timer.start();
    }

    public void onShow() {
        requestFocusInWindow();
        // coming back from the settings screen during a race
        if (paused && race != null && outcome == null) {
            paused = false;
            last = System.nanoTime();
            up = down = left = right = drift = nitro = false;
            Sound.playMusic(Sound.MUSIC_RACE);
            Sound.engineStart();
            timer.start();
        }
    }

    /** A small repeating pixel pattern used for grass, water, pavement... */
    private TexturePaint makeGround(Color[] colors) {
        BufferedImage tile = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        fill(g, colors[0], 0, 0, 32, 32);
        fill(g, colors[1], 0, 0, 16, 16);
        fill(g, colors[1], 16, 16, 16, 16);
        Random r = new Random(7);
        for (int i = 0; i < 14; i++) fill(g, colors[2], r.nextInt(30), r.nextInt(30), 2, 2);
        g.dispose();
        return new TexturePaint(tile, new Rectangle2D.Double(0, 0, 32, 32));
    }

    // ------------------------------------------------------------------
    // The game loop
    // ------------------------------------------------------------------
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - last) / 1e9);
        last = now;
        clock += dt;
        int steer = (right ? 1 : 0) - (left ? 1 : 0);
        race.update(dt, steer, up, down, drift, nitro);
        handleEvents();
        updateEffects(dt);
        updateSound();
        if (race.isOver() && outcome == null) finishRace();
        repaint();
    }

    private void handleEvents() {
        for (Race.Event e : race.drainEvents()) {
            switch (e.type) {
                case Race.EV_COUNT: Sound.play(Sound.COUNT); break;
                case Race.EV_GO: Sound.play(Sound.GO); break;
                case Race.EV_WALL:
                    Sound.play(Sound.CRASH);
                    sparks(e.who, 16, new Color(255, 190, 60));
                    shake = Math.min(10, 4 + e.value / 60.0);
                    break;
                case Race.EV_BUMP:
                    Sound.play(Sound.BUMP);
                    sparks(e.who, 7, new Color(240, 240, 255));
                    shake = Math.max(shake, 3);
                    break;
                case Race.EV_TRAFFIC:
                    Sound.play(Sound.CRASH);
                    sparks(e.who, 10, new Color(255, 210, 120));
                    shake = Math.max(shake, 5);
                    break;
                case Race.EV_NITRO:
                    Sound.play(Sound.NITRO);
                    say("NITRO!", new Color(255, 170, 60));
                    break;
                case Race.EV_BOOST:
                    Sound.play(Sound.BOOST);
                    say("DRIFT BOOST!", new Color(90, 220, 255));
                    break;
                case Race.EV_CHECKPOINT:
                    Sound.play(Sound.CHECKPOINT);
                    say("CHECKPOINT " + (int) e.value + "/" + race.getCheckpointCount() + "   TIME +" + String.format("%.0f", cpBonus()) + "s",
                            new Color(120, 255, 150));
                    break;
                case Race.EV_ELIMINATED:
                    Sound.play(Sound.ELIMINATED);
                    say(e.who.isPlayer ? "YOU ARE OUT!" : e.who.name.toUpperCase() + " IS OUT!", new Color(255, 90, 80));
                    break;
                case Race.EV_WARNING: Sound.play(Sound.WARNING); break;
                case Race.EV_SIREN: Sound.play(Sound.SIREN); break;
                case Race.EV_BUSTED:
                    Sound.play(Sound.CRASH);
                    say("BUSTED!", new Color(255, 90, 80));
                    break;
                case Race.EV_TIMEUP:
                    Sound.play(Sound.ELIMINATED);
                    say("TIME'S UP!", new Color(255, 90, 80));
                    break;
                default: break;
            }
        }
    }

    private double cpBonus() {
        // the bonus is not stored directly: it is what the timer just grew by
        Race.Racer me = race.getPlayer();
        return race.getCheckpointSpacing() / (0.86 * Math.max(120, me.car.getTopSpeedPx())) + 1.2;
    }

    private void say(String s, Color c) {
        banner = s;
        bannerColor = c;
        bannerTime = 1.6;
    }

    private void sparks(Race.Racer who, int n, Color c) {
        if (!Settings.effects || who == null) return;
        double x = race.roadCenter(who.dist) + who.offset;
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            p.dist = who.dist + fx.nextDouble() * 30 - 5;
            p.x = x + (who.offset > 0 ? 14 : -14) + fx.nextDouble() * 8 - 4;
            p.vd = who.speed * 0.6 + fx.nextDouble() * 120 - 40;
            p.vx = (fx.nextDouble() - 0.5) * 220;
            p.life = p.maxLife = 0.25 + fx.nextDouble() * 0.3;
            p.size = 3;
            p.kind = 1;
            p.color = c;
            particles.add(p);
        }
    }

    private void addSmoke(double dist, double x, int kind, double size, double life) {
        Particle p = new Particle();
        p.dist = dist;
        p.x = x;
        p.vd = 0;
        p.vx = (fx.nextDouble() - 0.5) * 30;
        p.life = p.maxLife = life;
        p.size = size;
        p.kind = kind;
        particles.add(p);
    }

    private void updateEffects(double dt) {
        if (bannerTime > 0) bannerTime -= dt;
        shake = Math.max(0, shake - 22 * dt);
        Race.Racer me = race.getPlayer();
        boolean raceOn = race.getCountdown() <= 0 && outcome == null;

        if (Settings.effects && raceOn && !me.finished) {
            double cx = race.roadCenter(me.dist) + me.offset;
            boolean braking = down && me.speed > 170;
            boolean slidey = me.surf == Race.SURF_ICE && Math.abs(me.vx) > 70;
            skidding = me.drifting || braking || slidey;
            if (skidding) {
                skids.add(new double[] { me.dist - 18, me.offset - 10 });
                skids.add(new double[] { me.dist - 18, me.offset + 10 });
                while (skids.size() > 800) skids.remove(0);
                if (fx.nextInt(2) == 0) {
                    addSmoke(me.dist - 20, cx - 11, 0, 8 + fx.nextInt(6), 0.7);
                    addSmoke(me.dist - 20, cx + 11, 0, 8 + fx.nextInt(6), 0.7);
                }
            }
            if (me.car.getCondition() < 35 && fx.nextInt(3) == 0) {
                addSmoke(me.dist + 26, cx, 2, 9 + fx.nextInt(8), 0.9);
            }
            if (me.nitroTime > 0 || me.boostTime > 0) {
                for (int i = 0; i < 2; i++) {
                    Particle p = new Particle();
                    p.dist = me.dist - 28;
                    p.x = cx + (fx.nextDouble() - 0.5) * 8;
                    p.vd = -60 - fx.nextDouble() * 80;
                    p.vx = (fx.nextDouble() - 0.5) * 40;
                    p.life = p.maxLife = 0.25;
                    p.size = 6 + fx.nextInt(5);
                    p.kind = 3;
                    particles.add(p);
                }
            }
            // opponents that drift also leave smoke and skid marks
            for (Race.Racer o : race.getRacers()) {
                if (o.isPlayer || o.eliminated || !o.drifting || Math.abs(o.dist - me.dist) > 520) continue;
                double ox = race.roadCenter(o.dist) + o.offset;
                if (fx.nextInt(3) == 0) {
                    addSmoke(o.dist - 20, ox - 11, 0, 8, 0.6);
                    addSmoke(o.dist - 20, ox + 11, 0, 8, 0.6);
                }
                skids.add(new double[] { o.dist - 18, o.offset - 10 });
                skids.add(new double[] { o.dist - 18, o.offset + 10 });
            }
            while (skids.size() > 800) skids.remove(0);
        } else {
            skidding = false;
        }
        Sound.skid(skidding && Settings.sfxOn);

        // move the particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            p.dist += p.vd * dt;
            p.x += p.vx * dt;
        }
        while (particles.size() > 400) particles.remove(0);

        // rain falls on wet road
        if (me.surf == Race.SURF_WET && Settings.effects) {
            for (double[] d : rain) {
                d[1] += 900 * dt;
                d[0] -= 120 * dt;
                if (d[1] > H) { d[1] = -10; d[0] = fx.nextInt(W + 100); }
                if (d[0] < -10) d[0] = W;
            }
        }
    }

    private void updateSound() {
        Race.Racer me = race.getPlayer();
        double gearSpan = Math.max(1, me.topSpeed * 1.15 / 6.0);
        int gear = (int) Math.min(5, me.speed / gearSpan);
        double inGear = Math.min(1.0, (me.speed - gear * gearSpan) / gearSpan);
        double rpm = 0.22 + 0.78 * Math.max(0, inGear);
        if (race.getCountdown() > 0) rpm = up ? 0.7 : 0.22;
        Sound.engineSet(rpm, up ? 1.0 : 0.15);
    }

    private void finishRace() {
        timer.stop();
        Sound.engineStop();
        Sound.skid(false);
        int place = race.getPlayerPlace();
        outcome = window.getCareer().finishRace(window.getDriver(), race, place, race.getRacerCount(), wager);
        boolean good = race.getMode() == Race.MODE_TRIAL ? outcome.medal > 0 : place == 1;
        Sound.play(good ? Sound.FINISH_WIN : Sound.FINISH_LOSE);
        Sound.playMusic(Sound.MUSIC_MENU);
        continueButton.setVisible(true);
        repaint();
    }

    /** After the results: offer the retro car, announce news, go back to where the race started. */
    private void finishAndLeave() {
        Career.RaceOutcome o = outcome;
        continueButton.setVisible(false);
        Sound.play(Sound.CLICK);
        if (o.prize > 0 || o.wagerNet > 0 || o.cupPrize > 0) Sound.play(Sound.CASH);
        if (o.rewardCar != null) {
            Car rc = o.rewardCar;
            boolean keep = window.confirm("SPECIAL REWARD!", "You discovered a " + rc.getName() + "!\nCondition: "
                    + rc.getCondition() + "%\n\nWould you like to keep it?", "KEEP IT", "LEAVE IT");
            if (keep) {
                window.getCareer().claimReward(window.getDriver(), race, rc);
                window.message("NEW CAR!", "The " + rc.getName() + " is now in your garage. Fix it up at the Repair Shop!");
            }
        }
        for (Career.Challenge c : o.challenges) {
            Sound.play(Sound.CASH);
            window.message("SPONSOR DEAL!", c.sponsor + " is happy!\n\"" + c.title + "\" complete: " + c.text
                    + ".\n\nYou earn " + money(c.money) + " and +" + c.rep + " fame.");
        }
        if (o.cupFinished) {
            String[] p = { "", "CHAMPION!", "2nd place", "3rd place", "4th place", "5th place", "6th place" };
            window.message("CUP FINISHED", "You finished the " + Career.CUP_NAMES[race.getCup()] + " in "
                    + p[Math.min(6, Math.max(1, o.cupPlace))] + "!"
                    + (o.cupPrize > 0 ? "\nCup prize: " + money(o.cupPrize) : "\nTop 3 get a cup prize - try again!"));
        }
        if (o.rankedUp) {
            Sound.play(Sound.RANKUP);
            window.message("RANK UP!", "Congratulations! You are now a " + Career.RANK_NAMES[o.newRank].toUpperCase() + " racer!"
                    + (o.paintShopUnlocked ? "\nThe Paint Shop is now open!" : "")
                    + "\nNew cars are available in the Car Shop.");
        }
        for (int b : o.newBuildings) {
            Sound.play(Sound.RANKUP);
            window.message("NEW BUILDING!", "The " + CityMap.BUILDING_NAMES[b] + " is now open in the city!");
        }
        if (o.careerComplete) {
            window.message("LEGENDARY!", "You won the Legendary rank and all 25 career races. You are a LEGENDARY RACER!");
        }
        window.showScreen(origin);
    }

    private void pause() {
        timer.stop();
        paused = true;
        Sound.engineStop();
        Sound.skid(false);
        int c = window.choose("PAUSED", "Take a breath! What would you like to do?", "RESUME", "SETTINGS", "QUIT RACE");
        if (c == 1) {
            window.showSettings();
            return;                      // onShow() resumes the race when we come back
        }
        if (c == 2) {
            boolean yes = window.confirm("QUIT RACE?", race.getMode() == Race.MODE_TRIAL
                    ? "If you quit now this time trial does not count."
                    : "If you quit now you finish in last place and earn no prize.", "QUIT", "KEEP RACING");
            if (yes) {
                paused = false;
                race.forfeit();
                finishRace();
                return;
            }
        }
        paused = false;
        last = System.nanoTime();
        up = down = left = right = drift = nitro = false;
        Sound.engineStart();
        timer.start();
        requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------
    private boolean arrows() { return Settings.controls != Settings.CONTROLS_WASD; }
    private boolean wasd() { return Settings.controls != Settings.CONTROLS_ARROWS; }

    private void setKey(int k, boolean on) {
        if ((arrows() && k == KeyEvent.VK_UP) || (wasd() && k == KeyEvent.VK_W)) up = on;
        else if ((arrows() && k == KeyEvent.VK_DOWN) || (wasd() && k == KeyEvent.VK_S)) down = on;
        else if ((arrows() && k == KeyEvent.VK_LEFT) || (wasd() && k == KeyEvent.VK_A)) left = on;
        else if ((arrows() && k == KeyEvent.VK_RIGHT) || (wasd() && k == KeyEvent.VK_D)) right = on;
        else if (k == KeyEvent.VK_SHIFT || k == KeyEvent.VK_X) drift = on;
        else if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_N) nitro = on;
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
    // Drawing the race
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (race == null || race.getPlayer() == null) return;
        Graphics2D g2 = (Graphics2D) g;
        Race.Racer me = race.getPlayer();
        double cam = Math.floor(me.dist / STEP) * STEP;

        AffineTransform base = g2.getTransform();
        if (shake > 0.5) g2.translate((fx.nextDouble() - 0.5) * shake, (fx.nextDouble() - 0.5) * shake);
        drawRoad(g2, cam);
        if (Settings.effects) drawSkids(g2, cam);
        drawGates(g2, cam);
        drawScenery(g2, cam);
        drawParticles(g2, cam, 0);
        drawCars(g2, me);
        drawParticles(g2, cam, 1);
        if (race.getMode() == Race.MODE_POLICE && race.getBust() > 0 && outcome == null) {
            boolean flip = ((int) (clock * 6) & 1) == 0;
            int a = (int) (30 + race.getBust());
            g2.setColor(flip ? new Color(255, 40, 40, Math.min(120, a)) : new Color(60, 110, 255, Math.min(120, a)));
            g2.fillRect(0, 0, 60, H);
            g2.setColor(flip ? new Color(60, 110, 255, Math.min(120, a)) : new Color(255, 40, 40, Math.min(120, a)));
            g2.fillRect(W - 60, 0, 60, H);
        }
        if (race.isNight()) drawNight(g2, me);
        if (me.surf == Race.SURF_WET && Settings.effects) drawRain(g2);
        g2.setTransform(base);

        drawHud(g2, me);
        if (outcome != null) drawResults(g2);
    }

    private Color[] roadColors(int surf) {
        switch (surf) {
            case Race.SURF_WET: return new Color[] { new Color(52, 60, 84), new Color(47, 55, 79) };
            case Race.SURF_GRAVEL: return new Color[] { new Color(126, 110, 88), new Color(118, 102, 82) };
            case Race.SURF_ICE: return new Color[] { new Color(158, 198, 222), new Color(148, 190, 216) };
            default: return new Color[] { new Color(70, 70, 80), new Color(64, 64, 74) };
        }
    }

    private void drawRoad(Graphics2D g, double cam) {
        double scroll = ((PLAYER_Y + cam) % 32 + 32) % 32;
        groundLeft = new TexturePaint(groundLeft.getImage(), new Rectangle2D.Double(0, scroll, 32, 32));
        groundRight = new TexturePaint(groundRight.getImage(), new Rectangle2D.Double(0, scroll, 32, 32));
        int edge = Race.ROAD_HALF + Race.BARRIER_WIDTH;

        // ground on both sides of the road
        g.setPaint(groundLeft);
        for (int y = 0; y < H; y += STEP) {
            int cx = (int) Math.round(race.roadCenter(cam + PLAYER_Y - y - 1));
            g.fillRect(-20, y, Math.max(0, cx - edge) + 20, STEP);
        }
        g.setPaint(groundRight);
        for (int y = 0; y < H; y += STEP) {
            int cx = (int) Math.round(race.roadCenter(cam + PLAYER_Y - y - 1));
            g.fillRect(cx + edge, y, W - (cx + edge) + 20, STEP);
        }

        Color red = new Color(214, 54, 54), white = new Color(240, 240, 240);
        for (int y = 0; y < H; y += STEP) {
            double d = cam + PLAYER_Y - y - 1;
            int cx = (int) Math.round(race.roadCenter(d));
            int l = cx - Race.ROAD_HALF, r = cx + Race.ROAD_HALF;
            boolean alt = ((int) Math.floor(d / 32.0) & 1) == 0;
            int surf = race.surfaceAt(d);
            Color[] rc = roadColors(surf);
            // barriers (red/white blocks)
            g.setColor(Color.BLACK);
            g.fillRect(l - Race.BARRIER_WIDTH - 2, y, 2, STEP);
            g.fillRect(r + Race.BARRIER_WIDTH, y, 2, STEP);
            g.setColor(alt ? red : white);
            g.fillRect(l - Race.BARRIER_WIDTH, y, Race.BARRIER_WIDTH, STEP);
            g.fillRect(r, y, Race.BARRIER_WIDTH, STEP);
            // tarmac (colour depends on the surface)
            g.setColor(alt ? rc[0] : rc[1]);
            g.fillRect(l, y, 2 * Race.ROAD_HALF, STEP);
            if (surf != Race.SURF_DRY) {
                int h = hash(race.getSeed(), (int) (d / 6), surf, 3);
                int sx = l + 8 + (h % (2 * Race.ROAD_HALF - 24));
                if (surf == Race.SURF_WET) {
                    g.setColor(new Color(120, 150, 210, 110));
                    g.fillRect(sx, y, 14 + (h >> 9) % 18, STEP);
                } else if (surf == Race.SURF_GRAVEL) {
                    g.setColor(new Color(170, 150, 120));
                    g.fillRect(sx, y, 3, STEP);
                    g.setColor(new Color(90, 78, 62));
                    g.fillRect(l + 8 + ((h >> 5) % (2 * Race.ROAD_HALF - 24)), y, 2, STEP);
                } else {
                    g.setColor(new Color(240, 250, 255, 170));
                    g.fillRect(sx, y, 10 + (h >> 9) % 24, STEP);
                }
            }
            // edge lines + dashed lane lines
            g.setColor(white);
            g.fillRect(l + 6, y, 4, STEP);
            g.fillRect(r - 10, y, 4, STEP);
            if (((int) Math.floor(d / 40.0) & 1) == 0) {
                g.fillRect(cx - 52, y, 4, STEP);
                g.fillRect(cx + 48, y, 4, STEP);
                g.setColor(new Color(240, 210, 70));
                g.fillRect(cx - 2, y, 4, STEP);
            }
            // start line and finish line
            if (d >= 0 && d < 14) {
                g.setColor(white);
                g.fillRect(l + 12, y, 2 * Race.ROAD_HALF - 24, STEP);
            }
            if (race.getMode() != Race.MODE_KNOCKOUT && d >= race.getLength() && d < race.getLength() + 36) {
                int row = (int) ((d - race.getLength()) / 18);
                for (int bx = l + 12, i = 0; bx < r - 12; bx += 18, i++) {
                    g.setColor(((i + row) & 1) == 0 ? Color.BLACK : white);
                    g.fillRect(bx, y, Math.min(18, r - 12 - bx), STEP);
                }
            }
        }
    }

    private void drawSkids(Graphics2D g, double cam) {
        g.setColor(new Color(15, 15, 20, 120));
        for (double[] s : skids) {
            int y = (int) Math.round(PLAYER_Y + cam - s[0]);
            if (y < -8 || y > H + 8) continue;
            int x = (int) Math.round(race.roadCenter(s[0]) + s[1]);
            g.fillRect(x - 2, y, 4, 6);
        }
    }

    /** Checkpoint gates (bright bands across the road with a sign overhead). */
    private void drawGates(Graphics2D g, double cam) {
        double sp = race.getCheckpointSpacing();
        if (race.getMode() != Race.MODE_CHECKPOINT || sp <= 0) return;
        for (int k = 1; k <= race.getCheckpointCount(); k++) {
            double d = k * sp;
            int y = (int) Math.round(PLAYER_Y + cam - d);
            if (y < -60 || y > H + 60) continue;
            int cx = (int) Math.round(race.roadCenter(d));
            boolean hit = k <= race.getCheckpointsHit();
            Color c = hit ? new Color(90, 200, 110, 120) : new Color(70, 255, 150);
            for (int i = 0; i < 2 * Race.ROAD_HALF; i += 20) {
                g.setColor(((i / 20) & 1) == 0 ? c : new Color(255, 255, 255, hit ? 100 : 230));
                g.fillRect(cx - Race.ROAD_HALF + i, y - 6, 20, 12);
            }
            fill(g, new Color(60, 66, 80), cx - Race.ROAD_HALF - 14, y - 40, 8, 46);
            fill(g, new Color(60, 66, 80), cx + Race.ROAD_HALF + 6, y - 40, 8, 46);
            fill(g, hit ? new Color(60, 120, 80) : new Color(30, 150, 90), cx - Race.ROAD_HALF - 14, y - 52, 2 * Race.ROAD_HALF + 28, 14);
            textCenter(g, "CHECKPOINT " + k, cx, y - 41, 11, Color.WHITE);
        }
    }

    private void drawParticles(Graphics2D g, double cam, int layer) {
        if (!Settings.effects) return;
        for (Particle p : particles) {
            boolean top = p.kind == 1 || p.kind == 3;
            if ((layer == 1) != top) continue;
            int y = (int) Math.round(PLAYER_Y + cam - p.dist);
            if (y < -20 || y > H + 20) continue;
            double t = p.life / p.maxLife;
            int s = (int) Math.max(2, p.size * (p.kind == 0 || p.kind == 2 ? (1.4 - t * 0.6) : 1.0));
            Color c;
            if (p.kind == 0) c = new Color(235, 235, 240, (int) (150 * t));
            else if (p.kind == 2) c = new Color(50, 50, 56, (int) (170 * t));
            else if (p.kind == 3) c = t > 0.5 ? new Color(255, 220, 90, 230) : new Color(255, 110, 30, (int) (230 * t * 2));
            else c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (255 * Math.min(1, t * 1.5)));
            g.setColor(c);
            g.fillRect((int) p.x - s / 2, y - s / 2, s, s);
        }
    }

    private void drawRain(Graphics2D g) {
        g.setColor(new Color(190, 210, 255, 120));
        for (double[] d : rain) g.fillRect((int) d[0], (int) d[1], 2, 12);
        g.setColor(new Color(20, 30, 70, 40));
        g.fillRect(0, 0, W, H);
    }

    private int hash(long seed, int a, int b, int c) {
        long h = seed * 31 + (a * 73856093L) ^ (b * 19349663L) ^ (c * 83492791L);
        h ^= (h >>> 29);
        h *= 0x9E3779B97F4A7C15L;
        h ^= (h >>> 32);
        return (int) (h & 0x7fffffff);
    }

    private int[] sceneryKinds(int theme, int side) {
        switch (theme) {
            case Race.THEME_CITY: return new int[] { 3, 3, 3, 12, 0 };
            case Race.THEME_SUBURB: return new int[] { 2, 2, 0, 0 };
            case Race.THEME_COUNTRY: return new int[] { 0, 10, 10, 1 };
            case Race.THEME_FOREST: return new int[] { 1, 1, 1, 1, 0 };
            case Race.THEME_INDUSTRIAL: return new int[] { 4, 5, 6, 4 };
            case Race.THEME_BRIDGE: return new int[] { 7, 7, 7 };
            case Race.THEME_SHORE: return side < 0 ? new int[] { 7, 7 } : new int[] { 8, 8, 11, 8 };
            default: return new int[] { 9, 9, 1, 9 };
        }
    }

    private int sceneryHalf(int kind) {
        switch (kind) {
            case 3: return 38;
            case 4: return 42;
            case 2: return 30;
            case 5: return 28;
            case 8: return 26;
            case 0: case 1: return 22;
            case 6: return 24;
            case 12: return 6;
            default: return 18;
        }
    }

    private void drawScenery(Graphics2D g, double cam) {
        int theme = race.getTheme();
        int slot = 90;
        double dMax = cam + PLAYER_Y + 80, dMin = cam + PLAYER_Y - H - 80;
        for (int s = (int) Math.floor(dMin / slot); s <= (int) Math.ceil(dMax / slot); s++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    int h = hash(race.getSeed(), s, side, j);
                    if (h % 100 >= 70) continue;
                    double d = s * slot + ((h >> 8) & 0xFF) / 256.0 * slot;
                    int[] kinds = sceneryKinds(theme, side);
                    int kind = kinds[(h >> 4) % kinds.length];
                    int half = sceneryHalf(kind);
                    int off = 14 + ((h >> 16) & 0x7F);
                    if (kind == 12) off = 2;
                    int y = (int) Math.round(PLAYER_Y + cam - d);
                    int cx = (int) Math.round(race.roadCenter(d));
                    int x = cx + side * (Race.ROAD_HALF + Race.BARRIER_WIDTH + off + half);
                    drawSprite(g, kind, x, y, h);
                }
            }
        }
    }

    private void drawSprite(Graphics2D g, int kind, int x, int y, int h) {
        int v = (h >> 12) & 7;
        switch (kind) {
            case 0: { // round tree
                int s = 26 + (v % 4) * 6;
                fill(g, new Color(0, 0, 0, 50), x - s / 2 + 5, y - s / 2 + 6, s, s);
                fill(g, new Color(26, 100, 38), x - s / 2, y - s / 2, s, s);
                fill(g, new Color(44, 146, 54), x - s / 2 + 3, y - s / 2 + 3, s - 8, s - 8);
                fill(g, new Color(92, 190, 92), x - s / 2 + 6, y - s / 2 + 6, s / 3, s / 3);
                break;
            }
            case 1: { // pine tree seen from above
                fill(g, new Color(0, 0, 0, 50), x - 18 + 5, y - 18 + 6, 36, 36);
                fill(g, new Color(16, 70, 36), x - 20, y - 20, 40, 40);
                fill(g, new Color(26, 100, 50), x - 14, y - 14, 28, 28);
                fill(g, new Color(46, 134, 66), x - 8, y - 8, 16, 16);
                fill(g, new Color(88, 170, 100), x - 3, y - 3, 6, 6);
                break;
            }
            case 2: { // house
                Color roof = v % 2 == 0 ? new Color(176, 62, 52) : new Color(96, 110, 170);
                fill(g, new Color(0, 0, 0, 50), x - 26, y - 22, 60, 50);
                fill(g, new Color(232, 226, 210), x - 30, y - 24, 60, 48);
                fill(g, roof, x - 28, y - 22, 56, 44);
                fill(g, roof.darker(), x - 28, y - 2, 56, 4);
                fill(g, roof.brighter(), x - 28, y - 22, 56, 4);
                fill(g, new Color(90, 70, 60), x + 12, y - 14, 8, 8);
                break;
            }
            case 3: { // city rooftop
                Color[] roofs = { new Color(140, 144, 160), new Color(170, 140, 120), new Color(110, 130, 160), new Color(150, 150, 150) };
                Color c = roofs[v % 4];
                int w = 56 + (v % 3) * 10, hh = 50 + (v % 2) * 20;
                fill(g, new Color(0, 0, 0, 55), x - w / 2 + 6, y - hh / 2 + 8, w, hh);
                fill(g, c.darker(), x - w / 2, y - hh / 2, w, hh);
                fill(g, c, x - w / 2 + 4, y - hh / 2 + 4, w - 8, hh - 8);
                fill(g, c.brighter(), x - w / 2 + 10, y - hh / 2 + 10, 14, 10);
                fill(g, c.darker(), x + 4, y + 2, 14, 12);
                break;
            }
            case 4: { // factory
                fill(g, new Color(0, 0, 0, 55), x - 40 + 6, y - 26 + 8, 84, 52);
                fill(g, new Color(110, 114, 126), x - 42, y - 26, 84, 52);
                fill(g, new Color(158, 162, 176), x - 38, y - 22, 76, 44);
                for (int i = 0; i < 5; i++) fill(g, new Color(130, 134, 148), x - 38 + i * 16, y - 22, 3, 44);
                fill(g, new Color(70, 70, 78), x + 20, y - 18, 14, 14);
                fill(g, new Color(180, 180, 190, 140), x + 18, y - 28, 18, 14);
                break;
            }
            case 5: { // storage tank
                fill(g, new Color(0, 0, 0, 55), x - 26 + 6, y - 26 + 8, 52, 52);
                fill(g, new Color(120, 124, 136), x - 28, y - 28, 56, 56);
                fill(g, new Color(200, 204, 216), x - 22, y - 22, 44, 44);
                fill(g, new Color(160, 164, 178), x - 12, y - 12, 24, 24);
                fill(g, new Color(230, 232, 240), x - 6, y - 6, 8, 8);
                break;
            }
            case 6: { // shipping containers
                Color[] cc = { new Color(200, 60, 50), new Color(60, 100, 200), new Color(230, 170, 40), new Color(70, 150, 90) };
                Color c = cc[v % 4];
                fill(g, new Color(0, 0, 0, 55), x - 16 + 5, y - 36 + 6, 32, 72);
                fill(g, c.darker(), x - 18, y - 36, 36, 72);
                fill(g, c, x - 15, y - 33, 30, 66);
                for (int i = 0; i < 6; i++) fill(g, c.darker(), x - 15, y - 30 + i * 11, 30, 2);
                break;
            }
            case 7: { // boat on the water
                fill(g, new Color(255, 255, 255, 90), x - 14, y + 14, 28, 3);
                fill(g, new Color(250, 250, 250), x - 10, y - 22, 20, 44);
                fill(g, new Color(250, 250, 250), x - 6, y - 28, 12, 8);
                fill(g, new Color(60, 100, 200), x - 10, y - 4, 20, 4);
                fill(g, new Color(170, 175, 190), x - 6, y - 14, 12, 10);
                fill(g, new Color(230, 80, 60), x - 4, y + 4, 8, 8);
                break;
            }
            case 8: { // palm tree
                fill(g, new Color(0, 0, 0, 45), x - 20 + 5, y - 20 + 6, 40, 40);
                g.setColor(new Color(36, 140, 60));
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3 + v;
                    int ex = x + (int) (Math.cos(a) * 24), ey = y + (int) (Math.sin(a) * 24);
                    g.setStroke(new java.awt.BasicStroke(7));
                    g.drawLine(x, y, ex, ey);
                }
                g.setStroke(new java.awt.BasicStroke(1));
                fill(g, new Color(110, 76, 40), x - 4, y - 4, 8, 8);
                break;
            }
            case 9: { // rock
                Polygon p = new Polygon(new int[] { x - 18, x - 6, x + 14, x + 20, x + 4, x - 14 },
                        new int[] { y - 4, y - 18, y - 12, y + 8, y + 18, y + 12 }, 6);
                g.setColor(new Color(0, 0, 0, 55));
                g.translate(5, 6);
                g.fillPolygon(p);
                g.translate(-5, -6);
                g.setColor(new Color(120, 114, 108));
                g.fillPolygon(p);
                g.setColor(new Color(166, 160, 152));
                g.fillRect(x - 8, y - 10, 12, 8);
                break;
            }
            case 10: { // hay bale
                fill(g, new Color(0, 0, 0, 45), x - 16 + 5, y - 16 + 6, 32, 32);
                fill(g, new Color(190, 150, 50), x - 18, y - 18, 36, 36);
                fill(g, new Color(232, 200, 90), x - 14, y - 14, 28, 28);
                fill(g, new Color(190, 150, 50), x - 14, y - 2, 28, 3);
                break;
            }
            case 11: { // beach umbrella
                fill(g, new Color(0, 0, 0, 45), x - 16 + 5, y - 16 + 6, 32, 32);
                fill(g, new Color(230, 60, 60), x - 18, y - 18, 36, 36);
                fill(g, new Color(250, 250, 250), x - 6, y - 18, 12, 36);
                fill(g, new Color(250, 250, 250), x - 18, y - 6, 36, 12);
                fill(g, new Color(230, 60, 60), x - 6, y - 6, 12, 12);
                break;
            }
            default: { // street lamp
                fill(g, new Color(40, 40, 48), x - 5, y - 5, 10, 10);
                fill(g, new Color(255, 236, 140), x - 3, y - 3, 6, 6);
                break;
            }
        }
    }


    private static final Color[] DOT_COLORS = {
        new Color(240, 90, 90), new Color(90, 160, 255), new Color(120, 230, 120),
        new Color(255, 150, 60), new Color(210, 120, 240), new Color(90, 230, 230)
    };

    private void drawOneCar(Graphics2D g, Race.Racer r, Race.Racer me, boolean showName) {
        int y = (int) Math.round(PLAYER_Y - (r.dist - me.dist));
        if (y < -70 || y > H + 70) return;
        int x = (int) Math.round(race.roadCenter(r.dist) + r.offset);
        double angle = Math.max(-0.3, Math.min(0.3, r.vx / 380.0)) * (r.drifting ? 1.8 : 1.0);
        if (!r.isPlayer && r.drifting) angle += Math.signum(race.roadSlope(r.dist)) * 0.28;
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);
        // flames from the exhaust while nitro or a drift boost is running
        if (r.nitroTime > 0 || r.boostTime > 0) {
            int fl = 18 + fx.nextInt(14);
            g.setColor(new Color(255, 110, 30));
            g.fillPolygon(new int[] { -8, 8, 0 }, new int[] { 30, 30, 30 + fl }, 3);
            g.setColor(new Color(255, 230, 120));
            g.fillPolygon(new int[] { -4, 4, 0 }, new int[] { 30, 30, 30 + fl / 2 }, 3);
        }
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(-13, -24, 33, 60);
        g.drawImage(r.car.getRaceImage(), -16, -30, 33, 60, null);
        if (r.isPolice) {
            boolean flip = ((int) (clock * 8) & 1) == 0;
            g.setColor(flip ? new Color(255, 40, 40) : new Color(60, 110, 255));
            g.fillRect(-9, -6, 8, 5);
            g.setColor(flip ? new Color(60, 110, 255) : new Color(255, 40, 40));
            g.fillRect(1, -6, 8, 5);
            g.setColor(flip ? new Color(255, 40, 40, 70) : new Color(60, 110, 255, 70));
            g.fillRect(-30, -26, 60, 52);
        }
        g.setTransform(old);
        if (showName && !r.isPlayer) {
            textCenter(g, r.name.split(" ")[0], x, y - 36, 11, Color.WHITE);
        }
    }

    private void drawCars(Graphics2D g, Race.Racer me) {
        for (Race.Racer t : race.getTraffic()) drawOneCar(g, t, me, false);
        for (Race.Racer p : race.getPolice()) drawOneCar(g, p, me, false);
        for (Race.Racer r : race.getRacers()) {
            if (r.eliminated || r.isPlayer) continue;
            drawOneCar(g, r, me, true);
        }
        // slipstream wind lines
        if (me.draft > 0.3 && !me.finished) {
            int x = (int) Math.round(race.roadCenter(me.dist) + me.offset);
            g.setColor(new Color(255, 255, 255, (int) (170 * me.draft)));
            for (int i = 0; i < 6; i++) {
                int yy = PLAYER_Y - 30 - (int) ((clock * 520 + i * 47) % 220);
                int xx = x + (i % 2 == 0 ? -26 : 24) + (i / 2) * (i % 2 == 0 ? -6 : 6);
                g.fillRect(xx, yy, 2, 18);
            }
        }
        drawOneCar(g, me, me, false);
    }

    private void drawNight(Graphics2D g, Race.Racer me) {
        g.setColor(new Color(8, 10, 50, 140));
        g.fillRect(0, 0, W, H);
        int x = (int) Math.round(race.roadCenter(me.dist) + me.offset);
        Polygon beam = new Polygon(new int[] { x - 10, x + 10, x + 60, x - 60 },
                new int[] { PLAYER_Y - 30, PLAYER_Y - 30, PLAYER_Y - 260, PLAYER_Y - 260 }, 4);
        g.setColor(new Color(255, 250, 190, 60));
        g.fillPolygon(beam);
    }

    private String formatTime(double t) {
        int m = (int) (t / 60);
        double s = t - m * 60;
        return String.format("%d:%04.1f", m, s);
    }

    // ------------------------------------------------------------------
    // HUD
    // ------------------------------------------------------------------
    private void drawHud(Graphics2D g, Race.Racer me) {
        int mode = race.getMode();
        Color panel = new Color(20, 24, 48, 215), edge = new Color(120, 160, 255);

        // ---- top left: position and time ----
        box(g, 12, 10, 210, 92, panel, edge);
        String pos;
        if (mode == Race.MODE_TRIAL) pos = "TRIAL";
        else if (mode == Race.MODE_POLICE) pos = "ESCAPE";
        else if (mode == Race.MODE_KNOCKOUT) pos = race.getActiveCount() + " LEFT";
        else pos = ordinal(race.getLivePlace(me)) + "/" + race.getRacerCount();
        text(g, pos, 24, 48, 32, new Color(255, 240, 170), true);
        text(g, "TIME " + formatTime(race.getTime()), 24, 70, 16, Color.WHITE, false);
        String sub = race.getName();
        if (mode == Race.MODE_TRIAL) sub = "GOLD " + formatTime(race.getGoldTime());
        if (sub.length() > 24) sub = sub.substring(0, 23) + ".";
        text(g, sub, 24, 91, 12, new Color(190, 200, 230), false);

        // ---- top right: speed and car condition ----
        box(g, 738, 10, 210, 92, panel, edge);
        textRight(g, String.valueOf(Settings.displaySpeed(me.speed)), 900, 54, 38, Color.WHITE);
        text(g, Settings.speedUnit(), 906, 54, 13, new Color(190, 200, 230), false);
        int cond = me.car.getCondition();
        text(g, "CAR " + cond + "%", 752, 78, 13, Color.WHITE, false);
        fill(g, new Color(50, 54, 70), 818, 68, 114, 12);
        fill(g, cond > 60 ? new Color(80, 210, 100) : cond > 30 ? new Color(240, 190, 50) : new Color(230, 60, 60), 820, 70, (int) (110 * cond / 100.0), 8);
        text(g, race.getThemeName().toUpperCase(), 752, 96, 11, new Color(150, 165, 200), false);

        // ---- nitro and drift boost bars ----
        box(g, 738, 108, 210, 62, panel, edge);
        int nitroSkill = window.getDriver().getSkill(Driver.SKILL_NITRO);
        double frac;
        String label;
        Color col;
        if (me.nitroTime > 0) {
            frac = me.nitroTime / (2.2 * (1 + 0.10 * nitroSkill));
            label = "NITRO!";
            col = ((int) (clock * 12) & 1) == 0 ? new Color(255, 160, 40) : new Color(255, 225, 100);
        } else if (me.nitroCool > 0) {
            frac = 1 - me.nitroCool / (9.0 * (1 - 0.06 * nitroSkill));
            label = "NITRO RECHARGING";
            col = new Color(80, 130, 220);
        } else {
            frac = 1;
            label = "NITRO READY [SPACE]";
            col = new Color(100, 230, 120);
        }
        text(g, label, 750, 126, 12, Color.WHITE, false);
        fill(g, new Color(50, 54, 70), 750, 130, 186, 10);
        fill(g, col, 751, 131, (int) (184 * Math.max(0, Math.min(1, frac))), 8);
        text(g, me.boostTime > 0 ? "BOOSTING!" : "DRIFT BOOST [SHIFT]", 750, 154, 12, Color.WHITE, false);
        fill(g, new Color(50, 54, 70), 750, 158, 186, 10);
        fill(g, me.boostTime > 0 ? new Color(255, 255, 255) : new Color(90, 220, 255), 751, 159, (int) (184 * (me.boostTime > 0 ? 1.0 : me.boostMeter / 100.0)), 8);

        // ---- road surface warning ----
        if (race.getCountdown() <= 0 && outcome == null) {
            int now = me.surf;
            int ahead = race.surfaceAt(me.dist + 450);
            if (now != Race.SURF_DRY) {
                box(g, 350, 10, 260, 34, new Color(60, 30, 30, 225), new Color(255, 150, 100));
                textCenter(g, Race.SURF_NAMES[now] + " - LOW GRIP", 480, 33, 16, new Color(255, 220, 150));
            } else if (ahead != Race.SURF_DRY) {
                box(g, 350, 10, 260, 34, new Color(60, 50, 20, 225), new Color(255, 210, 100));
                textCenter(g, "AHEAD: " + Race.SURF_NAMES[ahead], 480, 33, 16, new Color(255, 235, 150));
            }
        }

        // ---- mode specific panels ----
        if (mode == Race.MODE_KNOCKOUT && outcome == null && race.getCountdown() <= 0) {
            boolean danger = race.getLivePlace(me) == race.getActiveCount() && race.getNextKnockoutIn() < 6;
            box(g, 350, 50, 260, 50, danger && ((int) (clock * 6) & 1) == 0 ? new Color(120, 20, 20, 235) : panel, danger ? new Color(255, 90, 80) : edge);
            textCenter(g, danger ? "DANGER! YOU ARE LAST!" : "NEXT KNOCKOUT", 480, 70, 14, danger ? new Color(255, 200, 190) : Color.WHITE);
            textCenter(g, String.format("%.1f", Math.max(0, race.getNextKnockoutIn())) + "s", 480, 92, 20, new Color(255, 240, 170));
        }
        if (mode == Race.MODE_POLICE && outcome == null) {
            double b = race.getBust();
            boolean flip = ((int) (clock * 6) & 1) == 0;
            box(g, 350, 50, 260, 54, b > 60 && flip ? new Color(120, 20, 20, 235) : panel, b > 0 ? (flip ? new Color(255, 80, 80) : new Color(90, 140, 255)) : edge);
            textCenter(g, "BUSTED METER   (police: " + race.getPolice().size() + ")", 480, 68, 12, Color.WHITE);
            fill(g, new Color(50, 54, 70), 366, 76, 228, 16);
            fill(g, b > 66 ? new Color(255, 70, 60) : b > 33 ? new Color(255, 190, 60) : new Color(90, 190, 255), 368, 78, (int) (224 * b / 100.0), 12);
        }
        if (mode == Race.MODE_CHECKPOINT && outcome == null) {
            boolean low = race.getTimeLeft() < 8;
            box(g, 350, 50, 260, 52, low && ((int) (clock * 6) & 1) == 0 ? new Color(120, 20, 20, 235) : panel, low ? new Color(255, 90, 80) : edge);
            textCenter(g, "TIME LEFT", 480, 68, 12, Color.WHITE);
            textCenter(g, String.format("%.1f", race.getTimeLeft()), 480, 94, 26, low ? new Color(255, 120, 110) : new Color(120, 255, 150));
        }

        if (Settings.showMinimap) drawMinimap(g, me);
        if (Settings.showSpeedo) drawGauge(g, me, 868, 528, 66);

        // ---- bottom bar ----
        box(g, 170, 600, 620, 30, panel, edge);
        if (mode == Race.MODE_KNOCKOUT) {
            double f = 1 - Math.max(0, Math.min(1, race.getNextKnockoutIn() / 15.0));
            fill(g, new Color(230, 90, 70), 178, 608, (int) (604 * f), 14);
            textCenter(g, "KNOCKOUT TIMER", 480, 620, 11, Color.WHITE);
        } else {
            double prog = Math.max(0, Math.min(1, me.dist / race.getLength()));
            fill(g, new Color(80, 190, 110), 178, 608, (int) (604 * prog), 14);
            for (Race.Racer r : race.getRacers()) {
                if (r.isPlayer || r.eliminated) continue;
                double p = Math.max(0, Math.min(1, r.dist / race.getLength()));
                fill(g, DOT_COLORS[Math.abs(r.name.hashCode()) % DOT_COLORS.length], 176 + (int) (604 * p), 606, 6, 18);
            }
            fill(g, new Color(255, 240, 170), 176 + (int) (604 * prog), 604, 8, 22);
            text(g, "START", 182, 621, 10, Color.WHITE, false);
            textRight(g, "FINISH", 776, 621, 10, Color.WHITE);
        }

        // ---- banner messages ----
        if (bannerTime > 0 && outcome == null) {
            int a = (int) Math.min(255, bannerTime * 400);
            Color c = new Color(bannerColor.getRed(), bannerColor.getGreen(), bannerColor.getBlue(), Math.max(0, a));
            textCenter(g, banner, 480, 190, 34, c);
        }
        if (me.draft > 0.7 && outcome == null && !me.finished) {
            textCenter(g, "SLIPSTREAM!", 480, PLAYER_Y - 120, 16, new Color(255, 255, 255, 220));
        }

        if (race.getCountdown() > 0) drawCountdown(g);
        else if (race.getTime() < 2.5 && outcome == null) { /* controls hint fades after the start */ }
    }

    private void drawCountdown(Graphics2D g) {
        double cd = race.getCountdown();
        String s = cd > 2.5 ? "3" : cd > 1.5 ? "2" : cd > 0.5 ? "1" : "GO!";
        Color c = cd > 2.5 ? new Color(255, 120, 100) : cd > 1.5 ? new Color(255, 200, 80) : cd > 0.5 ? new Color(255, 240, 120) : new Color(120, 255, 140);
        textCenter(g, s, 480, 200, 110, c);

        box(g, 190, 280, 580, 168, new Color(20, 24, 48, 225), new Color(255, 230, 120));
        textCenter(g, race.getName().toUpperCase(), 480, 308, 20, new Color(255, 240, 170));
        String modeHint;
        switch (race.getMode()) {
            case Race.MODE_KNOCKOUT: modeHint = "KNOCKOUT: the last car is out every few seconds. Don't be last!"; break;
            case Race.MODE_CHECKPOINT: modeHint = "CHECKPOINT RUSH: reach every gate before the time runs out!"; break;
            case Race.MODE_STREET: modeHint = "STREET RACE: watch out for traffic!" + (wager > 0 ? "  Bet: " + money(wager) : ""); break;
            case Race.MODE_POLICE: modeHint = "POLICE CHASE: outrun the cops to the finish! Don't get BUSTED!"; break;
            case Race.MODE_TRIAL: modeHint = "TIME TRIAL: beat the medal times: " + formatTime(race.getGoldTime()) + " for gold!"; break;
            case Race.MODE_CHAMPIONSHIP: modeHint = "CHAMPIONSHIP: points for every place. Finish high!"; break;
            default: modeHint = "Finish FIRST to win!"; break;
        }
        textCenter(g, modeHint, 480, 334, 14, Color.WHITE);
        String go = wasd() ? (arrows() ? "UP/W gas   DOWN/S brake   LEFT/RIGHT or A/D steer" : "W gas   S brake   A/D steer")
                : "UP gas   DOWN brake   LEFT/RIGHT steer";
        textCenter(g, go, 480, 366, 15, new Color(190, 220, 255));
        textCenter(g, "SHIFT drift (fills the boost bar)    SPACE nitro", 480, 392, 15, new Color(190, 220, 255));
        textCenter(g, "Slipstream behind cars for speed.  ESC = pause menu", 480, 418, 14, new Color(150, 165, 200));
    }

    private void drawMinimap(Graphics2D g, Race.Racer me) {
        int bx = 12, by = 112, bw = 118, bh = 236;
        box(g, bx, by, bw, bh, new Color(20, 24, 48, 200), new Color(120, 160, 255));
        textCenter(g, "MAP", bx + bw / 2, by + 17, 12, new Color(190, 200, 230));
        double len = race.getLength();
        double win = Math.min(len, 8000);
        double d0 = Math.max(0, Math.min(len - win, me.dist - win * 0.25));
        int top = by + 28, bottom = by + bh - 12;
        double sy = (bottom - top) / win;
        double sx = 0.16;
        int cx = bx + bw / 2;

        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int px = 0, py = 0;
        for (int i = 0; i <= 90; i++) {
            double d = d0 + win * i / 90.0;
            int x = (int) (cx + (race.roadCenter(d) - 480) * sx);
            int y = (int) (bottom - (d - d0) * sy);
            if (i > 0) {
                int s = race.surfaceAt(d);
                g.setColor(s == Race.SURF_WET ? new Color(70, 110, 190) : s == Race.SURF_GRAVEL ? new Color(150, 120, 80)
                        : s == Race.SURF_ICE ? new Color(190, 230, 250) : new Color(100, 104, 122));
                g.drawLine(px, py, x, y);
            }
            px = x;
            py = y;
        }
        g.setStroke(old);

        // finish line and checkpoints
        if (race.getMode() != Race.MODE_KNOCKOUT && len <= d0 + win + 1) {
            int y = (int) (bottom - (len - d0) * sy);
            int x = (int) (cx + (race.roadCenter(len) - 480) * sx);
            for (int i = 0; i < 4; i++) fill(g, i % 2 == 0 ? Color.WHITE : Color.BLACK, x - 8 + i * 4, y - 2, 4, 4);
        }
        double sp = race.getCheckpointSpacing();
        if (race.getMode() == Race.MODE_CHECKPOINT && sp > 0) {
            for (int k = 1; k <= race.getCheckpointCount(); k++) {
                double d = k * sp;
                if (d < d0 || d > d0 + win) continue;
                int y = (int) (bottom - (d - d0) * sy);
                int x = (int) (cx + (race.roadCenter(d) - 480) * sx);
                fill(g, k <= race.getCheckpointsHit() ? new Color(90, 150, 100) : new Color(80, 255, 150), x - 6, y - 1, 12, 3);
            }
        }
        // the cars
        for (Race.Racer r : race.getRacers()) {
            if (r.eliminated || r.dist < d0 || r.dist > d0 + win) continue;
            int x = (int) (cx + (race.roadCenter(r.dist) - 480 + r.offset) * sx);
            int y = (int) (bottom - (r.dist - d0) * sy);
            if (r.isPlayer) continue;
            fill(g, DOT_COLORS[Math.abs(r.name.hashCode()) % DOT_COLORS.length], x - 3, y - 3, 6, 6);
        }
        for (Race.Racer p : race.getPolice()) {
            if (p.dist < d0 || p.dist > d0 + win) continue;
            int x = (int) (cx + (race.roadCenter(p.dist) - 480 + p.offset) * sx);
            int y = (int) (bottom - (p.dist - d0) * sy);
            fill(g, ((int) (clock * 6) & 1) == 0 ? new Color(255, 60, 60) : new Color(70, 120, 255), x - 3, y - 3, 6, 6);
        }
        int mx = (int) (cx + (race.roadCenter(me.dist) - 480 + me.offset) * sx);
        int my = (int) (bottom - (me.dist - d0) * sy);
        fill(g, Color.BLACK, mx - 5, my - 5, 10, 10);
        fill(g, new Color(255, 235, 90), mx - 4, my - 4, 8, 8);
    }

    private void drawGauge(Graphics2D g, Race.Racer me, int cx, int cy, int r) {
        g.setColor(new Color(20, 24, 48, 225));
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(4));
        g.setColor(new Color(120, 160, 255));
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);

        double maxPx = me.topSpeed * 1.5;
        double maxShown = Math.ceil(Settings.displaySpeed(maxPx) / 50.0) * 50.0;
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i <= 10; i++) {
            double f = i / 10.0;
            double a = Math.toRadians(225 - 270 * f);
            int x1 = (int) (cx + Math.cos(a) * (r - 6)), y1 = (int) (cy - Math.sin(a) * (r - 6));
            int x2 = (int) (cx + Math.cos(a) * (r - (i % 2 == 0 ? 15 : 11))), y2 = (int) (cy - Math.sin(a) * (r - (i % 2 == 0 ? 15 : 11)));
            g.setColor(f > 0.8 ? new Color(255, 100, 90) : Color.WHITE);
            g.drawLine(x1, y1, x2, y2);
            if (i % 2 == 0) {
                int lx = (int) (cx + Math.cos(a) * (r - 26)), ly = (int) (cy - Math.sin(a) * (r - 26));
                textCenter(g, String.valueOf((int) (maxShown * f)), lx, ly + 4, 9, new Color(190, 200, 230));
            }
        }
        // needle
        double shown = Settings.displaySpeed(me.speed);
        double f = Math.max(0, Math.min(1, shown / maxShown));
        double a = Math.toRadians(225 - 270 * f);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(255, 90, 70));
        g.drawLine(cx, cy, (int) (cx + Math.cos(a) * (r - 14)), (int) (cy - Math.sin(a) * (r - 14)));
        g.setStroke(old);
        fill(g, new Color(230, 230, 240), cx - 4, cy - 4, 8, 8);

        // gear and rev bar
        double gearSpan = Math.max(1, me.topSpeed * 1.15 / 6.0);
        int gear = (int) Math.min(5, me.speed / gearSpan);
        double inGear = Math.max(0, Math.min(1.0, (me.speed - gear * gearSpan) / gearSpan));
        double rpm = 0.2 + 0.8 * inGear;
        textCenter(g, "GEAR " + (gear + 1), cx, cy + 22, 12, Color.WHITE);
        textCenter(g, Settings.speedUnit(), cx, cy - 18, 10, new Color(150, 165, 200));
        for (int i = 0; i < 10; i++) {
            Color c = i < 6 ? new Color(90, 220, 110) : i < 8 ? new Color(240, 200, 60) : new Color(240, 70, 60);
            fill(g, i < rpm * 10 ? c : new Color(50, 54, 70), cx - 40 + i * 8, cy + 30, 6, 8);
        }
    }

    // ------------------------------------------------------------------
    // Results
    // ------------------------------------------------------------------
    private void drawResults(Graphics2D g) {
        Career.RaceOutcome o = outcome;
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, W, H);
        box(g, 170, 24, 620, 486, new Color(24, 28, 56), new Color(255, 230, 120));
        int mode = o.mode;
        textCenter(g, race.getName().toUpperCase().length() > 34 ? Race.MODE_NAMES[mode].toUpperCase() + " RESULTS"
                : race.getName().toUpperCase(), 480, 62, 24, new Color(255, 240, 170));
        textCenter(g, Race.MODE_NAMES[mode].toUpperCase(), 480, 84, 14, new Color(150, 165, 200));

        if (mode == Race.MODE_TRIAL) {
            drawTrialResults(g, o);
            return;
        }

        if (mode == Race.MODE_POLICE) {
            boolean caught = o.busted;
            textCenter(g, caught ? "BUSTED!" : "YOU ESCAPED!", 480, 160, 56, caught ? new Color(255, 100, 90) : new Color(120, 255, 140));
            textCenter(g, caught ? "The police caught you." : "You left the cops in the dust!", 480, 204, 18, Color.WHITE);
            int yy = 270;
            text(g, "Prize money", 214, yy, 17, Color.WHITE, false);
            textRight(g, money(o.prize), 746, yy, 17, o.prize > 0 ? new Color(120, 240, 130) : new Color(150, 150, 160));
            yy += 28;
            if (caught) {
                text(g, "Fine", 214, yy, 17, Color.WHITE, false);
                textRight(g, "-" + money(o.fine), 746, yy, 17, new Color(255, 120, 110));
                yy += 28;
            }
            text(g, "Fame", 214, yy, 17, Color.WHITE, false);
            textRight(g, "+" + o.repGain, 746, yy, 17, new Color(190, 170, 255));
            yy += 28;
            if (!o.challenges.isEmpty()) text(g, "Sponsor deals done: " + o.challenges.size() + "!", 214, yy, 16, new Color(255, 200, 90), false);
            return;
        }

        // ---- place ----
        String head;
        Color hc;
        if (o.timedOut && mode == Race.MODE_CHECKPOINT && !race.getPlayer().finished) { head = "TIME'S UP!"; hc = new Color(255, 100, 90); }
        else if (o.place == 1) { head = "YOU WIN!"; hc = new Color(120, 255, 140); }
        else { head = ordinal(o.place) + " PLACE"; hc = o.place <= 3 ? new Color(255, 220, 100) : new Color(255, 130, 110); }
        textCenter(g, head, 480, 142, 50, hc);

        // ---- standings ----
        List<Race.Racer> st = race.getStandings();
        int y = 186;
        for (int i = 0; i < st.size() && i < 6; i++) {
            Race.Racer r = st.get(i);
            Color c = r.isPlayer ? new Color(255, 240, 150) : Color.WHITE;
            if (r.isPlayer) fill(g, new Color(255, 255, 255, 30), 200, y - 17, 560, 24);
            text(g, (i + 1) + ".", 214, y, 17, c, false);
            text(g, r.name, 254, y, 17, c, false);
            String right;
            if (mode == Race.MODE_KNOCKOUT) right = i == 0 ? "WINNER" : r.eliminated ? "OUT" : "";
            else right = r.finished ? formatTime(r.finishTime) : "DNF";
            textRight(g, right, 746, y, 17, c);
            y += 27;
        }
        y = Math.max(y, 356) + 6;

        // ---- rewards ----
        text(g, "Prize money", 214, y, 17, Color.WHITE, false);
        textRight(g, money(o.prize), 746, y, 17, o.prize > 0 ? new Color(120, 240, 130) : new Color(150, 150, 160));
        y += 25;
        if (mode == Race.MODE_STREET) {
            text(g, "Bet " + (o.wager > 0 ? money(o.wager) : "(none)"), 214, y, 17, Color.WHITE, false);
            String s = o.wager == 0 ? "-" : o.wagerNet > 0 ? "WON +" + money(o.wagerNet) : o.wagerNet < 0 ? "LOST " + money(-o.wagerNet) : "GOT IT BACK";
            textRight(g, s, 746, y, 17, o.wagerNet > 0 ? new Color(120, 240, 130) : o.wagerNet < 0 ? new Color(255, 120, 110) : Color.WHITE);
            y += 25;
        }
        if (mode == Race.MODE_CHAMPIONSHIP) {
            text(g, "Championship points", 214, y, 17, Color.WHITE, false);
            textRight(g, "+" + o.cupPoints, 746, y, 17, new Color(150, 200, 255));
            y += 25;
            if (o.cupFinished) {
                text(g, "CUP FINISHED: " + ordinal(o.cupPlace), 214, y, 17, new Color(255, 220, 100), false);
                textRight(g, o.cupPrize > 0 ? money(o.cupPrize) : "no prize", 746, y, 17, new Color(120, 240, 130));
                y += 25;
            }
        }
        text(g, "Fame", 214, y, 17, Color.WHITE, false);
        textRight(g, "+" + o.repGain, 746, y, 17, new Color(190, 170, 255));
        y += 25;
        if (!o.challenges.isEmpty()) {
            text(g, "Sponsor deals done: " + o.challenges.size() + "!", 214, y, 16, new Color(255, 200, 90), false);
        } else if (mode == Race.MODE_NORMAL && o.win && race.getIndexInRank() < Career.RACES_PER_RANK - 1) {
            text(g, "Next race unlocked!", 214, y, 16, new Color(120, 240, 130), false);
        } else if (mode == Race.MODE_NORMAL && !o.win) {
            text(g, "Win this race to unlock the next one.", 214, y, 16, new Color(255, 190, 120), false);
        }
    }

    private void drawTrialResults(Graphics2D g, Career.RaceOutcome o) {
        String[] names = { "NO MEDAL", "BRONZE MEDAL", "SILVER MEDAL", "GOLD MEDAL!" };
        Color[] cols = { new Color(170, 170, 180), new Color(205, 130, 70), new Color(215, 220, 230), new Color(255, 215, 70) };
        if (o.timedOut && o.trialTime <= 0) {
            textCenter(g, "NOT FINISHED", 480, 170, 46, new Color(255, 130, 110));
            textCenter(g, "Finish the race to earn a medal.", 480, 220, 18, Color.WHITE);
            return;
        }
        textCenter(g, names[o.medal], 480, 150, 46, cols[o.medal]);
        textCenter(g, "YOUR TIME  " + formatTime(o.trialTime), 480, 200, 26, Color.WHITE);
        Race base = window.getCareer().getRace(race.getNumber());
        int y = 250;
        String[] lab = { "GOLD", "SILVER", "BRONZE" };
        double[] tt = { base.getGoldTime(), base.getSilverTime(), base.getBronzeTime() };
        Color[] mc = { cols[3], cols[2], cols[1] };
        for (int i = 0; i < 3; i++) {
            fill(g, mc[i], 260, y - 14, 16, 16);
            text(g, lab[i], 288, y, 18, Color.WHITE, false);
            textRight(g, formatTime(tt[i]), 700, y, 18, o.trialTime <= tt[i] ? new Color(120, 240, 130) : Color.WHITE);
            y += 30;
        }
        text(g, "BEST TIME", 260, y + 8, 18, new Color(150, 200, 255), false);
        textRight(g, formatTime(base.getBestTime()), 700, y + 8, 18, new Color(150, 200, 255));
        y += 50;
        if (o.skillPointsGained > 0) {
            textCenter(g, "+" + o.skillPointsGained + " SKILL POINT" + (o.skillPointsGained == 1 ? "" : "S") + "!", 480, y, 24, new Color(120, 240, 130));
            textCenter(g, "Spend them in the Practice Garage.", 480, y + 28, 15, Color.WHITE);
        } else {
            textCenter(g, "Beat a better medal for more skill points.", 480, y, 15, new Color(190, 200, 230));
        }
    }
}
