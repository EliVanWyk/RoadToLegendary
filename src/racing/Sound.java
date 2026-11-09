package racing;

import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

/**
 * All sound in the game is made by code (no sound files are needed):
 *  - short sound effects (clicks, crashes, nitro...)
 *  - two little chip-tune songs (menu music and race music)
 *  - a live engine sound that follows the car's RPM
 *
 * Everything is wrapped in try/catch, so a computer without speakers
 * (or without a sound card) simply plays the game silently.
 */
public final class Sound {

    // ---- sound effect ids ----
    public static final int CLICK = 0, BUY = 1, CASH = 2, COUNT = 3, GO = 4, CRASH = 5, BUMP = 6,
            NITRO = 7, FINISH_WIN = 8, FINISH_LOSE = 9, RANKUP = 10, ELIMINATED = 11,
            CHECKPOINT = 12, WARNING = 13, BOOST = 14, ERROR = 15, SIREN = 16;
    private static final int SFX_COUNT = 17;

    // ---- music ids ----
    public static final int MUSIC_NONE = -1, MUSIC_MENU = 0, MUSIC_RACE = 1;

    static final int RATE = 22050;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    private static byte[][] sfxData;
    private static Clip[][] sfxClips;
    private static byte[] skidData;
    private static Clip skidClip;
    private static byte[][] musicData;
    private static Clip[] musicClips;
    private static int currentMusic = MUSIC_NONE;
    private static boolean ready = false;
    private static boolean failed = false;

    // engine
    private static volatile boolean engineOn = false;
    private static volatile double engineRpm = 0.2;     // 0..1
    private static volatile double engineLoad = 0.0;    // 0..1 (gas pedal)
    private static Thread engineThread;

    private Sound() { }

    // ------------------------------------------------------------------
    // Start-up
    // ------------------------------------------------------------------
    /** Builds all sounds in a background thread so the window opens right away. */
    public static void init() {
        Thread t = new Thread(new Runnable() {
            public void run() { build(); }
        }, "sound-init");
        t.setDaemon(true);
        t.start();
    }

    private static synchronized void build() {
        if (ready || failed) return;
        try {
            sfxData = new byte[SFX_COUNT][];
            for (int i = 0; i < SFX_COUNT; i++) sfxData[i] = makeEffect(i);
            skidData = makeSkid();
            musicData = new byte[2][];
            musicData[MUSIC_MENU] = makeMusic(false);
            musicData[MUSIC_RACE] = makeMusic(true);
            sfxClips = new Clip[SFX_COUNT][];
            musicClips = new Clip[2];
            ready = true;
        } catch (Throwable e) {
            failed = true;
        }
        // if a song was asked for before we were ready, start it now
        if (ready && currentMusic != MUSIC_NONE) {
            int m = currentMusic;
            currentMusic = MUSIC_NONE;
            playMusic(m);
        }
    }

