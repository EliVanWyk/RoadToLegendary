package racing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * The player's profile: name, money, rank, reputation, driving skills,
 * statistics and the Garage.
 * Everything lives only while the program is running (there is no save file).
 */
public class Driver {

    // ---- reputation levels ----
    public static final String[] REP_NAMES = {
        "Nobody", "Rookie", "Local", "Known Racer", "Rising Star", "Fan Favorite", "Pro", "Star", "Icon", "Legend"
    };
    public static final int[] REP_LEVELS = { 0, 40, 120, 250, 450, 750, 1150, 1700, 2400, 3300 };

    // ---- driving skills (trained at the Practice Garage) ----
    public static final int SKILL_DRIFT = 0, SKILL_NITRO = 1, SKILL_DRAFT = 2;
    public static final String[] SKILL_NAMES = { "DRIFT CONTROL", "NITRO MASTERY", "SLIPSTREAM" };
    public static final String[] SKILL_INFO = {
        "Drift boost fills faster and lasts longer",
        "Nitro lasts longer and recharges sooner",
        "Slipstream gives a bigger speed boost"
    };
    public static final int MAX_SKILL = 5;
    private static final int[] SKILL_COSTS = { 2, 3, 4, 5, 6 };

    private final String name;
    private int money = 0;                 // start with $0
    private int rank = 0;                  // 0 = Beginner ... 4 = Legendary
    private int wins = 0;
    private int losses = 0;
    private int racesCompleted = 0;
    private int moneyEarned = 0;
    private int carsPurchased = 0;
    private int carsRepaired = 0;
    private int reputation = 0;
    private int winStreak = 0;
    private int skillPointsEarned = 0;
    private int skillPointsSpent = 0;
    private final int[] skills = new int[3];
    private int streetWins = 0, knockoutWins = 0, checkpointWins = 0, cupsWon = 0, goldMedals = 0, policeWins = 0, derbyWins = 0, derbyRuns = 0;
    private final Set<String> doneChallenges = new HashSet<String>();
    private final Garage garage = new Garage();

    public Driver(String name) {
        this.name = name;
        // every new racer starts with the basic Toyota Corolla
        garage.addCar(CarShop.createStarterCar());
    }

