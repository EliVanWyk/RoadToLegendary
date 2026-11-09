package racing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Save files: there are 3 SAVE SLOTS. Each slot is one small text file in
 * [your home folder]/.road_to_legendary/saves/ (slot1.properties, slot2..., slot3...).
 * The game saves into the slot you are playing by itself whenever you move between
 * screens and when you close the window.
 */
final class SaveGame {

    static final int SLOTS = 3;

    private SaveGame() { }

    /** A short summary of one slot (shown on the slot cards). */
    static class Info {
        String name;
        int rank;            // 0..4
        String rankName;
        int fame;            // "points" on the card
        int money;
        int won;
        Car car;             // the car the player is driving
        long time;
    }

    static File dir() {
        return new File(new File(System.getProperty("user.home", "."), ".road_to_legendary"), "saves");
    }

    static File slotFile(int slot) { return new File(dir(), "slot" + slot + ".properties"); }

    static boolean exists(int slot) { return info(slot) != null; }

    /** Saves the whole game into a slot. Returns false if the file could not be written. */
    static boolean save(int slot, Career career, Driver driver) {
        if (career == null || driver == null || slot < 1 || slot > SLOTS) return false;
        try {
            Properties p = new Properties();
            p.setProperty("version", "2");
            p.setProperty("savedAt", String.valueOf(System.currentTimeMillis()));
            p.setProperty("won", String.valueOf(career.getTotalWon()));
            driver.writeTo(p);
            career.writeTo(p);
            File f = slotFile(slot);
            f.getParentFile().mkdirs();
            File tmp = new File(f.getPath() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            try { p.store(out, "Road to Legendary save slot " + slot); } finally { out.close(); }
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Reads a slot. Returns null if it is empty (or broken). */
    static Properties read(int slot) {
        File f = slotFile(slot);
        if (!f.exists()) return null;
        try {
            Properties p = new Properties();
            FileInputStream in = new FileInputStream(f);
            try { p.load(in); } finally { in.close(); }
            return p.getProperty("name") == null ? null : p;
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    static void delete(int slot) {
        try {
            slotFile(slot).delete();
        } catch (RuntimeException e) {
            // ignore
        }
    }

    private static int num(Properties p, String key) {
        try { return Integer.parseInt(p.getProperty(key, "0").trim()); } catch (NumberFormatException e) { return 0; }
    }

    /** What the slot card shows, or null for an empty slot. */
    static Info info(int slot) {
        Properties p = read(slot);
        if (p == null) return null;
        Info i = new Info();
        i.name = p.getProperty("name");
        i.rank = Math.max(0, Math.min(4, num(p, "rank")));
        i.rankName = Career.RANK_NAMES[i.rank];
        i.fame = num(p, "reputation");
        i.money = num(p, "money");
        i.won = num(p, "won");
        try { i.time = Long.parseLong(p.getProperty("savedAt", "0").trim()); } catch (NumberFormatException e) { i.time = 0; }
        int cur = num(p, "garage.current");
        i.car = CarShop.deserialize(p.getProperty("garage.car" + cur, ""));
        if (i.car == null) i.car = CarShop.createStarterCar();
        return i;
    }
}
