package racing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * ONE class is used for every kind of race - a Race is just an object with
 * different data (name, theme, length, curves, opponents, prizes, MODE).
 *
 * Race also contains the racing "engine": update() moves every car a small
 * step each frame. The screen (GameWindow) only draws what Race tells it.
 *
 * The road scrolls downwards. "dist" is how far a car has driven along the
 * track (in pixels) and "offset" is how far it is from the middle of the road.
 *
 * New in Road to Legendary:
 *  - curvier tracks, with wet / gravel / icy sections
 *  - DRIFT (hold SHIFT while steering) fills a boost meter, released as a burst
 *  - NITRO (SPACE) with a cooldown
 *  - SLIPSTREAM: tuck in behind another car for a speed boost
 *  - rubber-band AI that keeps the opponents close enough to see
 *  - race modes: normal, championship, knockout, checkpoint rush, street, time trial
 */
public class Race {

    // ---- track themes (each one gets its own pixel-art scenery) ----
    public static final int THEME_CITY = 0;
    public static final int THEME_SUBURB = 1;
    public static final int THEME_COUNTRY = 2;
    public static final int THEME_FOREST = 3;
    public static final int THEME_INDUSTRIAL = 4;
    public static final int THEME_BRIDGE = 5;
    public static final int THEME_SHORE = 6;
    public static final int THEME_MOUNTAIN = 7;
    public static final String[] THEME_NAMES = {
        "City Streets", "Suburban Roads", "Country Roads", "Forest Trail",
        "Industrial Zone", "Bridge Crossing", "Lakeshore Drive", "Mountain Pass"
    };

    // ---- race modes ----
    public static final int MODE_NORMAL = 0, MODE_CHAMPIONSHIP = 1, MODE_KNOCKOUT = 2,
            MODE_CHECKPOINT = 3, MODE_STREET = 4, MODE_TRIAL = 5, MODE_POLICE = 6;
    public static final String[] MODE_NAMES = {
        "Race", "Championship", "Knockout", "Checkpoint Rush", "Street Race", "Time Trial", "Police Chase"
    };

    // ---- road surfaces ----
    public static final int SURF_DRY = 0, SURF_WET = 1, SURF_GRAVEL = 2, SURF_ICE = 3;
    public static final String[] SURF_NAMES = { "DRY ROAD", "WET ROAD", "GRAVEL", "ICY ROAD" };
    private static final double[] GRIP_RATE = { 14.0, 8.5, 5.5, 3.0 };      // how quickly the car obeys the wheel
    private static final double[] STEER_MULT = { 1.0, 0.92, 0.82, 0.70 };
    private static final double[] PUSH_MULT = { 1.0, 1.12, 1.25, 1.45 };    // how much curves push the car
    private static final double[] SPEED_MULT = { 1.0, 0.98, 0.94, 0.96 };
    private static final double[] BRAKE_MULT = { 1.0, 0.8, 0.7, 0.4 };

    // ---- road geometry ----
    public static final int ROAD_HALF = 150;          // road is 300 pixels wide
    public static final int BARRIER_WIDTH = 12;
    public static final int CAR_HALF_WIDTH = 17;
    public static final int CAR_LENGTH = 56;
    public static final int ROAD_CENTER_X = 480;
    private static final double DRIFT = 0.75;         // how strongly curves push the car

    // ---- events (the screen turns these into sounds and sparks) ----
    public static final int EV_COUNT = 0, EV_GO = 1, EV_WALL = 2, EV_BUMP = 3, EV_NITRO = 4, EV_BOOST = 5,
            EV_CHECKPOINT = 6, EV_ELIMINATED = 7, EV_WARNING = 8, EV_FINISH = 9, EV_TIMEUP = 10, EV_TRAFFIC = 11,
            EV_BUSTED = 12, EV_SIREN = 13;

    public static class Event {
        public final int type;
        public final Racer who;
        public final double value;
        Event(int type, Racer who, double value) { this.type = type; this.who = who; this.value = value; }
    }

    // ---- race information ----
    private int number;                  // 1..25 (also the difficulty)
    private String name;
    private final int rank;              // 0..4
    private final int indexInRank;       // 0..4
    private int theme;
    private boolean night;
    private final int[] prizes;          // 1st, 2nd, 3rd
    private double length;
    private long seed;
    private int mode = MODE_NORMAL;
    private int cupIndex = -1;
    private double[] amp = new double[3], freq = new double[3], ph = new double[3];
    private double envFreq, envPhase;
    private double[] surfStart = new double[0];
    private int[] surfType = new int[0];
    private ArrayList<Opponent> opponents = new ArrayList<Opponent>();

    // ---- career data for this race ----
    private int bestPlace = 0;           // 0 = not completed yet
    private double bestTime = 0;         // best time trial (0 = none)
    private int bestMedal = 0;           // 0 none, 1 bronze, 2 silver, 3 gold
    private int retroIndex = -1;         // which retro car this race can reward (-1 = none)
    private boolean rewardClaimed = false;

    // ---- mode settings ----
    private double cpSpacing = 0;        // checkpoint rush: distance between gates
    private int cpCount = 0;
    private double cpBonus = 0;
    private double cpStartTime = 0;
    private double koInterval = 15;
    private int trafficCount = 0;
    private int policeStart = 2, policeMax = 3;
    private int[] skills = { 0, 0, 0 };  // driver skills: drift, nitro, draft

    // ---- live race state ----
    private List<Racer> racers = new ArrayList<Racer>();
    private List<Racer> traffic = new ArrayList<Racer>();
    private List<Racer> knockedOut = new ArrayList<Racer>();
    private List<Racer> police = new ArrayList<Racer>();
    private double bust;
    private boolean busted;
    private double policeSpawnTimer, sirenTimer;
    private ArrayList<Event> events = new ArrayList<Event>();
    private Racer player;
    private double countdown;
    private int lastStage;
    private double time;
    private double afterFinish;
    private int finishOrder;
    private int damageTaken;
    private boolean over;
    private boolean timedOut;
    private double timeLeft;
    private int cpHit;
    private double nextKnockout;
    private double warnTimer;
    private Random rng = new Random();