    // ---- getters ----
    public String getName() { return name; }
    public int getMoney() { return money; }
    public int getRank() { return rank; }
    public String getRankName() { return Career.RANK_NAMES[rank]; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getRacesCompleted() { return racesCompleted; }
    public int getMoneyEarned() { return moneyEarned; }
    public int getCarsPurchased() { return carsPurchased; }
    public int getCarsRepaired() { return carsRepaired; }
    public Garage getGarage() { return garage; }
    public Car getCurrentCar() { return garage.getCurrentCar(); }
    public int getCarsOwned() { return garage.size(); }
    public int getWinStreak() { return winStreak; }
    public int getStreetWins() { return streetWins; }
    public int getKnockoutWins() { return knockoutWins; }
    public int getCheckpointWins() { return checkpointWins; }
    public int getCupsWon() { return cupsWon; }
    public int getGoldMedals() { return goldMedals; }
    public int getPoliceWins() { return policeWins; }
    public int getDerbyWins() { return derbyWins; }
    public int getDerbyRuns() { return derbyRuns; }

    public void setRank(int rank) { this.rank = Math.max(0, Math.min(Career.RANK_NAMES.length - 1, rank)); }

    // ---- money ----
    public boolean canAfford(int amount) { return money >= amount; }

    /** Takes money if the driver has enough. Returns true if it worked. */
    public boolean spend(int amount) {
        if (amount > money) return false;
        money -= amount;
        return true;
    }

    /** Money from selling something (does not count as race winnings). */
    public void receiveMoney(int amount) { money += amount; }

    /** Money that counts as earnings (prizes, sponsors, bets won). */
    public void earn(int amount) {
        money += amount;
        moneyEarned += amount;
    }

    // ---- statistics ----
    public void recordRace(boolean won, int prize) {
        racesCompleted++;
        if (won) { wins++; winStreak++; } else { losses++; winStreak = 0; }
        earn(prize);
    }

    public void recordPurchase() { carsPurchased++; }
    public void recordRepair() { carsRepaired++; }
    public void recordStreetWin() { streetWins++; }
    public void recordKnockoutWin() { knockoutWins++; }
    public void recordCheckpointWin() { checkpointWins++; }
    public void recordCupWon() { cupsWon++; }
    public void recordGold() { goldMedals++; }
    public void recordPoliceWin() { policeWins++; }
    public void recordDerby(boolean won) { derbyRuns++; if (won) derbyWins++; }

    // ---- reputation ----
    public int getReputation() { return reputation; }
    public void addReputation(int amount) { reputation += Math.max(0, amount); }

    public int getRepLevel() {
        int lvl = 0;
        for (int i = 0; i < REP_LEVELS.length; i++) if (reputation >= REP_LEVELS[i]) lvl = i;
        return lvl;
    }

    public String getRepName() { return REP_NAMES[getRepLevel()]; }

    /** Reputation needed for the next level, or -1 at the top. */
    public int getRepForNext() {
        int l = getRepLevel();
        return l + 1 < REP_LEVELS.length ? REP_LEVELS[l + 1] : -1;
    }

    // ---- driving skills ----
    public int getSkill(int i) { return skills[i]; }
    public int getSkillPoints() { return skillPointsEarned - skillPointsSpent; }
    public int getSkillPointsEarned() { return skillPointsEarned; }
    public void addSkillPoints(int n) { skillPointsEarned += Math.max(0, n); }

    /** Skill points needed for the next level of a skill, or -1 if it is maxed. */
    public int getSkillCost(int i) { return skills[i] >= MAX_SKILL ? -1 : SKILL_COSTS[skills[i]]; }

    public boolean upgradeSkill(int i) {
        int cost = getSkillCost(i);
        if (cost < 0 || getSkillPoints() < cost) return false;
        skillPointsSpent += cost;
        skills[i]++;
        return true;
    }

    // ---- sponsor challenges ----
    public boolean isChallengeDone(String id) { return doneChallenges.contains(id); }
    public void markChallengeDone(String id) { doneChallenges.add(id); }
    public int getChallengesDone() { return doneChallenges.size(); }

    // ------------------------------------------------------------------
    // Saving and loading
    // ------------------------------------------------------------------
    /** Writes everything about the driver (and the garage) into a Properties object. */
    public void writeTo(Properties p) {
        p.setProperty("name", name);
        p.setProperty("money", String.valueOf(money));
        p.setProperty("rank", String.valueOf(rank));
        p.setProperty("wins", String.valueOf(wins));
        p.setProperty("losses", String.valueOf(losses));
        p.setProperty("racesCompleted", String.valueOf(racesCompleted));
        p.setProperty("moneyEarned", String.valueOf(moneyEarned));
        p.setProperty("carsPurchased", String.valueOf(carsPurchased));
        p.setProperty("carsRepaired", String.valueOf(carsRepaired));
        p.setProperty("reputation", String.valueOf(reputation));
        p.setProperty("winStreak", String.valueOf(winStreak));
        p.setProperty("skillPointsEarned", String.valueOf(skillPointsEarned));
        p.setProperty("skillPointsSpent", String.valueOf(skillPointsSpent));
        p.setProperty("skills", skills[0] + "," + skills[1] + "," + skills[2]);
        p.setProperty("streetWins", String.valueOf(streetWins));
        p.setProperty("knockoutWins", String.valueOf(knockoutWins));
        p.setProperty("checkpointWins", String.valueOf(checkpointWins));
        p.setProperty("cupsWon", String.valueOf(cupsWon));
        p.setProperty("goldMedals", String.valueOf(goldMedals));
        p.setProperty("policeWins", String.valueOf(policeWins));
        p.setProperty("derbyWins", String.valueOf(derbyWins));
        p.setProperty("derbyRuns", String.valueOf(derbyRuns));
        StringBuilder sb = new StringBuilder();
        for (String c : doneChallenges) sb.append(sb.length() > 0 ? "," : "").append(c);
        p.setProperty("challenges", sb.toString());
        p.setProperty("garage.count", String.valueOf(garage.size()));
        int cur = 0;
        for (int i = 0; i < garage.size(); i++) {
            Car c = garage.getCars().get(i);
            if (c == garage.getCurrentCar()) cur = i;
            p.setProperty("garage.car" + i, c.serialize());
        }
        p.setProperty("garage.current", String.valueOf(cur));
    }

    private static int num(Properties p, String key) {
        try {
            return Integer.parseInt(p.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Builds a driver from a saved game. */
    public static Driver readFrom(Properties p) {
        Driver d = new Driver(p.getProperty("name", "Racer"));
        d.money = num(p, "money");
        d.rank = Math.max(0, Math.min(Career.RANK_NAMES.length - 1, num(p, "rank")));
        d.wins = num(p, "wins");
        d.losses = num(p, "losses");
        d.racesCompleted = num(p, "racesCompleted");
        d.moneyEarned = num(p, "moneyEarned");
        d.carsPurchased = num(p, "carsPurchased");
        d.carsRepaired = num(p, "carsRepaired");
        d.reputation = num(p, "reputation");
        d.winStreak = num(p, "winStreak");
        d.skillPointsEarned = num(p, "skillPointsEarned");
        d.skillPointsSpent = num(p, "skillPointsSpent");
        String[] sk = p.getProperty("skills", "0,0,0").split(",");
        for (int i = 0; i < 3 && i < sk.length; i++) {
            try { d.skills[i] = Math.max(0, Math.min(MAX_SKILL, Integer.parseInt(sk[i].trim()))); } catch (NumberFormatException e) { d.skills[i] = 0; }
        }
        d.streetWins = num(p, "streetWins");
        d.knockoutWins = num(p, "knockoutWins");
        d.checkpointWins = num(p, "checkpointWins");
        d.cupsWon = num(p, "cupsWon");
        d.goldMedals = num(p, "goldMedals");
        d.policeWins = num(p, "policeWins");
        d.derbyWins = num(p, "derbyWins");
        d.derbyRuns = num(p, "derbyRuns");
        for (String c : p.getProperty("challenges", "").split(",")) {
            if (!c.trim().isEmpty()) d.doneChallenges.add(c.trim());
        }
        int count = num(p, "garage.count");
        ArrayList<Car> loaded = new ArrayList<Car>();
        for (int i = 0; i < count; i++) {
            Car c = CarShop.deserialize(p.getProperty("garage.car" + i, ""));
            if (c != null) loaded.add(c);
        }
        if (!loaded.isEmpty()) {
            d.garage.clear();
            for (Car c : loaded) d.garage.addCar(c);
            int cur = Math.max(0, Math.min(loaded.size() - 1, num(p, "garage.current")));
            d.garage.setCurrentCar(loaded.get(cur));
        }
        return d;
    }
}
