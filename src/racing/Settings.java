package racing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * All the options the player can change on the SETTINGS screen.
 * They are saved to a small file in the user's home folder, so they are
 * remembered the next time the game starts.
 */
public final class Settings {

    public static final int CONTROLS_BOTH = 0, CONTROLS_ARROWS = 1, CONTROLS_WASD = 2;
    public static final String[] CONTROL_NAMES = { "ARROWS + WASD", "ARROWS ONLY", "WASD ONLY" };
    public static final int EASY = 0, NORMAL = 1, HARD = 2;
    public static final String[] DIFFICULTY_NAMES = { "EASY", "NORMAL", "HARD" };

    public static boolean sfxOn = true;
    public static int sfxVolume = 80;          // 0..100
    public static boolean musicOn = true;
    public static int musicVolume = 60;        // 0..100
    public static boolean showMinimap = true;
    public static boolean showSpeedo = true;
    public static boolean kph = true;          // false = miles per hour
    public static int controls = CONTROLS_BOTH;
    public static int difficulty = NORMAL;
    public static boolean hints = true;
    public static boolean effects = true;      // skid marks, smoke, sparks, rain

    private Settings() { }

    private static File file() {
        return new File(new File(System.getProperty("user.home", "."), ".road_to_legendary"), "settings.properties");
    }

    public static void load() {
        try {
            File f = file();
            if (!f.exists()) return;
            Properties p = new Properties();
            FileInputStream in = new FileInputStream(f);
            try { p.load(in); } finally { in.close(); }
            sfxOn = bool(p, "sfxOn", sfxOn);
            sfxVolume = num(p, "sfxVolume", sfxVolume, 0, 100);
            musicOn = bool(p, "musicOn", musicOn);
            musicVolume = num(p, "musicVolume", musicVolume, 0, 100);
            showMinimap = bool(p, "showMinimap", showMinimap);
            showSpeedo = bool(p, "showSpeedo", showSpeedo);
            kph = bool(p, "kph", kph);
            controls = num(p, "controls", controls, 0, 2);
            difficulty = num(p, "difficulty", difficulty, 0, 2);
            hints = bool(p, "hints", hints);
            effects = bool(p, "effects", effects);
        } catch (Exception e) {
            // a broken settings file is simply ignored
        }
    }

    public static void save() {
        try {
            File f = file();
            f.getParentFile().mkdirs();
            Properties p = new Properties();
            p.setProperty("sfxOn", String.valueOf(sfxOn));
            p.setProperty("sfxVolume", String.valueOf(sfxVolume));
            p.setProperty("musicOn", String.valueOf(musicOn));
            p.setProperty("musicVolume", String.valueOf(musicVolume));
            p.setProperty("showMinimap", String.valueOf(showMinimap));
            p.setProperty("showSpeedo", String.valueOf(showSpeedo));
            p.setProperty("kph", String.valueOf(kph));
            p.setProperty("controls", String.valueOf(controls));
            p.setProperty("difficulty", String.valueOf(difficulty));
            p.setProperty("hints", String.valueOf(hints));
            p.setProperty("effects", String.valueOf(effects));
            FileOutputStream out = new FileOutputStream(f);
            try { p.store(out, "Road to Legendary settings"); } finally { out.close(); }
        } catch (IOException e) {
            // not being able to save settings is not a problem
        } catch (RuntimeException e) {
            // same
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    private static int num(Properties p, String key, int def, int lo, int hi) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
            return Math.max(lo, Math.min(hi, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Converts a game speed (pixels per second) to the number shown on the speedometer. */
    public static int displaySpeed(double pxPerSecond) {
        double kmh = pxPerSecond * 0.55;
        return (int) Math.round(kph ? kmh : kmh * 0.621371);
    }

    public static String speedUnit() { return kph ? "KM/H" : "MPH"; }
}