    // ---- statistics for sponsor challenges ----
    private double topSpeedReached;
    private int nitroUses, driftBoosts, wallHits;
    private double draftSeconds;
    private final double[] surfaceMeters = new double[4];

    /** One car on the track during a race. */
    public static class Racer {
        public final String name;
        public final Car car;
        public final boolean isPlayer;
        public double dist, offset, speed, vx;
        public double topSpeed, accel, steerPx;
        public double targetOffset, laneTimer, blockTimer, phase, cooldown;
        public double skill = 0.5;
        public boolean finished, eliminated, drifting, isTraffic, isPolice;
        public double finishTime;
        public int place;
        public double nitroTime, nitroCool, boostMeter, boostTime, draft;
        public int surf;

        Racer(String name, Car car, boolean isPlayer) {
            this.name = name;
            this.car = car;
            this.isPlayer = isPlayer;
            this.topSpeed = car.getTopSpeedPx();
            this.accel = car.getAccelerationPx();
            this.steerPx = car.getSteeringPx();
        }
    }

    public Race(int number, String name, int rank, int indexInRank, int theme, boolean night, int[] prizes) {
        this.number = number;
        this.name = name;
        this.rank = rank;
        this.indexInRank = indexInRank;
        this.theme = theme;
        this.night = night;
        this.prizes = prizes;
        this.length = 9500 + rank * 1700 + indexInRank * 600;
        this.seed = number * 7919L;
        buildTrack();
    }

    /** A fresh copy of this race (same track) that can be played in another mode. */
    public Race makeVariant(int newMode) { return makeVariant(newMode, prizes); }

    public Race makeVariant(int newMode, int[] newPrizes) {
        Race r = new Race(number, name, rank, indexInRank, theme, night, newPrizes);
        r.seed = seed;
        r.length = length;
        r.mode = newMode;
        r.buildTrack();
        return r;
    }

    // ------------------------------------------------------------------
    // Setting up
    // ------------------------------------------------------------------
    public void setMode(int m) { mode = m; }
    public void setName(String n) { name = n; }
    public void setNight(boolean n) { night = n; }
    public void setTheme(int t) { theme = t; buildTrack(); }
    public void setSeed(long s) { seed = s; buildTrack(); }
    public void setLength(double l) { length = l; buildTrack(); }
    public void setNumber(int n) { number = n; buildTrack(); }
    public void setTrafficCount(int n) { trafficCount = n; }
    public void setPolice(int start, int max) { policeStart = start; policeMax = max; }
    public List<Racer> getPolice() { return police; }
    public double getBust() { return bust; }
    public boolean isBusted() { return busted; }
    public void setKnockoutInterval(double s) { koInterval = s; }
    public void setPlayerSkills(int drift, int nitro, int draft) { skills = new int[] { drift, nitro, draft }; }

