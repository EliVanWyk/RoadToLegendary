package racing;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * The career: 5 ranks x 5 races = 25 races, plus all the rules for moving up,
 * and everything the extra city buildings need (championship cups, knockout
 * and checkpoint events, street races, time trials and sponsor challenges).
 *
 * Rank rules:
 *  - You play the 5 races of your current rank in order.
 *  - You must WIN a race (finish 1st) to unlock the next one.
 *  - Win all 5 races of a rank and the next rank unlocks.
 *  - You can replay any race you have unlocked (to earn more money).
 */
public class Career {

    public static final String[] RANK_NAMES = { "Beginner", "Amateur", "Professional", "Expert", "Legendary" };
    public static final int RACES_PER_RANK = 5;
    public static final int WINS_NEEDED = 5;
    public static final int TOTAL_RACES = 25;

    /** Prize money: [rank][place - 1] for 1st, 2nd, 3rd. */
    public static final int[][] PRIZES = {
        { 150, 100, 75 },
        { 300, 200, 125 },
        { 600, 400, 250 },
        { 1000, 700, 450 },
        { 2000, 1400, 900 }
    };

    // ---- race data table (name, track theme, night?) ----
    private static final String[] RACE_NAMES = {
        "Sunrise Street Sprint", "Maple Suburb Run", "Cornfield Cruise", "Harbor Bridge Dash", "Rookie Cup Final",
        "Pine Ridge Loop", "Old Mill Bypass", "Riverside Drive", "Factory Row Sprint", "Amateur Cup Final",
        "Downtown Night Run", "Lakeshore Cruise", "Redwood Canyon", "Steelworks Circuit", "Professional Cup",
        "Skyline Bridge Run", "Harbor Docks Dash", "Mountain Switchback", "Neon Boulevard", "Expert Grand Prix",
        "Midnight Highway", "Cliffside Serpentine", "Iron Valley Gauntlet", "Coastal Thunder", "Legendary Championship"
    };
    private static final int[] RACE_THEMES = {
        Race.THEME_CITY, Race.THEME_SUBURB, Race.THEME_COUNTRY, Race.THEME_BRIDGE, Race.THEME_CITY,
        Race.THEME_FOREST, Race.THEME_COUNTRY, Race.THEME_SHORE, Race.THEME_INDUSTRIAL, Race.THEME_SUBURB,
        Race.THEME_CITY, Race.THEME_SHORE, Race.THEME_FOREST, Race.THEME_INDUSTRIAL, Race.THEME_CITY,
        Race.THEME_BRIDGE, Race.THEME_INDUSTRIAL, Race.THEME_MOUNTAIN, Race.THEME_CITY, Race.THEME_COUNTRY,
        Race.THEME_COUNTRY, Race.THEME_MOUNTAIN, Race.THEME_INDUSTRIAL, Race.THEME_SHORE, Race.THEME_CITY
    };
    private static final boolean[] RACE_NIGHT = {
        false, false, false, false, false,
        false, false, false, false, false,
        true, false, false, false, false,
        false, true, false, true, false,
        true, false, false, false, true
    };
    /** Race numbers that can give a retro car, and which retro car (index in CarShop). */
    private static final int[] RETRO_RACE_NUMBERS = { 4, 8, 11, 14, 17, 20, 23, 25 };

    // ---- names of the special events (one for each rank) ----
    public static final String[] CUP_NAMES = { "BRONZE CUP", "SILVER CUP", "GOLD CUP", "PLATINUM CUP", "LEGENDARY CUP" };
    public static final String[] KNOCKOUT_NAMES = { "Rookie Knockout", "Amateur Knockout", "Pro Knockout", "Expert Knockout", "Legend Knockout" };
    public static final String[] CHECKPOINT_NAMES = { "Rookie Rush", "Amateur Rush", "Pro Rush", "Expert Rush", "Legend Rush" };
    public static final String[] STREET_NAMES = { "Back Alley Dash", "Downtown Duel", "Harbor Run", "Skyline Showdown", "Street King Final" };
    public static final String[] POLICE_NAMES = { "Traffic Stop Dash", "Highway Escape", "Dock Run Getaway", "Skyline Getaway", "Most Wanted" };
    private static final int[] CUP_POINTS = { 25, 18, 15, 12, 10, 8 };

    private final Race[] races = new Race[TOTAL_RACES];
    private final Cup[] cups = new Cup[5];
    private final Race[] knockoutEvents = new Race[5];
    private final Race[] checkpointEvents = new Race[5];
    private final Race[] streetEvents = new Race[5];
    private final Race[] policeEvents = new Race[5];