    // ------------------------------------------------------------------
    // Public controls
    // ------------------------------------------------------------------
    public static void play(int id) {
        if (!Settings.sfxOn || Settings.sfxVolume <= 0 || !ready || id < 0 || id >= SFX_COUNT) return;
        try {
            Clip[] pool = sfxClips[id];
            if (pool == null) {
                pool = new Clip[2];
                sfxClips[id] = pool;
            }
            for (int i = 0; i < pool.length; i++) {
                if (pool[i] == null) pool[i] = openClip(sfxData[id]);
                Clip c = pool[i];
                if (c == null) continue;
                if (c.isRunning()) continue;
                setGain(c, Settings.sfxVolume / 100.0 * 0.9);
                c.setFramePosition(0);
                c.start();
                return;
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    /** Turns the tyre-screech on or off. */
    public static void skid(boolean on) {
        if (!ready) return;
        try {
            if (on && Settings.sfxOn && Settings.sfxVolume > 0) {
                if (skidClip == null) skidClip = openClip(skidData);
                if (skidClip == null) return;
                setGain(skidClip, Settings.sfxVolume / 100.0 * 0.45);
                if (!skidClip.isRunning()) {
                    skidClip.setFramePosition(0);
                    skidClip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } else if (skidClip != null && skidClip.isRunning()) {
                skidClip.stop();
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    public static void playMusic(int track) {
        if (track == currentMusic) {
            applyMusicSettings();
            return;
        }
        currentMusic = track;
        if (!ready) return;
        try {
            for (int i = 0; i < musicClips.length; i++) {
                if (musicClips[i] != null && musicClips[i].isRunning()) musicClips[i].stop();
            }
            if (track == MUSIC_NONE) return;
            if (musicClips[track] == null) musicClips[track] = openClip(musicData[track]);
            applyMusicSettings();
        } catch (Throwable e) {
            // ignore
        }
    }

    /** Call after the player changes music settings. */
    public static void applyMusicSettings() {
        if (!ready || currentMusic == MUSIC_NONE) return;
        try {
            Clip c = musicClips[currentMusic];
            if (c == null) {
                c = openClip(musicData[currentMusic]);
                musicClips[currentMusic] = c;
            }
            if (c == null) return;
            if (!Settings.musicOn || Settings.musicVolume <= 0) {
                if (c.isRunning()) c.stop();
                return;
            }
            setGain(c, Settings.musicVolume / 100.0 * 0.55);
            if (!c.isRunning()) {
                c.setFramePosition(0);
                c.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    /** Call after the player changes sound-effect settings. */
    public static void applySfxSettings() {
        if (!Settings.sfxOn || Settings.sfxVolume <= 0) {
            skid(false);
        }
    }

    // ---- engine ----
    public static void engineStart() {
        if (!ready) return;
        engineOn = true;
        if (engineThread != null && engineThread.isAlive()) return;
        engineThread = new Thread(new Runnable() {
            public void run() { engineLoop(); }
        }, "engine-sound");
        engineThread.setDaemon(true);
        engineThread.start();
    }

    public static void engineStop() { engineOn = false; }

    public static void engineSet(double rpm01, double load01) {
        engineRpm = Math.max(0, Math.min(1, rpm01));
        engineLoad = Math.max(0, Math.min(1, load01));
    }

    private static void engineLoop() {
        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, 4096);
            line.start();
            int n = 512;
            byte[] buf = new byte[n * 2];
            double phase = 0, lp = 0;
            double smoothRpm = 0.2;
            Random rnd = new Random(5);
            boolean wasOn = false;
            while (true) {
                if (!engineOn || !Settings.sfxOn || Settings.sfxVolume <= 0) {
                    if (wasOn) { line.flush(); wasOn = false; }
                    Thread.sleep(40);
                    continue;
                }
                wasOn = true;
                double vol = Settings.sfxVolume / 100.0 * 0.22;
                for (int i = 0; i < n; i++) {
                    smoothRpm += (engineRpm - smoothRpm) * 0.0006;
                    double f = 38 + smoothRpm * 140;
                    phase += f / RATE;
                    if (phase >= 1) phase -= 1;
                    double saw = phase * 2 - 1;
                    double sq = phase < 0.4 ? 1 : -1;
                    double s = 0.55 * saw + 0.35 * sq + 0.25 * Math.sin(phase * 4 * Math.PI);
                    s += (rnd.nextDouble() * 2 - 1) * (0.05 + 0.12 * engineLoad);
                    lp += (s - lp) * (0.18 + 0.25 * engineLoad);
                    double out = lp * vol * (0.55 + 0.45 * engineLoad);
                    short v = (short) Math.max(-32000, Math.min(32000, out * 32000));
                    buf[2 * i] = (byte) (v & 0xFF);
                    buf[2 * i + 1] = (byte) ((v >> 8) & 0xFF);
                }
                line.write(buf, 0, buf.length);
            }
        } catch (Throwable e) {
            // no sound card, or the thread was interrupted: just stop
        } finally {
            engineOn = false;
            try { if (line != null) line.close(); } catch (Throwable e) { /* ignore */ }
        }
    }

    // ------------------------------------------------------------------
    // Clip helpers
    // ------------------------------------------------------------------
    private static Clip openClip(byte[] data) {
        try {
            Clip c = AudioSystem.getClip();
            c.open(FORMAT, data, 0, data.length);
            return c;
        } catch (Throwable e) {
            return null;
        }
    }

    private static void setGain(Clip c, double volume) {
        try {
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl g = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                double v = Math.max(0.0001, Math.min(1.0, volume));
                float db = (float) (20.0 * Math.log10(v));
                db = Math.max(g.getMinimum(), Math.min(g.getMaximum(), db));
                g.setValue(db);
            }
        } catch (Throwable e) {
            // ignore
        }
    }

    // ------------------------------------------------------------------
    // Synthesiser
    // ------------------------------------------------------------------
    private static final int SQUARE = 0, PULSE = 1, TRIANGLE = 2, SAW = 3, NOISE = 4, SINE = 5;

    private static double midi(int note) { return 440.0 * Math.pow(2, (note - 69) / 12.0); }

    /** Adds one note to the mix buffer. */
    static void voice(float[] mix, double startSec, double durSec, double freq, int type, double vol, double freqEnd) {
        int start = (int) (startSec * RATE);
        int len = (int) (durSec * RATE);
        double phase = 0;
        Random rnd = new Random(start * 31L + (long) freq);
        double lp = 0;
        for (int i = 0; i < len; i++) {
            int idx = start + i;
            if (idx < 0 || idx >= mix.length) continue;
            double t = (double) i / len;
            double f = freqEnd > 0 ? freq + (freqEnd - freq) * t : freq;
            phase += f / RATE;
            phase -= Math.floor(phase);
            double s;
            switch (type) {
                case SQUARE: s = phase < 0.5 ? 1 : -1; break;
                case PULSE: s = phase < 0.25 ? 1 : -1; break;
                case TRIANGLE: s = 4 * Math.abs(phase - 0.5) - 1; break;
                case SAW: s = phase * 2 - 1; break;
                case SINE: s = Math.sin(phase * 2 * Math.PI); break;
                default: {
                    double nz = rnd.nextDouble() * 2 - 1;
                    // freq acts as a "brightness" for noise: higher = brighter
                    double k = Math.max(0.02, Math.min(1.0, f / 8000.0));
                    lp += (nz - lp) * k;
                    s = lp;
                }
            }
            double env = Math.min(1.0, i / (RATE * 0.003)) * Math.pow(1.0 - t, 1.4);
            mix[idx] += (float) (s * env * vol);
        }
    }

    private static byte[] toBytes(float[] mix, double gain) {
        byte[] out = new byte[mix.length * 2];
        for (int i = 0; i < mix.length; i++) {
            double v = Math.max(-1.0, Math.min(1.0, mix[i] * gain));
            short s = (short) (v * 30000);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }

    private static byte[] makeEffect(int id) {
        float[] m;
        switch (id) {
            case CLICK:
                m = new float[(int) (RATE * 0.06)];
                voice(m, 0, 0.05, 1000, SQUARE, 0.35, 700);
                break;
            case ERROR:
                m = new float[(int) (RATE * 0.3)];
                voice(m, 0, 0.12, 200, SQUARE, 0.4, 0);
                voice(m, 0.13, 0.15, 150, SQUARE, 0.4, 0);
                break;
            case SIREN:
                m = new float[(int) (RATE * 0.8)];
                voice(m, 0, 0.4, 760, SQUARE, 0.22, 0);
                voice(m, 0.4, 0.4, 1020, SQUARE, 0.22, 0);
                break;
            case BUY:
                m = new float[(int) (RATE * 0.3)];
                voice(m, 0, 0.09, midi(72), SQUARE, 0.3, 0);
                voice(m, 0.09, 0.2, midi(79), SQUARE, 0.3, 0);
                break;
            case CASH:
                m = new float[(int) (RATE * 0.55)];
                voice(m, 0, 0.1, midi(84), PULSE, 0.3, 0);
                voice(m, 0.09, 0.1, midi(88), PULSE, 0.3, 0);
                voice(m, 0.18, 0.35, midi(91), PULSE, 0.3, 0);
                break;
            case COUNT:
                m = new float[(int) (RATE * 0.25)];
                voice(m, 0, 0.2, 440, SQUARE, 0.35, 0);
                break;
            case GO:
                m = new float[(int) (RATE * 0.6)];
                voice(m, 0, 0.5, 880, SQUARE, 0.35, 0);
                voice(m, 0, 0.5, 1320, TRIANGLE, 0.25, 0);
                break;
            case CRASH:
                m = new float[(int) (RATE * 0.5)];
                voice(m, 0, 0.45, 2500, NOISE, 0.9, 400);
                voice(m, 0, 0.3, 110, SINE, 0.7, 40);
                break;
            case BUMP:
                m = new float[(int) (RATE * 0.2)];
                voice(m, 0, 0.18, 1200, NOISE, 0.6, 300);
                voice(m, 0, 0.15, 90, SINE, 0.6, 50);
                break;
            case NITRO:
                m = new float[(int) (RATE * 0.8)];
                voice(m, 0, 0.75, 300, SAW, 0.25, 1100);
                voice(m, 0, 0.75, 1500, NOISE, 0.5, 5000);
                break;
            case BOOST:
                m = new float[(int) (RATE * 0.4)];
                voice(m, 0, 0.35, 400, SQUARE, 0.2, 1200);
                voice(m, 0, 0.35, 900, NOISE, 0.3, 3000);
                break;
            case FINISH_WIN:
                m = new float[(int) (RATE * 1.6)];
                int[] win = { 60, 64, 67, 72, 67, 72, 76 };
                for (int i = 0; i < win.length; i++) {
                    voice(m, i * 0.13, i == win.length - 1 ? 0.8 : 0.16, midi(win[i]), SQUARE, 0.28, 0);
                    voice(m, i * 0.13, i == win.length - 1 ? 0.8 : 0.16, midi(win[i] - 12), TRIANGLE, 0.3, 0);
                }
                break;
            case FINISH_LOSE:
                m = new float[(int) (RATE * 1.2)];
                int[] lose = { 67, 64, 60 };
                for (int i = 0; i < lose.length; i++) {
                    voice(m, i * 0.22, i == 2 ? 0.6 : 0.24, midi(lose[i]), TRIANGLE, 0.45, 0);
                }
                break;
            case RANKUP:
                m = new float[(int) (RATE * 2.2)];
                int[] up = { 60, 64, 67, 72, 76, 79, 84 };
                for (int i = 0; i < up.length; i++) {
                    voice(m, i * 0.12, i == up.length - 1 ? 1.1 : 0.15, midi(up[i]), PULSE, 0.28, 0);
                    voice(m, i * 0.12, i == up.length - 1 ? 1.1 : 0.15, midi(up[i] - 12), SQUARE, 0.18, 0);
                }
                break;
            case ELIMINATED:
                m = new float[(int) (RATE * 0.6)];
                voice(m, 0, 0.5, 400, SAW, 0.3, 90);
                break;
            case CHECKPOINT:
                m = new float[(int) (RATE * 0.35)];
                voice(m, 0, 0.1, midi(79), SQUARE, 0.3, 0);
                voice(m, 0.1, 0.2, midi(86), SQUARE, 0.3, 0);
                break;
            default: // WARNING
                m = new float[(int) (RATE * 0.3)];
                voice(m, 0, 0.12, 660, SQUARE, 0.3, 0);
                voice(m, 0.15, 0.12, 660, SQUARE, 0.3, 0);
        }
        return toBytes(m, 1.0);
    }

    private static byte[] makeSkid() {
        float[] m = new float[(int) (RATE * 0.5)];
        Random r = new Random(3);
        double lp = 0, lp2 = 0;
        for (int i = 0; i < m.length; i++) {
            double nz = r.nextDouble() * 2 - 1;
            lp += (nz - lp) * 0.35;
            lp2 += (lp - lp2) * 0.5;
            m[i] = (float) ((lp - lp2) * 2.0 + Math.sin(i * 0.19) * 0.08);
        }
        return toBytes(m, 0.7);
    }

    /**
     * A little chip-tune song. Menu: relaxed and bouncy. Race: fast and driving.
     * The chords go Am - F - C - G (menu) or Am - G - F - E (race).
     */
    private static byte[] makeMusic(boolean race) {
        int bpm = race ? 152 : 112;
        double beat = 60.0 / bpm;
        int bars = 8;
        double total = bars * 4 * beat;
        float[] m = new float[(int) (total * RATE) + RATE / 2];

        int[][] chords = race
                ? new int[][] { { 57, 60, 64 }, { 55, 59, 62 }, { 53, 57, 60 }, { 52, 56, 59 } }
                : new int[][] { { 57, 60, 64 }, { 53, 57, 60 }, { 48, 52, 55 }, { 55, 59, 62 } };
        int[] bassRoot = race ? new int[] { 33, 31, 29, 28 } : new int[] { 33, 29, 36, 31 };
        int[] melodyScale = { 69, 72, 74, 76, 79, 81 };
        Random rnd = new Random(race ? 11 : 7);

        for (int bar = 0; bar < bars; bar++) {
            int ci = bar % 4;
            double t0 = bar * 4 * beat;
            // bass: triangle, root on every beat (race) or on 1 and 3 (menu)
            for (int b = 0; b < 4; b++) {
                if (race || b % 2 == 0) {
                    double t = t0 + b * beat;
                    int note = bassRoot[ci] + ((b == 3 && race) ? 12 : 0);
                    voice(m, t, beat * (race ? 0.9 : 1.8), midi(note), TRIANGLE, 0.5, 0);
                }
            }
            // arpeggio: pulse wave through the chord
            int steps = race ? 16 : 8;
            double stepLen = 4 * beat / steps;
            for (int s = 0; s < steps; s++) {
                int[] ch = chords[ci];
                int note = ch[s % 3] + 12 * ((s / 3) % 2);
                voice(m, t0 + s * stepLen, stepLen * 0.9, midi(note), PULSE, race ? 0.13 : 0.11, 0);
            }
            // melody: a few notes per bar (second half of the song is busier)
            int notes = bar >= 4 ? 4 : 2;
            double nl = 4 * beat / notes;
            for (int k = 0; k < notes; k++) {
                if (rnd.nextInt(5) == 0) continue;
                int note = melodyScale[rnd.nextInt(melodyScale.length)];
                voice(m, t0 + k * nl, nl * 0.85, midi(note), SQUARE, 0.12, 0);
            }
            // drums
            for (int b = 0; b < 4; b++) {
                double t = t0 + b * beat;
                if (race || b % 2 == 0) voice(m, t, 0.12, 140, SINE, 0.55, 45);          // kick
                if (b % 2 == 1) voice(m, t, 0.1, 3500, NOISE, race ? 0.28 : 0.18, 0);      // snare
                voice(m, t, 0.04, 9000, NOISE, 0.10, 0);                                   // hat
                voice(m, t + beat / 2, 0.04, 9000, NOISE, 0.08, 0);
            }
        }
        // make the loop seamless: fade out the very end
        int fade = RATE / 20;
        for (int i = 0; i < fade; i++) {
            int idx = (int) (total * RATE) - i;
            if (idx > 0 && idx < m.length) m[idx] *= (float) i / fade;
        }
        float[] cut = new float[(int) (total * RATE)];
        System.arraycopy(m, 0, cut, 0, cut.length);
        return toBytes(cut, 0.8);
    }

    /** Used by the self-test: true if the synthesiser produced all its sounds. */
    public static boolean selfTest() {
        build();
        return ready;
    }
}
