package racing;

import static racing.GameWindow.box;
import static racing.GameWindow.font;
import static racing.GameWindow.money;
import static racing.GameWindow.ordinal;
import static racing.GameWindow.text;
import static racing.GameWindow.textCenter;
import static racing.GameWindow.textRight;
import static racing.GameWindow.wrap;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

/*
 * The screens of the new buildings. They all follow the same recipe as the
 * original Race HQ screen: a list on the right, information on the left and
 * a big START button at the bottom.
 */

/** Small helpers shared by these screens. */
final class ScreenUtil {
    private ScreenUtil() { }

    static String shorten(String s, int max) { return s.length() > max ? s.substring(0, max - 1) + "." : s; }

    static String time(double t) {
        int m = (int) (t / 60);
        return String.format("%d:%04.1f", m, t - m * 60);
    }

    static void highlight(Graphics2D g, Rectangle r) {
        g.setColor(new Color(255, 240, 120));
        for (int t = 0; t < 3; t++) g.drawRect(r.x - 3 + t, r.y - 3 + t, r.width + 6 - 2 * t - 1, r.height + 6 - 2 * t - 1);
    }

    static void drawWrapped(Graphics2D g, String s, int x, int y, int width, int size, Color c, int lineHeight) {
        FontMetrics fm = g.getFontMetrics(font(size));
        for (String line : wrap(s, fm, width)) {
            text(g, line, x, y, size, c, false);
            y += lineHeight;
        }
    }

    static void paintCarSummary(Graphics2D g, Car c, int x, int y) {
        text(g, "YOUR CAR", x, y, 16, new Color(255, 240, 170), true);
        g.drawImage(c.getLargeImage(), x, y + 8, 150, 62, null);
        text(g, c.getName(), x + 160, y + 34, 14, Color.WHITE, false);
        text(g, "Condition " + c.getCondition() + "%", x + 160, y + 56, 14,
                c.getCondition() > 60 ? new Color(120, 240, 130) : new Color(255, 150, 90), false);
    }
}

// ======================================================================
// CHAMPIONSHIP HALL
// ======================================================================
class ChampionshipScreen extends ScreenBase {
    private int viewTier = 0;
    private final GameWindow.PixelButton[] rows = new GameWindow.PixelButton[5];
    private final GameWindow.PixelButton startButton, restartButton;

    ChampionshipScreen(GameWindow window) {
        super(window, CityMap.CHAMPIONSHIP);
        for (int i = 0; i < 5; i++) {
            final int n = i;
            rows[i] = addButton("", new Color(60, 80, 140), 40, 130 + i * 62, 332, 52, () -> {
                viewTier = n;
                setStatus("");
            });
            rows[i].setFont(font(15));
        }
        startButton = addButton("START CUP", new Color(60, 170, 80), 400, 548, 190, 50, this::start);
        restartButton = addButton("RESTART CUP", new Color(210, 130, 40), 602, 548, 170, 50, this::restart);
        addBackButton(784, 548, 152, 50);
    }

    @Override
    public void onShow() {
        viewTier = Math.min(viewTier, driver().getRank());
        super.onShow();
    }

    private boolean open(int tier) { return tier <= driver().getRank(); }

    @Override
    protected void refresh() {
        for (int i = 0; i < 5; i++) {
            Career.Cup c = career().getCup(i);
            String s = !open(i) ? "LOCKED" : c.finished ? "DONE " + ordinal(c.finalPlace) : c.started ? "ROUND " + (c.roundsDone + 1) + "/4" : "NEW";
            rows[i].setText(String.format("%-13s %s", Career.CUP_NAMES[i], s));
            rows[i].setEnabled(open(i));
            rows[i].setBase(c.timesWon > 0 ? new Color(150, 120, 40) : c.started && !c.finished ? new Color(60, 120, 140) : new Color(60, 80, 140));
        }
        Career.Cup cup = career().getCup(viewTier);
        startButton.setText(!cup.started ? "START CUP" : cup.finished ? "NEW ATTEMPT" : "NEXT ROUND " + (cup.roundsDone + 1));
        startButton.setEnabled(open(viewTier));
        restartButton.setEnabled(cup.started && !cup.finished);
    }