    /** What happened after a race (shown on the results screen). */
    public static class RaceOutcome {
        public int mode;
        public int place;
        public int racerCount;
        public int prize;
        public boolean win;
        public boolean rankedUp;
        public boolean careerComplete;
        public boolean paintShopUnlocked;
        public int newRank;
        public Car rewardCar;           // a broken retro car, or null
        public int repGain;
        public int wagerNet;            // street races: money won (+) or lost (-)
        public int wager;
        public boolean busted;
        public int fine;
        public int medal;               // time trial: 0 none .. 3 gold
        public double trialTime;
        public int skillPointsGained;
        public boolean timedOut;
        public boolean cupRoundDone;
        public boolean cupFinished;
        public int cupPlace;
        public int cupPrize;
        public int cupPoints;
        public ArrayList<Challenge> challenges = new ArrayList<Challenge>();
        public ArrayList<Integer> newBuildings = new ArrayList<Integer>();
    }

    // ------------------------------------------------------------------
    // Championship cups
    // ------------------------------------------------------------------
    /** One championship: 4 races against the same 5 rivals, points add up. */
    public static class Cup {
        public final int tier;
        public final Race[] rounds = new Race[4];
        public final ArrayList<Opponent> field;
        public final LinkedHashMap<String, Integer> points = new LinkedHashMap<String, Integer>();
        public final int[] playerPlaces = new int[4];
        public int roundsDone = 0;
        public boolean started = false;
        public boolean finished = false;
        public int finalPlace = 0;
        public int timesWon = 0;
        private String playerName = "";

        Cup(int tier, ArrayList<Opponent> field) {
            this.tier = tier;
            this.field = field;
        }

        public void start(String name) {
            playerName = name;
            points.clear();
            points.put(name, 0);
            for (Opponent o : field) points.put(o.getName(), 0);
            roundsDone = 0;
            started = true;
            finished = false;
            finalPlace = 0;
            for (int i = 0; i < 4; i++) playerPlaces[i] = 0;
        }

        void restore(boolean started, boolean finished, int roundsDone, int finalPlace, int timesWon, String[] places, String player, String pts) {
            this.started = started;
            this.finished = finished;
            this.roundsDone = Math.max(0, Math.min(4, roundsDone));
            this.finalPlace = finalPlace;
            this.timesWon = timesWon;
            for (int i = 0; i < 4 && i < places.length; i++) playerPlaces[i] = toInt(places[i], 0);
            this.playerName = player;
            points.clear();
            for (String e : pts.split(";")) {
                int c = e.lastIndexOf(':');
                if (c > 0) points.put(e.substring(0, c), toInt(e.substring(c + 1), 0));
            }
        }

        public Race getNextRace() { return (started && !finished && roundsDone < 4) ? rounds[roundsDone] : null; }

        /** Adds up the points after a round. Returns the player's points from it. */
        int record(Race race) {
            List<Race.Racer> st = race.getStandings();
            int gained = 0;
            for (int i = 0; i < st.size(); i++) {
                int pts = i < CUP_POINTS.length ? CUP_POINTS[i] : 0;
                Race.Racer r = st.get(i);
                String key = r.isPlayer ? playerName : r.name;
                Integer old = points.get(key);
                points.put(key, (old == null ? 0 : old) + pts);
                if (r.isPlayer) {
                    gained = pts;
                    playerPlaces[roundsDone] = i + 1;
                }
            }
            roundsDone++;
            if (roundsDone >= 4) {
                finished = true;
                List<Map.Entry<String, Integer>> table = getTable();
                for (int i = 0; i < table.size(); i++) {
                    if (table.get(i).getKey().equals(playerName)) finalPlace = i + 1;
                }
            }
            return gained;
        }