    /** Checkpoint rush: gates every "spacing" pixels, "count" gates before the finish. */
    public void setCheckpoints(double spacing, int count) {
        cpSpacing = spacing;
        cpCount = count;
        length = spacing * (count + 1);
        buildTrack();
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    /** Works out the curves and the wet / gravel / ice sections from the seed. */
    private void buildTrack() {
        Random r = new Random(seed);
        double t = Math.min(1.0, Math.max(0.0, (number - 1) / 24.0));
        double a1 = lerp(115, 168, t) * (0.9 + 0.2 * r.nextDouble());
        double f1 = lerp(0.0024, 0.0034, t) * (0.9 + 0.2 * r.nextDouble());
        amp[0] = a1;
        amp[1] = lerp(50, 82, t) * (0.9 + 0.2 * r.nextDouble());
        amp[2] = lerp(15, 38, t) * (0.9 + 0.2 * r.nextDouble());
        freq[0] = f1;
        freq[1] = f1 * 2.3;
        freq[2] = f1 * 4.6;
        for (int i = 0; i < 3; i++) ph[i] = r.nextDouble() * Math.PI * 2;
        envFreq = f1 * 0.37;
        envPhase = r.nextDouble() * Math.PI * 2;

        // road surfaces: later races have more slippery sections
        Random sr = new Random(seed * 13 + 5);
        ArrayList<Double> starts = new ArrayList<Double>();
        ArrayList<Integer> types = new ArrayList<Integer>();
        double chance = 0.18 + 0.016 * (number - 1);
        double d = 1800;
        int[] allowed = surfacesForTheme(theme);
        while (d < length - 1200 && allowed.length > 0) {
            double len = 900 + sr.nextDouble() * 800;
            if (sr.nextDouble() < chance) {
                starts.add(d);
                types.add(allowed[sr.nextInt(allowed.length)]);
                starts.add(d + len);
                types.add(SURF_DRY);
            }
            d += len + 500 + sr.nextDouble() * 900;
        }
        surfStart = new double[starts.size()];
        surfType = new int[starts.size()];
        for (int i = 0; i < starts.size(); i++) {
            surfStart[i] = starts.get(i);
            surfType[i] = types.get(i);
        }
    }

    private static int[] surfacesForTheme(int theme) {
        switch (theme) {
            case THEME_CITY: return new int[] { SURF_WET, SURF_WET };
            case THEME_SUBURB: return new int[] { SURF_WET, SURF_GRAVEL };
            case THEME_COUNTRY: return new int[] { SURF_GRAVEL, SURF_WET };
            case THEME_FOREST: return new int[] { SURF_GRAVEL, SURF_WET };
            case THEME_INDUSTRIAL: return new int[] { SURF_GRAVEL, SURF_WET };
            case THEME_BRIDGE: return new int[] { SURF_WET, SURF_ICE };
            case THEME_SHORE: return new int[] { SURF_WET, SURF_GRAVEL };
            default: return new int[] { SURF_ICE, SURF_GRAVEL, SURF_WET };
        }
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------
    public int getNumber() { return number; }
    public String getName() { return name; }
    public int getRank() { return rank; }
    public int getIndexInRank() { return indexInRank; }
    public int getTheme() { return theme; }
    public String getThemeName() { return THEME_NAMES[theme]; }
    public boolean isNight() { return night; }
    public double getLength() { return length; }
    public long getSeed() { return seed; }
    public int getMode() { return mode; }
    public int getCup() { return cupIndex; }
    public void setCup(int c) { cupIndex = c; }
    public ArrayList<Opponent> getOpponents() { return opponents; }
    public void setOpponents(ArrayList<Opponent> list) { opponents = list; }
    public int getBestPlace() { return bestPlace; }
    public boolean isCompleted() { return bestPlace > 0; }
    public boolean isWon() { return bestPlace == 1; }
    public double getCheckpointSpacing() { return cpSpacing; }
    public int getCheckpointCount() { return cpCount; }
    public int getCheckpointsHit() { return cpHit; }
    public double getTimeLeft() { return timeLeft; }
    public boolean isTimedOut() { return timedOut; }
    public double getNextKnockoutIn() { return nextKnockout; }
    public List<Racer> getTraffic() { return traffic; }

    public int getPrize(int place) {
        if (place >= 1 && place <= 3) return prizes[place - 1];
        return 0;
    }

    /** Difficulty 1-25 (the race number: every race is harder than the last). */
    public int getDifficulty() { return number; }

    /** Puts saved career results back (used when loading a saved game). */
    public void restoreProgress(int place, double time, int medal, boolean claimed) {
        bestPlace = place;
        bestTime = time;
        bestMedal = medal;
        rewardClaimed = claimed;
    }

    public void recordResult(int place) {
        if (bestPlace == 0 || place < bestPlace) bestPlace = place;
    }

    // ---- time trial medals ----
    /** The speed (px/s) a "par" driver averages on this race; medal times are built from it. */
    private static final double[] REF_TOP = { 245, 285, 320, 350, 375 };
    private double parSpeed() { return REF_TOP[rank] * (1 + 0.012 * indexInRank); }
    public double getGoldTime() { return length / (parSpeed() * 1.04); }
    public double getSilverTime() { return length / (parSpeed() * 0.96); }
    public double getBronzeTime() { return length / (parSpeed() * 0.88); }
    public double getBestTime() { return bestTime; }
    public int getBestMedal() { return bestMedal; }

    /** 3 gold, 2 silver, 1 bronze, 0 none. */
    public int medalFor(double t) {
        if (t <= getGoldTime()) return 3;
        if (t <= getSilverTime()) return 2;
        if (t <= getBronzeTime()) return 1;
        return 0;
    }

    public void recordTime(double t) {
        if (bestTime <= 0 || t < bestTime) bestTime = t;
        int m = medalFor(t);
        if (m > bestMedal) bestMedal = m;
    }

    // ---- retro rewards ----
    public void setRetroReward(int index) { retroIndex = index; }
    public boolean hasRetroReward() { return retroIndex >= 0; }
    public boolean isRewardClaimed() { return rewardClaimed; }
    public void setRewardClaimed(boolean claimed) { rewardClaimed = claimed; }
    public Car createRewardCar() { return CarShop.createBrokenRetroCar(retroIndex, new Random()); }

    // ------------------------------------------------------------------
    // Track shape
    // ------------------------------------------------------------------
    /** X position of the middle of the road at distance d. */
    public double roadCenter(double d) {
        double start = Math.min(1.0, Math.max(0.0, d) / 700.0);
        double end = Math.min(1.0, Math.max(0.0, (length + 700 - d) / 700.0));
        double k = start * end;       // straight at the start and finish
        double env = 0.72 + 0.28 * Math.sin(d * envFreq + envPhase);
        double sum = 0;
        for (int i = 0; i < 3; i++) sum += amp[i] * Math.sin(d * freq[i] + ph[i]);
        sum = 285.0 * Math.tanh(env * sum / 285.0);
        return ROAD_CENTER_X + k * sum;
    }

    /** How quickly the road bends sideways at distance d. */
    public double roadSlope(double d) {
        return (roadCenter(d + 2) - roadCenter(d - 2)) / 4.0;
    }

    /** Which surface covers the road at distance d. */
    public int surfaceAt(double d) {
        int s = SURF_DRY;
        for (int i = 0; i < surfStart.length; i++) {
            if (surfStart[i] <= d) s = surfType[i]; else break;
        }
        return s;
    }

    // ------------------------------------------------------------------
    // Starting and running a race
    // ------------------------------------------------------------------
    public void prepare(Car playerCar, String playerName) {
        racers = new ArrayList<Racer>();
        traffic = new ArrayList<Racer>();
        knockedOut = new ArrayList<Racer>();
        events = new ArrayList<Event>();
        rng = new Random();
        double diffFactor = Settings.difficulty == Settings.EASY ? 0.96 : Settings.difficulty == Settings.HARD ? 1.04 : 1.0;
        diffFactor *= 0.94;      // opponents also drift and use nitro now, so their base pace is a little lower
        if (mode != MODE_TRIAL) {
            for (Opponent o : opponents) {
                Racer r = new Racer(o.getName(), o.getCar(), false);
                r.topSpeed = o.getTopSpeed(rng) * diffFactor * (mode == MODE_STREET ? 1.03 : 1.0);
                r.skill = o.getSkill();
                r.phase = rng.nextDouble() * 6.28;
                r.laneTimer = rng.nextDouble() * 3;
                racers.add(r);
            }
        }
        player = new Racer(playerName, playerCar, true);
        racers.add(Math.min(2, racers.size()), player);

        // starting grid: two cars per row, behind the start line
        for (int i = 0; i < racers.size(); i++) {
            Racer r = racers.get(i);
            r.dist = -70 - (i / 2) * 75;
            r.offset = (i % 2 == 0) ? -55 : 55;
            r.targetOffset = r.offset;
            r.speed = 0;
        }

        // street races have slow traffic on the road
        if ((mode == MODE_STREET || mode == MODE_POLICE) && trafficCount > 0) {
            ArrayList<Car> catalog = CarShop.getCatalog();
            double d = 700;
            int[] lanes = { -100, 0, 100 };
            for (int i = 0; i < trafficCount; i++) {
                Car c = catalog.get(rng.nextInt(catalog.size())).copy();
                c.setPaint(rng.nextInt(Car.PAINT_NAMES.length));
                Racer t = new Racer("Traffic", c, false);
                t.isTraffic = true;
                t.offset = lanes[rng.nextInt(3)];
                t.dist = d;
                t.speed = 85 + rng.nextDouble() * 70;
                traffic.add(t);
                d += 380 + rng.nextDouble() * 520;
                if (d > length - 500) break;
            }
        }

        police = new ArrayList<Racer>();
        bust = 0;
        busted = false;
        policeSpawnTimer = 9;
        sirenTimer = 1;
        if (mode == MODE_POLICE) {
            for (int i = 0; i < policeStart; i++) spawnPolice(-480 - i * 90, i % 2 == 0 ? -60 : 60);
        }

        countdown = 3.5;
        lastStage = 4;
        time = 0;
        afterFinish = 0;
        finishOrder = 0;
        damageTaken = 0;
        over = false;
        timedOut = false;
        topSpeedReached = 0;
        nitroUses = 0;
        driftBoosts = 0;
        wallHits = 0;
        draftSeconds = 0;
        for (int i = 0; i < 4; i++) surfaceMeters[i] = 0;
        cpHit = 0;
        if (mode == MODE_CHECKPOINT) {
            cpBonus = cpSpacing / (0.86 * Math.max(120, playerCar.getTopSpeedPx())) + 1.2;
            cpStartTime = cpBonus * 1.25 + 4;
            timeLeft = cpStartTime;
        }
        if (mode == MODE_KNOCKOUT) nextKnockout = koInterval + 4;
        warnTimer = 0;
    }

    private static int stageOf(double cd) { return cd > 2.5 ? 3 : cd > 1.5 ? 2 : cd > 0.5 ? 1 : 0; }

    /**
     * Moves everything forward by dt seconds.
     * steer: -1 left, 0 straight, +1 right.
     */
    public void update(double dt, int steer, boolean gas, boolean brake, boolean drift, boolean nitro) {
        if (over || racers.isEmpty()) return;
        if (countdown > 0) {
            countdown -= dt;
            int st = stageOf(countdown);
            if (st != lastStage) {
                lastStage = st;
                events.add(new Event(st > 0 ? EV_COUNT : EV_GO, player, st));
            }
            return;
        }
        time += dt;
        for (Racer r : racers) {
            if (r.eliminated) continue;
            if (r.cooldown > 0) r.cooldown -= dt;
            if (r.blockTimer > 0) r.blockTimer -= dt;
            updateDraft(r, dt);
            if (r.isPlayer) updatePlayer(r, dt, steer, gas, brake, drift, nitro);
            else updateOpponent(r, dt);
        }
        for (Racer t : traffic) t.dist += t.speed * dt;
        if (mode == MODE_POLICE) updatePolice(dt);
        resolveCollisions();
        if (mode == MODE_POLICE) updateBust(dt);

        if (mode == MODE_CHECKPOINT && !player.finished) updateCheckpoints(dt);
        if (mode == MODE_KNOCKOUT) updateKnockout(dt);
        if (mode != MODE_KNOCKOUT) checkFinish(dt);
    }

    // ---- slipstream ----
    private void updateDraft(Racer r, double dt) {
        boolean behind = false;
        for (Racer o : racers) {
            if (o == r || o.eliminated) continue;
            double gap = o.dist - r.dist;
            if (gap > CAR_LENGTH * 0.9 && gap < 170 && Math.abs(o.offset - r.offset) < 28) behind = true;
        }
        for (Racer o : traffic) {
            double gap = o.dist - r.dist;
            if (gap > CAR_LENGTH * 0.9 && gap < 150 && Math.abs(o.offset - r.offset) < 26) behind = true;
        }
        r.draft += (behind && !r.finished ? 1.1 : -1.6) * dt;
        r.draft = clamp(r.draft, 0, 1);
        if (r.isPlayer && r.draft > 0.6) draftSeconds += dt;
    }

    private void updatePlayer(Racer r, double dt, int steer, boolean gas, boolean brake, boolean drift, boolean nitro) {
        Car car = r.car;
        int surf = surfaceAt(r.dist);
        r.surf = surf;
        if (!r.finished) surfaceMeters[surf] += r.speed * dt;
        double cond01 = car.getCondition() / 100.0;
        // the car's condition is checked every frame, so damage hurts straight away
        r.topSpeed = car.getTopSpeedPx();
        r.accel = car.getAccelerationPx();
        r.steerPx = car.getSteeringPx() * (0.88 + 0.12 * cond01);
        int sDrift = skills[0], sNitro = skills[1], sDraft = skills[2];

        if (r.finished) {
            // after the finish line the car rolls to a stop in the middle of the road
            r.speed = Math.max(0, r.speed - 140 * dt);
            r.offset += clamp(-r.offset * 2, -120, 120) * dt;
            r.vx = 0;
            r.drifting = false;
            r.nitroTime = 0;
            r.boostTime = 0;
        } else {
            // ---- nitro ----
            if (nitro && r.nitroTime <= 0 && r.nitroCool <= 0) {
                r.nitroTime = 2.2 * (1 + 0.10 * sNitro);
                nitroUses++;
                events.add(new Event(EV_NITRO, r, 0));
            }
            if (r.nitroTime > 0) {
                r.nitroTime -= dt;
                if (r.nitroTime <= 0) {
                    r.nitroTime = 0;
                    r.nitroCool = 9.0 * (1 - 0.06 * sNitro);
                }
            } else if (r.nitroCool > 0) {
                r.nitroCool -= dt;
            }

            // ---- drift boost ----
            boolean wasDrifting = r.drifting;
            r.drifting = drift && r.speed > 110;
            if (r.drifting && steer != 0) {
                r.boostMeter = Math.min(100, r.boostMeter + (52 + 7 * sDrift) * Math.min(1.0, r.speed / Math.max(1, r.topSpeed)) * dt);
            } else if (!r.drifting) {
                r.boostMeter = Math.max(0, r.boostMeter - 8 * dt);
            }
            if (wasDrifting && !r.drifting && r.boostMeter >= 25) {
                r.boostTime = 0.45 + 1.0 * r.boostMeter / 100.0 + 0.08 * sDrift;
                r.boostMeter = 0;
                driftBoosts++;
                events.add(new Event(EV_BOOST, r, 0));
            }
            if (r.boostTime > 0) r.boostTime -= dt;

            // ---- speed ----
            double mult = 1 + (r.nitroTime > 0 ? 0.32 : 0) + (r.boostTime > 0 ? 0.15 : 0) + r.draft * (0.06 + 0.012 * sDraft);
            double cap = r.topSpeed * mult * SPEED_MULT[surf];
            double acc = r.accel * (1 + (r.nitroTime > 0 ? 1.4 : 0) + (r.boostTime > 0 ? 0.8 : 0) + r.draft * 0.3);
            if (gas) {
                if (r.speed < cap) r.speed = Math.min(cap, r.speed + acc * dt);
            } else {
                r.speed -= 45 * dt;
            }
            if (brake) r.speed -= 340 * BRAKE_MULT[surf] * dt;
            if (r.drifting) r.speed -= 30 * dt;
            if (r.speed > cap) r.speed = Math.max(cap, r.speed - 160 * dt);
            r.speed = Math.max(0, r.speed);

            // ---- sideways movement ----
            double steerRate = r.steerPx * Math.min(1.0, r.speed / 100.0) * STEER_MULT[surf] * (r.drifting ? 1.5 : 1.0);
            // the road bends away from a car that drives straight
            double push = roadSlope(r.dist) * r.speed * DRIFT * PUSH_MULT[surf] * (r.drifting ? 0.85 : 1.0);
            double desired = steer * steerRate - push;
            double grip = GRIP_RATE[surf] * (r.drifting ? 0.5 : 1.0);
            r.vx += (desired - r.vx) * Math.min(1.0, grip * dt);
            r.offset += r.vx * dt;
        }

        // crashing into the barrier hurts the car
        double limit = ROAD_HALF - CAR_HALF_WIDTH - 2;
        if (Math.abs(r.offset) > limit) {
            r.offset = Math.signum(r.offset) * limit;
            r.vx *= -0.2;
            r.speed = Math.max(0, r.speed - 200 * dt);
            if (r.cooldown <= 0 && !r.finished) {
                r.cooldown = 0.8;
                int dmg = 1 + (int) (r.speed / 200);
                car.damage(dmg);
                damageTaken += dmg;
                wallHits++;
                events.add(new Event(EV_WALL, r, r.speed));
                r.speed *= 0.75;
                r.boostMeter = 0;
            }
        }
        r.dist += r.speed * dt;
        topSpeedReached = Math.max(topSpeedReached, r.speed);
    }

    private void updateOpponent(Racer r, double dt) {
        int surf = surfaceAt(r.dist);
        r.surf = surf;
        // pick a new lane every few seconds
        r.laneTimer -= dt;
        if (r.laneTimer <= 0) {
            r.targetOffset = (rng.nextDouble() * 2 - 1) * (ROAD_HALF - 45);
            r.laneTimer = 2 + rng.nextDouble() * 4;
        }

        // drive at the opponent's pace (it wobbles a little over time)
        double want = r.topSpeed * (1 + 0.025 * Math.sin(time * 0.6 + r.phase));
        want *= SPEED_MULT[surf];

        // weaker drivers slow down in sharp curves
        double bend = Math.min(1.0, Math.abs(roadSlope(r.dist)) / 0.6);
        want *= 1 - 0.10 * (1 - r.skill) * bend;

        // opponents drift through curves and fill a boost meter, just like the player
        double bendAbs = Math.abs(roadSlope(r.dist));
        boolean wasDrifting = r.drifting;
        r.drifting = !r.finished && r.skill > 0.25 && bendAbs > 0.3 && r.speed > 150;
        if (r.drifting) {
            r.boostMeter = Math.min(100, r.boostMeter + (35 + 45 * r.skill) * dt);
            want *= 0.97;
        } else {
            r.boostMeter = Math.max(0, r.boostMeter - 10 * dt);
        }
        if (wasDrifting && !r.drifting && r.boostMeter >= 25) {
            r.boostTime = 0.4 + r.boostMeter / 100.0 * 0.9;
            r.boostMeter = 0;
        }
        if (r.boostTime > 0) {
            r.boostTime -= dt;
            want *= 1.08 + 0.06 * r.skill;
        }

        // ...and press their "nitro button" on the straights
        if (!r.finished && r.nitroTime <= 0 && r.nitroCool <= 0 && time > 3) {
            double chance = dt * (bendAbs < 0.3 ? 0.35 : 0.04) * (0.5 + r.skill);
            if (rng.nextDouble() < chance) r.nitroTime = 2.0;
        }
        if (r.nitroTime > 0) {
            r.nitroTime -= dt;
            want *= 1.12 + 0.16 * r.skill;
            if (r.nitroTime <= 0) {
                r.nitroTime = 0;
                r.nitroCool = 9 + rng.nextDouble() * 3;
            }
        } else if (r.nitroCool > 0) {
            r.nitroCool -= dt;
        }
        want *= 1 + r.draft * 0.06;

        // RUBBER BAND: keep the opponents close enough to see and race
        if (!r.finished && !player.finished && mode != MODE_TRIAL) {
            double gap = r.dist - player.dist;           // > 0 means the opponent is ahead
            double maxBoost, maxSlow;
            if (Settings.difficulty == Settings.EASY) { maxBoost = 0.15; maxSlow = 0.14; }
            else if (Settings.difficulty == Settings.HARD) { maxBoost = 0.17; maxSlow = 0.03; }
            else { maxBoost = 0.16; maxSlow = 0.07; }
            if (gap < -150) want *= 1 + Math.min(maxBoost, (-gap - 150) / 450.0 * maxBoost);
            else if (gap > 260) want *= 1 - Math.min(maxSlow, (gap - 260) / 600.0 * maxSlow);
        }
        if (r.finished) want = r.topSpeed * 0.5;

        // don't crash into a slower car in front: slow down and swerve around it
        for (int pass = 0; pass < 2; pass++) {
            List<Racer> others = pass == 0 ? racers : traffic;
            for (Racer o : others) {
                if (o == r || o.eliminated) continue;
                double gap = o.dist - r.dist;
                if (gap > 0 && gap < 115 && Math.abs(o.offset - r.offset) < 40) {
                    want = Math.min(want, o.speed + 5);
                    if (r.blockTimer <= 0) {
                        double side = (r.offset <= o.offset) ? -60 : 60;
                        r.targetOffset = clamp(r.offset + side, -(ROAD_HALF - 45), ROAD_HALF - 45);
                        r.blockTimer = 1.0;
                        r.laneTimer = 2;
                    }
                }
            }
        }

        if (r.speed < want) r.speed = Math.min(want, r.speed + r.accel * (1 + (r.nitroTime > 0 ? 0.8 : 0) + (r.boostTime > 0 ? 0.5 : 0)) * dt);
        else r.speed = Math.max(want, r.speed - 160 * dt);

        double move = clamp((r.targetOffset - r.offset) * 3, -r.steerPx * 0.8, r.steerPx * 0.8);
        r.vx += (move - r.vx) * Math.min(1.0, 8 * dt);
        r.offset += r.vx * dt;
        double limit = ROAD_HALF - CAR_HALF_WIDTH - 2;
        r.offset = clamp(r.offset, -limit, limit);
        r.dist += r.speed * dt;
    }

    private void resolveCollisions() {
        for (int i = 0; i < racers.size(); i++) {
            Racer a = racers.get(i);
            if (a.eliminated) continue;
            for (int j = i + 1; j < racers.size(); j++) {
                Racer b = racers.get(j);
                if (b.eliminated) continue;
                resolvePair(a, b, false);
            }
            for (Racer t : traffic) resolvePair(a, t, true);
            for (Racer pc : police) resolvePair(a, pc, true);
        }
    }

    private void resolvePair(Racer a, Racer b, boolean bIsTraffic) {
        double dd = a.dist - b.dist;
        double dx = a.offset - b.offset;
        double ox = 2 * CAR_HALF_WIDTH - 2 - Math.abs(dx);     // overlap sideways
        double oy = CAR_LENGTH - 2 - Math.abs(dd);              // overlap lengthways
        if (ox <= 0 || oy <= 0) return;

        if (ox / 32.0 < oy / 54.0) {
            // side-to-side bump: push the cars apart
            double push = ox / 2 + 0.5;
            if (bIsTraffic) {
                a.offset += (dx >= 0 ? 1 : -1) * (push * 2);
            } else if (dx >= 0) { a.offset += push; b.offset -= push; }
            else { a.offset -= push; b.offset += push; }
        } else if (bIsTraffic) {
            if (dd < 0) {            // we are behind the traffic car
                a.dist -= oy * 0.5;
                a.speed = Math.min(a.speed, b.speed * 0.9);
            } else {
                a.dist += oy * 0.5;
            }
        } else {
            // rear-end bump: the car behind loses speed
            Racer front = dd >= 0 ? a : b;
            Racer back = dd >= 0 ? b : a;
            back.dist -= oy * 0.5;
            back.speed = Math.min(back.speed, front.speed * 0.96);
        }
        for (Racer r : new Racer[] { a, b }) {
            if (r.isPlayer && r.cooldown <= 0) {
                r.cooldown = 0.8;
                int dmg = bIsTraffic ? 2 : 1;
                if (b.isPolice) bust = Math.min(100, bust + 18);
                r.car.damage(dmg);
                damageTaken += dmg;
                events.add(new Event(bIsTraffic ? EV_TRAFFIC : EV_BUMP, r, 0));
            }
        }
    }

    // ---- police chase ----
    private void spawnPolice(double dist, double offset) {
        ArrayList<Car> catalog = CarShop.getCatalog();
        Car c = catalog.get(Math.min(catalog.size() - 1, 2 + rank * 2 + police.size() % 2)).copy();
        c.setPaint(police.size() % 2 == 0 ? 4 : 3);
        Racer p = new Racer("Police", c, false);
        p.isPolice = true;
        p.dist = dist;
        p.offset = offset;
        p.targetOffset = offset;
        p.accel = 190 + 20 * rank;
        p.steerPx = 240;
        p.speed = 0;
        police.add(p);
    }

    private void updatePolice(double dt) {
        for (Racer p : police) {
            double gap = player.dist - p.dist;                 // > 0: the police car is behind the player
            double pace = player.topSpeed * (0.80 + 0.035 * rank);
            double want = pace;
            if (gap > 110) want = Math.min(pace * (1.08 + 0.03 * rank), player.speed * 1.06 + 30);
            else if (gap < 20) want = Math.min(want, player.speed * 0.97);
            if (player.finished) want = pace * 0.5;
            // aim at the player's car
            p.targetOffset = clamp(player.offset + player.vx * 0.15, -(ROAD_HALF - 45), ROAD_HALF - 45);
            // slow traffic in front: slow down and go around
            for (Racer t : traffic) {
                double g = t.dist - p.dist;
                if (g > 0 && g < 110 && Math.abs(t.offset - p.offset) < 40) {
                    want = Math.min(want, t.speed + 5);
                    p.targetOffset = clamp(p.offset + (p.offset <= t.offset ? -60 : 60), -(ROAD_HALF - 45), ROAD_HALF - 45);
                }
            }
            // keep the police cars from driving through each other
            for (Racer q : police) {
                if (q == p) continue;
                double g = q.dist - p.dist;
                if (g > 0 && g < 70 && Math.abs(q.offset - p.offset) < 36) {
                    want = Math.min(want, q.speed);
                    p.targetOffset = clamp(p.offset + (p.offset <= q.offset ? -50 : 50), -(ROAD_HALF - 45), ROAD_HALF - 45);
                }
            }
            if (p.speed < want) p.speed = Math.min(want, p.speed + p.accel * dt);
            else p.speed = Math.max(want, p.speed - 200 * dt);
            double move = clamp((p.targetOffset - p.offset) * 3, -p.steerPx * 0.8, p.steerPx * 0.8);
            p.vx += (move - p.vx) * Math.min(1.0, 8 * dt);
            p.offset = clamp(p.offset + p.vx * dt, -(ROAD_HALF - CAR_HALF_WIDTH - 2), ROAD_HALF - CAR_HALF_WIDTH - 2);
            p.dist += p.speed * dt;
        }
    }

    private void updateBust(double dt) {
        if (busted) return;
        double rate = 0;
        for (Racer p : police) {
            double gap = player.dist - p.dist;
            double lat = Math.abs(player.offset - p.offset);
            if (gap > -35 && gap < 60 && lat < 45) rate += 28;
            else if (gap > 0 && gap < 140 && lat < 70) rate += 6;
        }
        rate = Math.min(rate, 60);
        if (player.finished) rate = 0;
        if (rate > 0) bust += rate * dt;
        else bust = Math.max(0, bust - 14 * dt);
        if (bust >= 100 && !player.finished) {
            bust = 100;
            busted = true;
            over = true;
            events.add(new Event(EV_BUSTED, player, 0));
            return;
        }
        // more police join the chase as time goes by
        policeSpawnTimer -= dt;
        if (policeSpawnTimer <= 0 && police.size() < policeMax && !player.finished) {
            policeSpawnTimer = 11;
            spawnPolice(player.dist - 560, (rng.nextDouble() * 2 - 1) * 90);
        }
        sirenTimer -= dt;
        if (sirenTimer <= 0) {
            sirenTimer = 1.7;
            events.add(new Event(EV_SIREN, player, 0));
        }
    }

    // ---- checkpoint rush ----
    private void updateCheckpoints(double dt) {
        timeLeft -= dt;
        while (cpHit < cpCount && player.dist >= (cpHit + 1) * cpSpacing) {
            cpHit++;
            timeLeft += cpBonus;
            events.add(new Event(EV_CHECKPOINT, player, cpHit));
        }
        if (timeLeft <= 0 && !player.finished) {
            timeLeft = 0;
            timedOut = true;
            over = true;
            events.add(new Event(EV_TIMEUP, player, 0));
        }
    }

    // ---- knockout ----
    private int activeCount() {
        int n = 0;
        for (Racer r : racers) if (!r.eliminated) n++;
        return n;
    }

    private Racer lastPlaceActive() {
        Racer last = null;
        for (Racer r : racers) {
            if (r.eliminated) continue;
            if (last == null || r.dist < last.dist) last = r;
        }
        return last;
    }

    private void updateKnockout(double dt) {
        nextKnockout -= dt;
        // warn the player when they are about to be knocked out
        if (nextKnockout < 4 && lastPlaceActive() == player) {
            warnTimer -= dt;
            if (warnTimer <= 0) {
                warnTimer = 0.8;
                events.add(new Event(EV_WARNING, player, 0));
            }
        }
        if (nextKnockout > 0) return;
        Racer last = lastPlaceActive();
        if (last == null) return;
        last.eliminated = true;
        knockedOut.add(last);
        events.add(new Event(EV_ELIMINATED, last, 0));
        nextKnockout = koInterval;
        if (last.isPlayer || activeCount() <= 1) over = true;
    }

    private void checkFinish(double dt) {
        boolean allDone = true;
        for (Racer r : racers) {
            if (!r.finished && r.dist >= length) {
                r.finished = true;
                finishOrder++;
                r.place = finishOrder;
                r.finishTime = time - (r.dist - length) / Math.max(r.speed, 1);
                if (r.isPlayer) events.add(new Event(EV_FINISH, r, 0));
            }
            if (!r.finished) allDone = false;
        }
        if (player.finished) {
            afterFinish += dt;
            double wait = (mode == MODE_TRIAL || mode == MODE_POLICE) ? 1.5 : 3.0;
            if (afterFinish >= wait || allDone) over = true;
        }
    }

    /** Ends the race early (player quit). */
    public void forfeit() { over = true; timedOut = true; }

    // ------------------------------------------------------------------
    // Results
    // ------------------------------------------------------------------
    public List<Racer> getStandings() {
        List<Racer> list = new ArrayList<Racer>();
        if (mode == MODE_KNOCKOUT) {
            for (Racer r : racers) if (!r.eliminated) list.add(r);
            Collections.sort(list, new Comparator<Racer>() {
                public int compare(Racer a, Racer b) { return Double.compare(b.dist, a.dist); }
            });
            for (int i = knockedOut.size() - 1; i >= 0; i--) list.add(knockedOut.get(i));
            return list;
        }
        list.addAll(racers);
        Collections.sort(list, new Comparator<Racer>() {
            public int compare(Racer a, Racer b) {
                if (a.finished && b.finished) return Double.compare(a.finishTime, b.finishTime);
                if (a.finished) return -1;
                if (b.finished) return 1;
                return Double.compare(b.dist, a.dist);
            }
        });
        return list;
    }

    /** Final place of the player (1 = winner). If the player quit or ran out of time, they finish last. */
    public int getPlayerPlace() {
        if (mode == MODE_POLICE) return player.finished && !busted ? 1 : 2;
        if (mode == MODE_KNOCKOUT) {
            if (timedOut && !player.eliminated) return racers.size();
            return getStandings().indexOf(player) + 1;
        }
        if (!player.finished) return racers.size();
        return getStandings().indexOf(player) + 1;
    }

    /** Where the player is right now while racing. */
    public int getLivePlace(Racer who) {
        int pos = 1;
        for (Racer o : racers) {
            if (o == who || o.eliminated) continue;
            boolean ahead;
            if (o.finished && who.finished) ahead = o.finishTime < who.finishTime;
            else if (o.finished) ahead = true;
            else if (who.finished) ahead = false;
            else ahead = o.dist > who.dist;
            if (ahead) pos++;
        }
        return pos;
    }

    public List<Racer> getRacers() { return racers; }
    public Racer getPlayer() { return player; }
    public double getCountdown() { return countdown; }
    public double getTime() { return time; }
    public boolean isOver() { return over; }
    public int getDamageTaken() { return damageTaken; }
    public int getRacerCount() { return racers.size(); }
    public int getActiveCount() { return activeCount(); }
    public double getPlayerFinishTime() { return player == null ? 0 : player.finishTime; }

    /** Things that happened since the last call (the screen plays sounds for them). */
    public List<Event> drainEvents() {
        List<Event> copy = new ArrayList<Event>(events);
        events.clear();
        return copy;
    }

    // ---- statistics (sponsor challenges) ----
    public double getTopSpeedReached() { return topSpeedReached; }
    public int getNitroUses() { return nitroUses; }
    public int getDriftBoosts() { return driftBoosts; }
    public int getWallHits() { return wallHits; }
    public double getDraftSeconds() { return draftSeconds; }
    public double getSurfaceMeters(int surf) { return surfaceMeters[surf]; }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ------------------------------------------------------------------
    // Theme colours (used by the race screen to paint each track differently)
    // ------------------------------------------------------------------
    /** Returns {colourA, colourB, speckle} for the ground. side: -1 left, +1 right. */
    public static Color[] getGroundColors(int theme, int side) {
        Color[] water = { new Color(48, 110, 200), new Color(42, 100, 190), new Color(130, 185, 245) };
        switch (theme) {
            case THEME_CITY:
                return new Color[] { new Color(105, 108, 118), new Color(98, 101, 111), new Color(132, 135, 144) };
            case THEME_SUBURB:
                return new Color[] { new Color(70, 160, 70), new Color(62, 148, 62), new Color(125, 205, 105) };
            case THEME_COUNTRY:
                return new Color[] { new Color(150, 175, 70), new Color(138, 163, 62), new Color(205, 195, 95) };
            case THEME_FOREST:
                return new Color[] { new Color(36, 105, 52), new Color(30, 94, 46), new Color(62, 142, 72) };
            case THEME_INDUSTRIAL:
                return new Color[] { new Color(118, 106, 92), new Color(108, 96, 84), new Color(152, 137, 112) };
            case THEME_BRIDGE:
                return water;
            case THEME_SHORE:
                if (side < 0) return water;
                return new Color[] { new Color(224, 204, 140), new Color(214, 194, 130), new Color(246, 232, 172) };
            default:
                return new Color[] { new Color(112, 98, 84), new Color(102, 88, 75), new Color(152, 137, 122) };
        }
    }

    // ------------------------------------------------------------------
    // Race Track lobby pixel art (240 x 160 grid)
    // ------------------------------------------------------------------
    public static void paintTrackLobby(Graphics2D g) {
        // sky
        for (int i = 0; i < 8; i++) {
            GameWindow.fill(g, new Color(100 + i * 10, 170 + i * 8, 240), 0, i * 8, 240, 8);
        }
        // clouds
        int[][] clouds = { {20, 14, 30}, {110, 8, 36}, {180, 22, 28} };
        for (int[] c : clouds) {
            GameWindow.fill(g, Color.WHITE, c[0], c[1], c[2], 6);
            GameWindow.fill(g, Color.WHITE, c[0] + 5, c[1] - 3, c[2] - 12, 4);
        }
        // distant hills
        GameWindow.fill(g, new Color(90, 160, 100), 0, 58, 240, 20);
        for (int x = 0; x < 240; x += 40) GameWindow.fill(g, new Color(90, 160, 100), x, 50, 28, 10);
        GameWindow.fill(g, new Color(70, 140, 85), 0, 74, 240, 8);

        // grandstand with crowd
        GameWindow.fill(g, new Color(120, 124, 138), 10, 54, 220, 46);
        GameWindow.fill(g, new Color(86, 90, 104), 10, 54, 220, 3);
        for (int row = 0; row < 4; row++) {
            GameWindow.fill(g, new Color(100, 104, 118), 10, 62 + row * 9, 220, 1);
            for (int i = 0; i < 44; i++) {
                int k = (i * 31 + row * 17) % 7;
                Color[] shirts = { new Color(220, 60, 60), new Color(60, 110, 220), new Color(240, 200, 50),
                                   new Color(240, 240, 240), new Color(70, 180, 100), new Color(240, 130, 40), new Color(170, 80, 200) };
                GameWindow.fill(g, shirts[k], 12 + i * 5, 58 + row * 9, 3, 4);
                GameWindow.fill(g, new Color(240, 200, 160), 12 + i * 5, 56 + row * 9, 3, 2);
            }
        }
        // roof over the stand
        GameWindow.fill(g, new Color(200, 50, 50), 6, 46, 228, 6);
        GameWindow.fill(g, new Color(150, 36, 40), 6, 52, 228, 2);

        // asphalt track and barrier
        GameWindow.fill(g, new Color(200, 200, 205), 0, 100, 240, 4);
        for (int x = 0; x < 240; x += 16) GameWindow.fill(g, new Color(210, 50, 50), x, 100, 8, 4);
        GameWindow.fill(g, new Color(68, 68, 78), 0, 104, 240, 56);
        GameWindow.fill(g, new Color(58, 58, 68), 0, 104, 240, 2);
        for (int x = 0; x < 240; x += 24) GameWindow.fill(g, Color.WHITE, x, 132, 12, 2);

        // checkered start line
        for (int y = 108; y < 156; y += 6) {
            for (int c = 0; c < 2; c++) {
                boolean dark = ((y / 6) + c) % 2 == 0;
                GameWindow.fill(g, dark ? Color.BLACK : Color.WHITE, 112 + c * 6, y, 6, 6);
            }
        }
        // checkered banner on poles
        GameWindow.fill(g, new Color(60, 60, 70), 24, 70, 4, 34);
        GameWindow.fill(g, new Color(60, 60, 70), 212, 70, 4, 34);
        for (int x = 24; x < 216; x += 6) {
            GameWindow.fill(g, (x / 6) % 2 == 0 ? Color.BLACK : Color.WHITE, x, 70, 6, 3);
            GameWindow.fill(g, (x / 6) % 2 == 0 ? Color.WHITE : Color.BLACK, x, 73, 6, 3);
        }
    }
}