    private void start() {
        if (!open(viewTier)) return;
        Career.Cup cup = career().getCup(viewTier);
        if (!cup.started || cup.finished) cup.start(driver().getName());
        if (!checkCar()) return;
        Race r = cup.getNextRace();
        if (r != null) window.startRace(r, 0, "b" + id);
    }

    private void restart() {
        if (window.confirm("RESTART CUP?", "All points of this cup are lost and you start again from round 1.", "RESTART", "KEEP GOING")) {
            career().getCup(viewTier).start(driver().getName());
            setStatus("Cup restarted. Good luck!");
        }
    }

    @Override
    protected void paintContent(Graphics2D g) {
        box(g, 24, 84, 364, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        text(g, "CHOOSE A CUP", 44, 116, 20, new Color(255, 240, 170), true);
        ScreenUtil.highlight(g, rows[viewTier].getBounds());
        text(g, "Cups won: " + driver().getCupsWon(), 44, 466, 15, new Color(255, 220, 100), true);
        ScreenUtil.drawWrapped(g, "4 rounds, same 5 rivals. Points: 25-18-15-12-10-8. Higher cups open as you rank up.",
                44, 486, 330, 12, new Color(190, 200, 230), 16);

        box(g, 400, 84, 536, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        Career.Cup cup = career().getCup(viewTier);
        textCenter(g, Career.CUP_NAMES[viewTier], 668, 118, 24, new Color(255, 240, 170));
        int[] p = Career.PRIZES[viewTier];
        textCenter(g, "Cup prize: 1st " + money(p[0] * 5) + "   2nd " + money(p[1] * 3) + "   3rd " + money(p[2] * 2), 668, 142, 13, new Color(120, 240, 130));

        for (int k = 0; k < 4; k++) {
            int y = 176 + k * 28;
            String name = cup.rounds[k].getName();
            int cut = name.indexOf(": ");
            if (cut >= 0) name = name.substring(cut + 2);
            boolean done = cup.started && k < cup.roundsDone;
            boolean next = cup.started && !cup.finished && k == cup.roundsDone;
            if (next) g.setColor(new Color(255, 255, 255, 30));
            if (next) g.fillRect(412, y - 18, 512, 24);
            text(g, "R" + (k + 1) + "  " + ScreenUtil.shorten(name, 26), 424, y, 15, next ? new Color(255, 240, 150) : Color.WHITE, false);
            String right = done ? ordinal(cup.playerPlaces[k]) + " PLACE" : next ? "NEXT" : "-";
            textRight(g, right, 910, y, 15, done ? new Color(150, 200, 255) : next ? new Color(120, 240, 130) : new Color(130, 135, 155));
        }

        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(412, 296, 512, 1);
        if (cup.started) {
            text(g, cup.finished ? "FINAL STANDINGS" : "STANDINGS", 424, 322, 16, new Color(255, 240, 170), true);
            List<Map.Entry<String, Integer>> table = cup.getTable();
            for (int i = 0; i < table.size(); i++) {
                int y = 350 + i * 28;
                boolean me = table.get(i).getKey().equals(cup.getPlayerName());
                if (me) {
                    g.setColor(new Color(255, 255, 255, 30));
                    g.fillRect(412, y - 18, 512, 24);
                }
                Color c = me ? new Color(255, 240, 150) : Color.WHITE;
                text(g, (i + 1) + ".", 424, y, 16, c, false);
                text(g, ScreenUtil.shorten(table.get(i).getKey(), 16), 462, y, 16, c, false);
                textRight(g, table.get(i).getValue() + " pts", 910, y, 16, c);
            }
        } else {
            text(g, "YOUR RIVALS", 424, 322, 16, new Color(255, 240, 170), true);
            for (int i = 0; i < cup.field.size(); i++) {
                Opponent o = cup.field.get(i);
                text(g, ScreenUtil.shorten(o.getName() + " (" + o.getCar().getName() + ")", 44), 424, 352 + i * 24, 14, Color.WHITE, false);
            }
        }
    }
}

// ======================================================================
// PRACTICE GARAGE (time trials + driving skills)
// ======================================================================
class PracticeScreen extends ScreenBase {
    private int viewRank;
    private Race selected;
    private final GameWindow.PixelButton[] rows = new GameWindow.PixelButton[5];
    private final GameWindow.PixelButton[] upgrades = new GameWindow.PixelButton[3];
    private final GameWindow.PixelButton startButton, prevRank, nextRank;
    private static final String[] MEDALS = { "NO MEDAL", "BRONZE", "SILVER", "GOLD" };

    PracticeScreen(GameWindow window) {
        super(window, CityMap.PRACTICE);
        prevRank = addButton("<", new Color(70, 90, 160), 416, 92, 44, 34, () -> changeRank(-1));
        nextRank = addButton(">", new Color(70, 90, 160), 884, 92, 44, 34, () -> changeRank(1));
        for (int i = 0; i < 5; i++) {
            final int n = i;
            rows[i] = addButton("", new Color(60, 80, 140), 416, 134 + i * 50, 504, 44, () -> {
                selected = career().getRaceInRank(viewRank, n);
                setStatus("");
            });
            rows[i].setFont(font(15));
        }
        for (int i = 0; i < 3; i++) {
            final int s = i;
            upgrades[i] = addButton("UPGRADE", new Color(60, 150, 90), 44, 214 + i * 108, 324, 30, () -> upgrade(s));
        }
        startButton = addButton("START TIME TRIAL", new Color(60, 170, 80), 400, 548, 260, 50, this::start);
        addBackButton(676, 548, 260, 50);
    }

    @Override
    public void onShow() {
        viewRank = driver().getRank();
        for (int rk = driver().getRank(); rk >= 0; rk--) {
            if (career().getCompletedInRank(rk) > 0) { viewRank = rk; break; }
        }
        selected = career().getRaceInRank(viewRank, 0);
        for (int i = 4; i >= 0; i--) {
            Race r = career().getRaceInRank(viewRank, i);
            if (r.isCompleted()) { selected = r; break; }
        }
        super.onShow();
    }

    private void changeRank(int d) {
        viewRank = Math.max(0, Math.min(4, viewRank + d));
        selected = career().getRaceInRank(viewRank, 0);
        setStatus("");
    }

    private void upgrade(int s) {
        Driver d = driver();
        int cost = d.getSkillCost(s);
        if (cost < 0) { setStatus("That skill is already at the maximum level."); return; }
        if (d.getSkillPoints() < cost) {
            Sound.play(Sound.ERROR);
            setStatus("You need " + cost + " skill points. Win medals in time trials to earn more!");
            return;
        }
        d.upgradeSkill(s);
        Sound.play(Sound.BUY);
        setStatus(Driver.SKILL_NAMES[s] + " is now level " + d.getSkill(s) + "!");
    }

    @Override
    protected void refresh() {
        Driver d = driver();
        prevRank.setEnabled(viewRank > 0);
        nextRank.setEnabled(viewRank < 4);
        for (int i = 0; i < 5; i++) {
            Race r = career().getRaceInRank(viewRank, i);
            boolean open = r.isCompleted();
            String result = !open ? "LOCKED" : r.getBestMedal() > 0 ? MEDALS[r.getBestMedal()] + " " + ScreenUtil.time(r.getBestTime())
                    : r.getBestTime() > 0 ? "NO MEDAL" : "NEW";
            rows[i].setText(String.format("%02d %-22s %s", r.getNumber(), ScreenUtil.shorten(r.getName(), 22), result));
            rows[i].setEnabled(open);
            rows[i].setBase(r.getBestMedal() == 3 ? new Color(150, 120, 40) : r.getBestMedal() > 0 ? new Color(60, 140, 90) : new Color(60, 80, 140));
        }
        for (int i = 0; i < 3; i++) {
            int cost = d.getSkillCost(i);
            upgrades[i].setText(cost < 0 ? "MAX LEVEL" : "UPGRADE  -  " + cost + " SKILL POINTS");
            upgrades[i].setEnabled(cost >= 0 && d.getSkillPoints() >= cost);
        }
        startButton.setEnabled(selected != null && selected.isCompleted());
    }

    private void start() {
        if (selected == null || !selected.isCompleted()) return;
        if (!checkCar()) return;
        Race trial = selected.makeVariant(Race.MODE_TRIAL);
        window.startRace(trial, 0, "b" + id);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        Driver d = driver();
        box(g, 24, 84, 364, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        text(g, "DRIVING SKILLS", 44, 116, 20, new Color(255, 240, 170), true);
        textRight(g, "Points: " + d.getSkillPoints(), 368, 116, 16, new Color(120, 240, 130));
        for (int i = 0; i < 3; i++) {
            int y0 = 140 + i * 108;
            text(g, Driver.SKILL_NAMES[i], 44, y0 + 20, 17, Color.WHITE, true);
            textRight(g, "LV " + d.getSkill(i) + "/" + Driver.MAX_SKILL, 368, y0 + 20, 15, new Color(190, 200, 230));
            for (int k = 0; k < Driver.MAX_SKILL; k++) {
                g.setColor(Color.BLACK);
                g.fillRect(44 + k * 66, y0 + 30, 62, 14);
                g.setColor(k < d.getSkill(i) ? new Color(90, 220, 255) : new Color(50, 54, 80));
                g.fillRect(46 + k * 66, y0 + 32, 58, 10);
            }
            text(g, Driver.SKILL_INFO[i], 44, y0 + 62, 12, new Color(190, 200, 230), false);
        }
        ScreenUtil.drawWrapped(g, "Earn skill points with medals: bronze 1, silver +1, gold +2 (first time on each race).",
                44, 476, 330, 12, new Color(190, 200, 230), 16);
        text(g, "Medals: " + career().getTotalMedals() + "/75", 44, 520, 14, new Color(255, 220, 100), true);

        box(g, 400, 84, 536, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        textCenter(g, Career.RANK_NAMES[viewRank].toUpperCase() + "  TIME TRIALS", 672, 116, 16, new Color(255, 240, 170));
        if (selected != null) {
            ScreenUtil.highlight(g, rows[selected.getIndexInRank()].getBounds());
            box(g, 412, 388, 512, 138, new Color(12, 16, 36), new Color(90, 120, 200));
            text(g, "#" + selected.getNumber() + "  " + selected.getName(), 428, 414, 16, new Color(255, 240, 170), true);
            if (!selected.isCompleted()) {
                text(g, "Finish this race in the Race Track first.", 428, 446, 14, new Color(255, 190, 120), false);
            } else {
                text(g, "Track: " + selected.getThemeName() + (selected.isNight() ? " (night)" : ""), 428, 436, 13, Color.WHITE, false);
                text(g, "GOLD   " + ScreenUtil.time(selected.getGoldTime()), 428, 462, 15, new Color(255, 215, 70), false);
                text(g, "SILVER " + ScreenUtil.time(selected.getSilverTime()), 428, 484, 15, new Color(215, 220, 230), false);
                text(g, "BRONZE " + ScreenUtil.time(selected.getBronzeTime()), 428, 506, 15, new Color(205, 130, 70), false);
                text(g, "YOUR BEST", 700, 462, 13, new Color(190, 200, 230), false);
                text(g, selected.getBestTime() > 0 ? ScreenUtil.time(selected.getBestTime()) : "--", 700, 488, 24, Color.WHITE, true);
                text(g, MEDALS[selected.getBestMedal()], 700, 508, 14, new Color(255, 220, 100), false);
            }
        }
    }
}

// ======================================================================
// KNOCKOUT ARENA (knockout + checkpoint rush)
// ======================================================================
class KnockoutScreen extends ScreenBase {
    private int eventMode = 0;          // 0 = knockout, 1 = checkpoint rush
    private int tier = 0;
    private final GameWindow.PixelButton[] rows = new GameWindow.PixelButton[5];
    private final GameWindow.PixelButton startButton, tabKnock, tabRush;

    KnockoutScreen(GameWindow window) {
        super(window, CityMap.KNOCKOUT);
        tabKnock = addButton("KNOCKOUT", new Color(190, 60, 60), 416, 92, 250, 34, () -> { eventMode = 0; setStatus(""); });
        tabRush = addButton("CHECKPOINT RUSH", new Color(60, 150, 100), 674, 92, 250, 34, () -> { eventMode = 1; setStatus(""); });
        for (int i = 0; i < 5; i++) {
            final int n = i;
            rows[i] = addButton("", new Color(60, 80, 140), 416, 134 + i * 50, 504, 44, () -> {
                tier = n;
                setStatus("");
            });
            rows[i].setFont(font(15));
        }
        startButton = addButton("START EVENT", new Color(60, 170, 80), 400, 548, 260, 50, this::start);
        addBackButton(676, 548, 260, 50);
    }

    @Override
    public void onShow() {
        tier = driver().getRank();
        super.onShow();
    }

    private Race event(int t) { return eventMode == 0 ? career().getKnockoutEvent(t) : career().getCheckpointEvent(t); }

    private boolean open(int t) { return t <= driver().getRank(); }

    @Override
    protected void refresh() {
        for (int i = 0; i < 5; i++) {
            Race r = event(i);
            String result = !open(i) ? "LOCKED" : r.isCompleted() ? "BEST " + ordinal(r.getBestPlace()) : "NEW";
            rows[i].setText(String.format("%-13s %-17s %s", Career.RANK_NAMES[i].toUpperCase(), ScreenUtil.shorten(r.getName(), 17), result));
            rows[i].setEnabled(open(i));
            rows[i].setBase(r.isWon() ? new Color(60, 140, 90) : new Color(60, 80, 140));
        }
        startButton.setEnabled(open(tier));
        tabKnock.setBase(eventMode == 0 ? new Color(210, 70, 70) : new Color(90, 60, 70));
        tabRush.setBase(eventMode == 1 ? new Color(60, 170, 110) : new Color(50, 80, 70));
    }

    private void start() {
        if (!open(tier)) return;
        if (!checkCar()) return;
        window.startRace(event(tier), 0, "b" + id);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        box(g, 24, 84, 364, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        text(g, eventMode == 0 ? "KNOCKOUT" : "CHECKPOINT RUSH", 44, 120, 22, new Color(255, 240, 170), true);
        String rules = eventMode == 0
                ? "Every few seconds the car in LAST PLACE is knocked out. Stay ahead of the pack and be the last driver standing!\n\nIf you are last when the timer hits zero, you're out."
                : "Drive through the glowing checkpoints before time runs out. Each gate adds bonus seconds.\n\nReach the final gate first to win. Run out of time and you lose.";
        ScreenUtil.drawWrapped(g, rules, 44, 152, 330, 14, Color.WHITE, 20);
        ScreenUtil.paintCarSummary(g, driver().getCurrentCar(), 44, 390);
        text(g, "Wins: " + (eventMode == 0 ? driver().getKnockoutWins() : driver().getCheckpointWins()), 44, 500, 16, new Color(120, 240, 130), true);

        box(g, 400, 84, 536, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        ScreenUtil.highlight(g, rows[tier].getBounds());
        Race r = event(tier);
        box(g, 412, 388, 512, 138, new Color(12, 16, 36), new Color(90, 120, 200));
        text(g, r.getName(), 428, 414, 16, new Color(255, 240, 170), true);
        text(g, "Track: " + r.getThemeName() + (r.isNight() ? " (night)" : ""), 428, 438, 13, Color.WHITE, false);
        if (eventMode == 0) text(g, "Knockout every " + (int) (15 - tier) + " seconds", 428, 458, 13, Color.WHITE, false);
        else text(g, r.getCheckpointCount() + " checkpoints, " + String.format("%,d", (int) r.getLength()) + " m", 428, 458, 13, Color.WHITE, false);
        text(g, "1st " + money(r.getPrize(1)) + "  2nd " + money(r.getPrize(2)) + "  3rd " + money(r.getPrize(3)), 428, 480, 13, new Color(120, 240, 130), false);
        text(g, "Rivals", 700, 414, 13, new Color(190, 200, 230), false);
        for (int i = 0; i < r.getOpponents().size() && i < 5; i++) {
            text(g, ScreenUtil.shorten(r.getOpponents().get(i).getName(), 22), 700, 434 + i * 17, 11, Color.WHITE, false);
        }
    }
}

// ======================================================================
// STREET RACING SPOT
// ======================================================================
class StreetScreen extends ScreenBase {
    private static final int[] BETS = { 0, 100, 250, 500, 1000, 2500, 5000, 10000 };
    private int tier = 0;
    private int betIndex = 0;
    private final GameWindow.PixelButton[] rows = new GameWindow.PixelButton[5];
    private final GameWindow.PixelButton startButton, betDown, betUp;

    StreetScreen(GameWindow window) {
        super(window, CityMap.STREET);
        betDown = addButton("-", new Color(70, 90, 160), 44, 296, 60, 40, () -> changeBet(-1));
        betUp = addButton("+", new Color(70, 90, 160), 308, 296, 60, 40, () -> changeBet(1));
        for (int i = 0; i < 5; i++) {
            final int n = i;
            rows[i] = addButton("", new Color(60, 80, 140), 416, 100 + i * 56, 504, 48, () -> {
                tier = n;
                setStatus("");
            });
            rows[i].setFont(font(15));
        }
        startButton = addButton("START STREET RACE", new Color(60, 170, 80), 400, 548, 260, 50, this::start);
        addBackButton(676, 548, 260, 50);
    }

    @Override
    public void onShow() {
        tier = driver().getRank();
        betIndex = 0;
        super.onShow();
    }

    private boolean open(int t) { return t <= driver().getRank(); }

    private void changeBet(int d) {
        int idx = betIndex + d;
        if (idx < 0 || idx >= BETS.length) return;
        if (BETS[idx] > driver().getMoney()) {
            setStatus("You don't have enough money for that bet.");
            return;
        }
        betIndex = idx;
        setStatus("");
    }

    @Override
    protected void refresh() {
        while (betIndex > 0 && BETS[betIndex] > driver().getMoney()) betIndex--;
        Career career = career();
        for (int i = 0; i < 5; i++) {
            Race r = career.getStreetEvent(i);
            String result = !open(i) ? "LOCKED" : r.isCompleted() ? "BEST " + ordinal(r.getBestPlace()) : "NEW";
            rows[i].setText(String.format("%-13s %-18s %s", Career.RANK_NAMES[i].toUpperCase(), ScreenUtil.shorten(r.getName(), 18), result));
            rows[i].setEnabled(open(i));
            rows[i].setBase(r.isWon() ? new Color(60, 140, 90) : new Color(90, 60, 130));
        }
        betDown.setEnabled(betIndex > 0);
        betUp.setEnabled(betIndex + 1 < BETS.length && BETS[betIndex + 1] <= driver().getMoney());
        startButton.setEnabled(open(tier));
    }

    private void start() {
        if (!open(tier)) return;
        int bet = BETS[betIndex];
        if (bet > driver().getMoney()) { setStatus("You can't afford that bet."); return; }
        if (!checkCar()) return;
        if (bet > 0 && !window.confirm("PLACE YOUR BET", "You are betting " + money(bet)
                + ".\nWin = you get " + money(bet * 2) + " extra.\n2nd = you keep your money.\n3rd or worse = you lose " + money(bet) + ".", "LET'S RACE", "NO WAY")) return;
        window.startRace(career().getStreetEvent(tier), bet, "b" + id);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        int bet = BETS[betIndex];
        box(g, 24, 84, 364, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        text(g, "YOUR BET", 44, 120, 22, new Color(255, 240, 170), true);
        textCenter(g, bet == 0 ? "NO BET" : money(bet), 206, 326, 28, bet == 0 ? Color.WHITE : new Color(120, 240, 130));
        text(g, "Win:  +" + money(bet * 2), 44, 376, 16, new Color(120, 240, 130), false);
        text(g, "2nd:  money back", 44, 400, 16, Color.WHITE, false);
        text(g, "Else: -" + money(bet), 44, 424, 16, new Color(255, 130, 110), false);
        ScreenUtil.drawWrapped(g, "Street races happen at night in busy traffic. Slow cars block the road, so weave around them!",
                44, 460, 330, 12, new Color(190, 200, 230), 16);
        text(g, "Street wins: " + driver().getStreetWins(), 44, 516, 15, new Color(120, 240, 130), true);
        ScreenUtil.paintCarSummary(g, driver().getCurrentCar(), 44, 170);

        box(g, 400, 84, 536, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        ScreenUtil.highlight(g, rows[tier].getBounds());
        Race r = career().getStreetEvent(tier);
        text(g, r.getName() + "  -  " + String.format("%,d", (int) r.getLength()) + " m, "
                + r.getOpponents().size() + " rivals + traffic", 416, 400, 14, Color.WHITE, false);
        text(g, "Prize money:  1st " + money(r.getPrize(1)) + "  2nd " + money(r.getPrize(2)) + "  3rd " + money(r.getPrize(3)), 416, 424, 13, new Color(120, 240, 130), false);
        text(g, "Street races earn extra fame!", 416, 448, 13, new Color(190, 170, 255), false);
        text(g, "Crashing into traffic damages your car.", 416, 470, 13, new Color(255, 190, 120), false);
    }
}

// ======================================================================
// SPONSOR BOARD (opened from Race HQ)
// ======================================================================
class SponsorScreen extends ScreenBase {
    private static final int PER_PAGE = 6;
    private int page = 0;
    private final GameWindow.PixelButton prev, next;

    SponsorScreen(GameWindow window) {
        super(window, CityMap.RACE_HQ);
        prev = addButton("<", new Color(70, 90, 160), 24, 548, 60, 50, () -> { page--; setStatus(""); });
        next = addButton(">", new Color(70, 90, 160), 96, 548, 60, 50, () -> { page++; setStatus(""); });
        addButton("BACK TO RACE HQ", new Color(190, 70, 60), 676, 548, 260, 50, () -> window.showBuilding(CityMap.RACE_HQ));
    }

    @Override
    protected String title() { return "SPONSOR BOARD"; }

    @Override
    public void onShow() {
        page = 0;
        super.onShow();
    }

    private int pages() { return (Career.CHALLENGES.length + PER_PAGE - 1) / PER_PAGE; }

    @Override
    protected void refresh() {
        prev.setEnabled(page > 0);
        next.setEnabled(page < pages() - 1);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        Driver d = driver();
        box(g, 24, 84, 912, 450, new Color(18, 22, 46, 236), new Color(120, 160, 255));
        text(g, "SPONSOR CHALLENGES", 44, 118, 22, new Color(255, 240, 170), true);
        textRight(g, "Done " + d.getChallengesDone() + "/" + Career.CHALLENGES.length + "     Page " + (page + 1) + "/" + pages(), 916, 118, 15, new Color(190, 200, 230));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= Career.CHALLENGES.length) break;
            Career.Challenge c = Career.CHALLENGES[idx];
            boolean done = d.isChallengeDone(c.id);
            int y = 136 + i * 64;
            box(g, 40, y, 880, 56, done ? new Color(30, 60, 44) : new Color(26, 30, 60), done ? new Color(90, 200, 120) : new Color(80, 100, 170));
            g.setColor(done ? new Color(90, 220, 120) : new Color(60, 64, 90));
            g.fillRect(56, y + 16, 24, 24);
            if (done) text(g, "OK", 58, y + 34, 13, Color.BLACK, false);
            text(g, c.title, 98, y + 24, 17, done ? new Color(150, 230, 170) : new Color(255, 240, 170), true);
            text(g, c.sponsor, 98, y + 44, 12, new Color(150, 165, 200), false);
            text(g, c.text, 330, y + 34, 14, Color.WHITE, false);
            textRight(g, done ? "COMPLETE" : money(c.money) + "  +" + c.rep + " fame", 904, y + 34, 14, done ? new Color(120, 240, 130) : new Color(255, 220, 100));
        }
    }
}

// ======================================================================
// POLICE CHASE
// ======================================================================
class PoliceScreen extends ScreenBase {
    private int tier = 0;
    private final GameWindow.PixelButton[] rows = new GameWindow.PixelButton[5];
    private final GameWindow.PixelButton startButton;

    PoliceScreen(GameWindow window) {
        super(window, CityMap.POLICE);
        for (int i = 0; i < 5; i++) {
            final int n = i;
            rows[i] = addButton("", new Color(60, 80, 140), 416, 100 + i * 56, 504, 48, () -> {
                tier = n;
                setStatus("");
            });
            rows[i].setFont(font(15));
        }
        startButton = addButton("START CHASE", new Color(60, 170, 80), 400, 548, 260, 50, this::start);
        addBackButton(676, 548, 260, 50);
    }

    @Override
    public void onShow() {
        tier = driver().getRank();
        super.onShow();
    }

    private boolean open(int t) { return t <= driver().getRank(); }

    @Override
    protected void refresh() {
        for (int i = 0; i < 5; i++) {
            Race r = career().getPoliceEvent(i);
            String result = !open(i) ? "LOCKED" : r.isWon() ? "ESCAPED" : r.isCompleted() ? "CAUGHT" : "NEW";
            rows[i].setText(String.format("%-13s %-18s %s", Career.RANK_NAMES[i].toUpperCase(), ScreenUtil.shorten(r.getName(), 18), result));
            rows[i].setEnabled(open(i));
            rows[i].setBase(r.isWon() ? new Color(60, 140, 90) : new Color(40, 70, 150));
        }
        startButton.setEnabled(open(tier));
    }

    private void start() {
        if (!open(tier)) return;
        if (!checkCar()) return;
        window.startRace(career().getPoliceEvent(tier), 0, "b" + id);
    }

    @Override
    protected void paintContent(Graphics2D g) {
        box(g, 24, 84, 364, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        text(g, "POLICE CHASE", 44, 120, 22, new Color(255, 240, 170), true);
        ScreenUtil.drawWrapped(g, "The cops are after you! Drive to the finish line before they catch you.\n\n"
                + "Police cars chase you and try to box you in. If they stay close or ram you, the BUSTED meter fills up. "
                + "When it is full you are caught and pay a fine.\n\nUse drift boost and nitro to leave them behind, and watch out for traffic!",
                44, 152, 330, 14, Color.WHITE, 20);
        ScreenUtil.paintCarSummary(g, driver().getCurrentCar(), 44, 410);
        text(g, "Escapes: " + driver().getPoliceWins(), 44, 516, 16, new Color(120, 240, 130), true);

        box(g, 400, 84, 536, 450, new Color(18, 22, 46, 232), new Color(120, 160, 255));
        ScreenUtil.highlight(g, rows[tier].getBounds());
        Race r = career().getPoliceEvent(tier);
        text(g, r.getName() + "  -  " + String.format("%,d", (int) r.getLength()) + " m", 416, 400, 14, Color.WHITE, false);
        text(g, "Escape prize: " + money(r.getPrize(1)) + "     Fine if caught: " + money(Career.PRIZES[tier][2]), 416, 424, 13, new Color(120, 240, 130), false);
        text(g, "Police cars: " + (2 + tier / 2) + " at the start, up to " + (3 + tier) + " later.", 416, 448, 13, new Color(150, 190, 255), false);
        text(g, "Police chases earn lots of fame!", 416, 470, 13, new Color(190, 170, 255), false);
        text(g, r.isNight() ? "It is a night chase." : "A daytime chase.", 416, 492, 13, new Color(190, 200, 230), false);
    }
}