        /** The points table, best first. */
        public List<Map.Entry<String, Integer>> getTable() {
            List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(points.entrySet());
            Collections.sort(list, new java.util.Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                    return b.getValue() - a.getValue();
                }
            });
            return list;
        }

        public String getPlayerName() { return playerName; }
    }

    // ------------------------------------------------------------------
    // Sponsor challenges
    // ------------------------------------------------------------------
    public static class Challenge {
        public final String id, sponsor, title, text;
        public final int money, rep;
        Challenge(String id, String sponsor, String title, String text, int money, int rep) {
            this.id = id; this.sponsor = sponsor; this.title = title; this.text = text;
            this.money = money; this.rep = rep;
        }
    }

    public static final Challenge[] CHALLENGES = {
        new Challenge("stock", "Pinewood Tires", "Stock Hero", "Win a race in a car with NO upgrades", 400, 25),
        new Challenge("clean", "Gleam Wax", "Clean Sweep", "Win a race without any damage", 500, 30),
        new Challenge("night", "Neon Energy", "Night Owl", "Win a race that is held at night", 500, 30),
        new Challenge("retro", "Vintage Garage", "Retro Pride", "Win a race in a retro car", 1000, 50),
        new Challenge("wet", "Splash Wipers", "Rain Master", "Win after driving a long way on wet road", 600, 30),
        new Challenge("gravel", "RockGrip", "Gravel Ace", "Win after driving a long way on gravel", 600, 30),
        new Challenge("ice", "Frost Cola", "Ice Cold", "Finish a race after driving on ice", 500, 30),
        new Challenge("drift", "Slide Oil", "Drift King", "Get 4 drift boosts in a single race", 600, 35),
        new Challenge("nitro", "Boost Fuel", "Nitro Rush", "Use nitro 3 times in one race and win", 500, 30),
        new Challenge("draft", "AeroFlow", "Slipstream Pro", "Draft behind other cars for 8 seconds", 400, 25),
        new Challenge("speed", "Redline Radio", "Speed Demon", "Reach 240 KM/H (150 MPH)", 700, 40),
        new Challenge("streak", "Champion Spark", "Hot Streak", "Win 3 races in a row", 800, 40),
        new Challenge("street", "Backalley Tires", "Street Legend", "Win a street race", 700, 40),
        new Challenge("knockout", "Last Stand Gear", "Survivor", "Win a Knockout event", 700, 40),
        new Challenge("rush", "TickTock Watches", "Beat the Clock", "Win a Checkpoint Rush", 700, 40),
        new Challenge("gold", "Gold Rim Co.", "Gold Standard", "Earn a gold medal in a time trial", 800, 40),
        new Challenge("derby", "Crash Test Co.", "Last Car Standing", "Win a demolition derby", 800, 45),
        new Challenge("escape", "Shade Sunglasses", "Great Escape", "Escape from a Police Chase", 900, 45),
        new Challenge("cup", "Trophy Time", "Champion", "Win a championship cup", 1500, 80)
    };

    public Career() {
        buildRaces();
        buildEvents();
    }

    // ------------------------------------------------------------------
    // Building the 25 races
    // ------------------------------------------------------------------
    private ArrayList<Opponent> makeField(int rank, int count, int levelBoost, int pseudoNumber, Random rng) {
        ArrayList<Car> catalog = CarShop.getCatalog();
        ArrayList<Car> pool = new ArrayList<Car>();
        for (Car c : catalog) {
            if (c.getRankRequired() == rank) pool.add(c);
        }
        ArrayList<String> names = new ArrayList<String>();
        Collections.addAll(names, Opponent.NAMES);
        Collections.shuffle(names, rng);

        ArrayList<Opponent> list = new ArrayList<Opponent>();
        for (int i = 0; i < count; i++) {
            Car car = pool.get((i + levelBoost) % pool.size()).copy();
            car.setUpgradeLevels(Math.min(Car.MAX_UPGRADE_LEVEL, levelBoost));
            double skill = Math.min(0.95, 0.35 + 0.022 * (pseudoNumber - 1) + rng.nextDouble() * 0.10);
            list.add(new Opponent(names.get(i), car, skill));
        }
        return list;
    }

    private void buildRaces() {
        Random rng = new Random(2026);
        for (int n = 1; n <= TOTAL_RACES; n++) {
            int rank = (n - 1) / RACES_PER_RANK;
            int idx = (n - 1) % RACES_PER_RANK;
            Race race = new Race(n, RACE_NAMES[n - 1], rank, idx, RACE_THEMES[n - 1], RACE_NIGHT[n - 1], PRIZES[rank]);

            int count = 3 + (idx >= 2 ? 1 : 0) + (idx == 4 ? 1 : 0);   // 3,3,4,4,5 opponents
            race.setOpponents(makeField(rank, count, idx / 2, n, rng));

            for (int k = 0; k < RETRO_RACE_NUMBERS.length; k++) {
                if (RETRO_RACE_NUMBERS[k] == n) race.setRetroReward(k);
            }
            races[n - 1] = race;
        }
    }

    private static int[] scaled(int[] p, double f) {
        return new int[] { (int) Math.round(p[0] * f), (int) Math.round(p[1] * f), (int) Math.round(p[2] * f) };
    }

    /** Builds the championship cups and the Knockout / Checkpoint / Street events for every rank. */
    private void buildEvents() {
        Random rng = new Random(77);
        for (int r = 0; r < 5; r++) {
            Race base = getRaceInRank(r, 2);
            int pseudo = r * 5 + 3;

            // championship cup: 4 rounds against the same 5 rivals
            Cup cup = new Cup(r, makeField(r, 5, 1 + r / 2, pseudo + 1, rng));
            for (int k = 0; k < 4; k++) {
                Race b = getRaceInRank(r, k);
                Race rr = b.makeVariant(Race.MODE_CHAMPIONSHIP, scaled(PRIZES[r], 0.5));
                rr.setSeed(b.getSeed() + 4242 + k * 17);
                rr.setName(CUP_NAMES[r] + " round " + (k + 1) + ": " + b.getName());
                rr.setCup(r);
                rr.setOpponents(cup.field);
                cup.rounds[k] = rr;
            }
            cups[r] = cup;

            // knockout
            Race ko = base.makeVariant(Race.MODE_KNOCKOUT, scaled(PRIZES[r], 1.5));
            ko.setName(KNOCKOUT_NAMES[r]);
            ko.setSeed(base.getSeed() + 777);
            ko.setLength(60000);
            ko.setKnockoutInterval(15 - r);
            ko.setOpponents(makeField(r, 5, 1 + r / 2, pseudo, rng));
            knockoutEvents[r] = ko;

            // checkpoint rush
            Race cp = base.makeVariant(Race.MODE_CHECKPOINT, scaled(PRIZES[r], 1.5));
            cp.setName(CHECKPOINT_NAMES[r]);
            cp.setSeed(base.getSeed() + 888);
            cp.setCheckpoints(2800 + 250 * r, 7);
            cp.setOpponents(makeField(r, 3, 1 + r / 2, pseudo, rng));
            checkpointEvents[r] = cp;

            // street race at night with traffic
            Race st = base.makeVariant(Race.MODE_STREET, scaled(PRIZES[r], 0.5));
            st.setName(STREET_NAMES[r]);
            st.setTheme(Race.THEME_CITY);
            st.setNight(true);
            st.setSeed(base.getSeed() + 1234);
            st.setLength(10500 + r * 1500);
            st.setTrafficCount(16 + r * 3);
            st.setOpponents(makeField(r, 4, 1 + r / 2, pseudo + 2, rng));
            streetEvents[r] = st;

            // police chase: the cops are after you!
            Race po = base.makeVariant(Race.MODE_POLICE, scaled(PRIZES[r], 1.5));
            po.setName(POLICE_NAMES[r]);
            po.setNight(r % 2 == 1);
            po.setSeed(base.getSeed() + 2468);
            po.setLength(9000 + r * 1500);
            po.setTrafficCount(8 + r * 3);
            po.setPolice(2 + r / 2, 3 + r);
            policeEvents[r] = po;
        }
    }

    // ------------------------------------------------------------------
    // Reading career information
    // ------------------------------------------------------------------
    public Race[] getRaces() { return races; }
    public Race getRace(int number) { return races[number - 1]; }
    public Cup getCup(int tier) { return cups[tier]; }
    public Race getKnockoutEvent(int tier) { return knockoutEvents[tier]; }
    public Race getCheckpointEvent(int tier) { return checkpointEvents[tier]; }
    public Race getStreetEvent(int tier) { return streetEvents[tier]; }
    public Race getPoliceEvent(int tier) { return policeEvents[tier]; }

    public Race getRaceInRank(int rank, int indexInRank) {
        return races[rank * RACES_PER_RANK + indexInRank];
    }

    public int getCompletedInRank(int rank) {
        int n = 0;
        for (int i = 0; i < RACES_PER_RANK; i++) {
            if (getRaceInRank(rank, i).isCompleted()) n++;
        }
        return n;
    }

    public int getWinsInRank(int rank) {
        int n = 0;
        for (int i = 0; i < RACES_PER_RANK; i++) {
            if (getRaceInRank(rank, i).isWon()) n++;
        }
        return n;
    }

    public int getTotalCompleted() {
        int n = 0;
        for (Race r : races) if (r.isCompleted()) n++;
        return n;
    }

    public int getTotalWon() {
        int n = 0;
        for (Race r : races) if (r.isWon()) n++;
        return n;
    }

    public int getTotalMedals() {
        int n = 0;
        for (Race r : races) n += r.getBestMedal();
        return n;
    }

    public boolean isCareerComplete() {
        return getWinsInRank(4) >= WINS_NEEDED;
    }

    /**
     * Races of lower ranks can always be replayed. In your current rank you must
     * WIN a race to unlock the next one. Higher ranks are locked.
     */
    public boolean isRaceUnlocked(Driver driver, Race race) {
        if (race.getRank() < driver.getRank()) return true;
        if (race.getRank() > driver.getRank()) return false;
        return race.getIndexInRank() == 0 || getRaceInRank(race.getRank(), race.getIndexInRank() - 1).isWon();
    }

    /** Practice Garage can train on any race you have already completed. */
    public boolean isTrialUnlocked(Race race) {
        return race.isCompleted();
    }

    // ---- city buildings ----
    public boolean isBuildingUnlocked(Driver driver, int building) {
        switch (building) {
            case CityMap.PAINT_SHOP: return driver.getRank() >= 1;
            case CityMap.CHAMPIONSHIP: return driver.getRank() >= 1;
            case CityMap.PRACTICE: return getTotalCompleted() >= 1;
            case CityMap.KNOCKOUT: return driver.getWins() >= 2;
            case CityMap.STREET: return driver.getRepLevel() >= 3;
            case CityMap.POLICE: return driver.getWins() >= 3;
            case CityMap.DERBY: return driver.getWins() >= 1;
            default: return true;
        }
    }

    public String getBuildingLockText(int building) {
        switch (building) {
            case CityMap.PAINT_SHOP: return "Unlocks at Amateur rank";
            case CityMap.CHAMPIONSHIP: return "Unlocks at Amateur rank";
            case CityMap.PRACTICE: return "Finish your first race to unlock it";
            case CityMap.KNOCKOUT: return "Win 2 races to unlock it";
            case CityMap.STREET: return "Reach reputation level 3 (Known Racer)";
            case CityMap.POLICE: return "Win 3 races to unlock it";
            case CityMap.DERBY: return "Win your first race to unlock it";
            default: return "";
        }
    }

    private int unlockMask(Driver d) {
        int m = 0;
        for (int i = 0; i < CityMap.BUILDING_COUNT; i++) if (isBuildingUnlocked(d, i)) m |= (1 << i);
        return m;
    }

    // ------------------------------------------------------------------
    // Finishing a race
    // ------------------------------------------------------------------
    /** Updates the driver and career after a race and says what happened. */
    public RaceOutcome finishRace(Driver driver, Race race, int place, int racerCount, int wager) {
        RaceOutcome out = new RaceOutcome();
        int before = unlockMask(driver);
        int mode = race.getMode();
        out.mode = mode;
        out.place = place;
        out.racerCount = racerCount;
        out.timedOut = race.isTimedOut();
        Car car = driver.getCurrentCar();

        if (mode == Race.MODE_TRIAL) {
            finishTrial(driver, race, out);
            collectNewBuildings(driver, before, out);
            return out;
        }

        out.win = (place == 1);
        out.prize = race.getPrize(place);
        if (mode == Race.MODE_POLICE) out.prize = out.win ? race.getPrize(1) : 0;
        boolean alreadyWon = race.isWon();

        driver.recordRace(out.win, out.prize);
        race.recordResult(place);

        // ---- mode specific results ----
        if (mode == Race.MODE_NORMAL) {
            // special retro-car reward for winning certain races
            if (out.win && race.hasRetroReward() && !race.isRewardClaimed()) {
                out.rewardCar = race.createRewardCar();
            }
            // rank progression (only when racing in your current rank)
            int rank = driver.getRank();
            if (race.getRank() == rank && getWinsInRank(rank) >= WINS_NEEDED) {
                if (rank < RANK_NAMES.length - 1) {
                    driver.setRank(rank + 1);
                    out.rankedUp = true;
                    out.newRank = rank + 1;
                    out.paintShopUnlocked = (rank + 1 == 1);
                } else {
                    out.careerComplete = true;
                }
            }
        } else if (mode == Race.MODE_CHAMPIONSHIP) {
            Cup cup = cups[race.getCup()];
            out.cupPoints = cup.record(race);
            out.cupRoundDone = true;
            if (cup.finished) {
                out.cupFinished = true;
                out.cupPlace = cup.finalPlace;
                int[] top = PRIZES[cup.tier];
                if (cup.finalPlace >= 1 && cup.finalPlace <= 3) {
                    out.cupPrize = top[cup.finalPlace - 1] * (cup.finalPlace == 1 ? 5 : cup.finalPlace == 2 ? 3 : 2);
                    driver.earn(out.cupPrize);
                }
                if (cup.finalPlace == 1) {
                    driver.recordCupWon();
                    cup.timesWon++;
                }
            }
        } else if (mode == Race.MODE_STREET) {
            out.wager = wager;
            out.wagerNet = place == 1 ? 2 * wager : place == 2 ? 0 : -wager;
            if (out.wagerNet > 0) driver.earn(out.wagerNet);
            else if (out.wagerNet < 0) driver.spend(Math.min(-out.wagerNet, driver.getMoney()));
            if (out.win) driver.recordStreetWin();
        } else if (mode == Race.MODE_POLICE) {
            out.busted = race.isBusted();
            if (out.busted) {
                out.fine = Math.min(driver.getMoney(), PRIZES[race.getRank()][2]);
                driver.spend(out.fine);
            }
            if (out.win) driver.recordPoliceWin();
        } else if (mode == Race.MODE_KNOCKOUT) {
            if (out.win) driver.recordKnockoutWin();
        } else if (mode == Race.MODE_CHECKPOINT) {
            if (out.win && !race.isTimedOut()) driver.recordCheckpointWin();
        }

        // ---- reputation ----
        int base = place == 1 ? 30 : place == 2 ? 18 : place == 3 ? 10 : 3;
        double factor = 1 + 0.25 * race.getRank();
        double modeFactor = mode == Race.MODE_POLICE ? 1.7 : mode == Race.MODE_STREET ? 1.6 : mode == Race.MODE_NORMAL ? 1.0 : mode == Race.MODE_CHAMPIONSHIP ? 1.2 : 1.3;
        double rep = base * factor * modeFactor;
        if (mode == Race.MODE_NORMAL && alreadyWon) rep *= 0.3;          // replaying a won race pays less fame
        out.repGain = (int) Math.round(rep);
        if (out.cupFinished && out.cupPlace >= 1 && out.cupPlace <= 3) out.repGain += out.cupPlace == 1 ? 100 : out.cupPlace == 2 ? 60 : 30;
        driver.addReputation(out.repGain);

        // ---- sponsor challenges ----
        for (Challenge c : CHALLENGES) {
            if (driver.isChallengeDone(c.id)) continue;
            if (challengeMet(c.id, driver, race, out, car)) {
                driver.markChallengeDone(c.id);
                driver.earn(c.money);
                driver.addReputation(c.rep);
                out.challenges.add(c);
            }
        }
        collectNewBuildings(driver, before, out);
        return out;
    }

    private void collectNewBuildings(Driver driver, int before, RaceOutcome out) {
        int after = unlockMask(driver);
        for (int i = 0; i < CityMap.BUILDING_COUNT; i++) {
            if ((after & (1 << i)) != 0 && (before & (1 << i)) == 0) out.newBuildings.add(i);
        }
    }

    private static boolean isStock(Car car) {
        for (int i = 0; i < 3; i++) if (car.getUpgradeLevel(i) > 0) return false;
        return true;
    }

    private boolean challengeMet(String id, Driver d, Race race, RaceOutcome out, Car car) {
        boolean won = out.win && !race.isTimedOut();
        boolean finished = !race.isTimedOut();
        switch (id) {
            case "stock": return won && isStock(car);
            case "clean": return won && race.getDamageTaken() == 0;
            case "night": return won && race.isNight();
            case "retro": return won && car.isRetro();
            case "wet": return won && race.getSurfaceMeters(Race.SURF_WET) >= 800;
            case "gravel": return won && race.getSurfaceMeters(Race.SURF_GRAVEL) >= 800;
            case "ice": return finished && race.getSurfaceMeters(Race.SURF_ICE) >= 300;
            case "drift": return finished && race.getDriftBoosts() >= 4;
            case "nitro": return won && race.getNitroUses() >= 3;
            case "draft": return finished && race.getDraftSeconds() >= 8;
            case "speed": return finished && race.getTopSpeedReached() * 0.55 >= 240;
            case "streak": return d.getWinStreak() >= 3;
            case "street": return won && race.getMode() == Race.MODE_STREET;
            case "knockout": return won && race.getMode() == Race.MODE_KNOCKOUT;
            case "rush": return won && race.getMode() == Race.MODE_CHECKPOINT;
            case "escape": return won && race.getMode() == Race.MODE_POLICE;
            case "cup": return out.cupFinished && out.cupPlace == 1;
            default: return false;
        }
    }

    /** Time trial: medals give skill points (first time you reach each medal on a race). */
    private void finishTrial(Driver driver, Race race, RaceOutcome out) {
        Race base = getRace(race.getNumber());
        if (race.isTimedOut() || race.getPlayerFinishTime() <= 0) {
            out.medal = 0;
            out.timedOut = true;
            return;
        }
        double t = race.getPlayerFinishTime();
        out.trialTime = t;
        out.medal = base.medalFor(t);
        int[] pts = { 0, 1, 2, 4 };
        int oldMedal = base.getBestMedal();
        base.recordTime(t);
        if (out.medal > oldMedal) {
            out.skillPointsGained = pts[out.medal] - pts[oldMedal];
            driver.addSkillPoints(out.skillPointsGained);
        }
        if (out.medal == 3) driver.recordGold();
        for (Challenge c : CHALLENGES) {
            if (c.id.equals("gold") && out.medal == 3 && !driver.isChallengeDone("gold")) {
                driver.markChallengeDone("gold");
                driver.earn(c.money);
                driver.addReputation(c.rep);
                out.challenges.add(c);
            }
        }
    }

    /** What happened after a demolition derby. */
    public static class DerbyOutcome {
        public DerbyCar car;
        public int place, kos, prize, koBonus, repGain;
        public ArrayList<Challenge> challenges = new ArrayList<Challenge>();
    }

    /** Pays the prize and fame for a finished derby (the car itself is scrapped). */
    DerbyOutcome finishDerby(Driver driver, DerbyCar car, int place, int kos) {
        DerbyOutcome o = new DerbyOutcome();
        o.car = car;
        o.place = place;
        o.kos = kos;
        o.prize = car.prize(place);
        o.koBonus = 75 * kos;
        driver.earn(o.prize + o.koBonus);
        driver.recordDerby(place == 1);
        int base = place == 1 ? 35 : place == 2 ? 20 : place == 3 ? 12 : 4;
        o.repGain = (int) Math.round((base + 4 * kos) * (1 + 0.2 * driver.getRank()));
        driver.addReputation(o.repGain);
        if (place == 1) {
            for (Challenge ch : CHALLENGES) {
                if (ch.id.equals("derby") && !driver.isChallengeDone("derby")) {
                    driver.markChallengeDone("derby");
                    driver.earn(ch.money);
                    driver.addReputation(ch.rep);
                    o.challenges.add(ch);
                }
            }
        }
        return o;
    }

    /** Player chose to keep the retro car. */
    public void claimReward(Driver driver, Race race, Car car) {
        driver.getGarage().addCar(car);
        race.setRewardClaimed(true);
    }

    // ------------------------------------------------------------------
    // Saving and loading the career
    // ------------------------------------------------------------------
    private static String clean(String s) { return s.replaceAll("[;:,|=]", "_"); }

    /** Writes race results, cups and event records into a Properties object. */
    public void writeTo(Properties p) {
        for (int n = 1; n <= TOTAL_RACES; n++) {
            Race r = races[n - 1];
            p.setProperty("race." + n, r.getBestPlace() + ";" + r.getBestTime() + ";" + r.getBestMedal() + ";" + r.isRewardClaimed());
        }
        for (int t = 0; t < 5; t++) {
            p.setProperty("ko." + t, String.valueOf(knockoutEvents[t].getBestPlace()));
            p.setProperty("cp." + t, String.valueOf(checkpointEvents[t].getBestPlace()));
            p.setProperty("st." + t, String.valueOf(streetEvents[t].getBestPlace()));
            p.setProperty("po." + t, String.valueOf(policeEvents[t].getBestPlace()));
            Cup c = cups[t];
            p.setProperty("cup." + t, c.started + ";" + c.finished + ";" + c.roundsDone + ";" + c.finalPlace + ";" + c.timesWon + ";"
                    + c.playerPlaces[0] + "," + c.playerPlaces[1] + "," + c.playerPlaces[2] + "," + c.playerPlaces[3]);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : c.points.entrySet()) {
                sb.append(sb.length() > 0 ? ";" : "").append(clean(e.getKey())).append(":").append(e.getValue());
            }
            p.setProperty("cup." + t + ".points", sb.toString());
            p.setProperty("cup." + t + ".player", clean(c.getPlayerName()));
        }
    }

    private static int toInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (RuntimeException e) { return def; }
    }

    /** Puts saved results back. Call on a brand-new Career(). */
    public void readFrom(Properties p) {
        for (int n = 1; n <= TOTAL_RACES; n++) {
            String v = p.getProperty("race." + n);
            if (v == null) continue;
            String[] a = v.split(";");
            if (a.length < 4) continue;
            double time = 0;
            try { time = Double.parseDouble(a[1]); } catch (NumberFormatException e) { time = 0; }
            races[n - 1].restoreProgress(toInt(a[0], 0), time, toInt(a[2], 0), Boolean.parseBoolean(a[3]));
        }
        for (int t = 0; t < 5; t++) {
            knockoutEvents[t].restoreProgress(toInt(p.getProperty("ko." + t, "0"), 0), 0, 0, false);
            checkpointEvents[t].restoreProgress(toInt(p.getProperty("cp." + t, "0"), 0), 0, 0, false);
            streetEvents[t].restoreProgress(toInt(p.getProperty("st." + t, "0"), 0), 0, 0, false);
            policeEvents[t].restoreProgress(toInt(p.getProperty("po." + t, "0"), 0), 0, 0, false);
            String v = p.getProperty("cup." + t);
            if (v == null) continue;
            String[] a = v.split(";");
            if (a.length < 6) continue;
            Cup c = cups[t];
            c.restore(Boolean.parseBoolean(a[0]), Boolean.parseBoolean(a[1]), toInt(a[2], 0), toInt(a[3], 0), toInt(a[4], 0),
                    a[5].split(","), p.getProperty("cup." + t + ".player", ""), p.getProperty("cup." + t + ".points", ""));
        }
    }

    // ------------------------------------------------------------------
    // Race HQ interior pixel art (240 x 160 grid)
    // ------------------------------------------------------------------
    public static void paintHeadquarters(Graphics2D g) {
        // dark blue office wall
        GameWindow.fill(g, new Color(34, 48, 92), 0, 0, 240, 100);
        GameWindow.fill(g, new Color(26, 38, 76), 0, 84, 240, 16);
        // big leaderboard screen
        GameWindow.fill(g, new Color(20, 20, 28), 70, 12, 100, 60);
        GameWindow.fill(g, new Color(10, 24, 44), 73, 15, 94, 54);
        Color[] bars = { new Color(240, 200, 50), new Color(190, 195, 205), new Color(205, 130, 60),
                         new Color(70, 170, 240), new Color(90, 200, 120) };
        for (int i = 0; i < 5; i++) {
            GameWindow.fill(g, bars[i], 78, 20 + i * 10, 14, 6);
            GameWindow.fill(g, bars[i], 96, 20 + i * 10, 60 - i * 8, 6);
        }
        // trophy shelf (left)
        GameWindow.fill(g, new Color(110, 78, 46), 8, 56, 52, 4);
        for (int i = 0; i < 3; i++) {
            int x = 14 + i * 16;
            GameWindow.fill(g, new Color(240, 200, 50), x, 40, 10, 8);
            GameWindow.fill(g, new Color(240, 200, 50), x + 4, 48, 2, 6);
            GameWindow.fill(g, new Color(240, 200, 50), x + 1, 54, 8, 2);
            GameWindow.fill(g, new Color(255, 235, 140), x + 2, 41, 2, 5);
        }
        // checkered flags (right)
        for (int f = 0; f < 2; f++) {
            int x = 184 + f * 24;
            GameWindow.fill(g, new Color(180, 180, 190), x, 14, 2, 60);
            for (int yy = 0; yy < 4; yy++) {
                for (int xx = 0; xx < 4; xx++) {
                    boolean dark = (xx + yy) % 2 == 0;
                    GameWindow.fill(g, dark ? Color.BLACK : Color.WHITE, x + 2 + xx * 4, 14 + yy * 4, 4, 4);
                }
            }
        }
        // floor
        GameWindow.fill(g, new Color(78, 82, 96), 0, 100, 240, 60);
        for (int y = 100; y < 160; y += 10) {
            for (int x = ((y / 10) % 2) * 10; x < 240; x += 20) GameWindow.fill(g, new Color(70, 74, 88), x, y, 10, 10);
        }
        GameWindow.fill(g, new Color(20, 28, 56), 0, 100, 240, 2);
        // desk
        GameWindow.fill(g, new Color(120, 84, 52), 10, 104, 90, 20);
        GameWindow.fill(g, new Color(150, 108, 68), 10, 104, 90, 3);
        GameWindow.fill(g, new Color(30, 32, 40), 24, 92, 24, 14);
        GameWindow.fill(g, new Color(90, 190, 240), 26, 94, 20, 10);
        GameWindow.fill(g, new Color(30, 32, 40), 34, 106, 4, 3);
        // ceiling lights
        for (int x = 20; x < 240; x += 70) {
            GameWindow.fill(g, new Color(230, 235, 255), x, 0, 30, 3);
            g.setColor(new Color(220, 230, 255, 30));
            g.fillRect(x - 6, 3, 42, 18);
        }
    }
}
