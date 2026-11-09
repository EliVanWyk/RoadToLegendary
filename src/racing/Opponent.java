package racing;

import java.util.Random;

/**
 * A computer-controlled racer. It has a name, a Car (same Car class the
 * player uses) and a skill level between 0.0 (rookie) and 1.0 (ace).
 */
public class Opponent {

    public static final String[] NAMES = {
        "Max Torque", "Rita Rev", "Dusty Ray", "Nitro Nate", "Lola Drift",
        "Turbo Tom", "Sandy Skid", "Jet Jenkins", "Gear Gus", "Penny Piston",
        "Flash Fiona", "Rusty Rod", "Zoom Zed", "Bolt Betty", "Hank Hairpin",
        "Mia Mach", "Chip Clutch", "Vicky Velocity", "Axel Ash", "Ryder Road",
        "Sparky Sam", "Wheelie Wes", "Tina Tailpipe", "Cruz Cole", "Drift King Dan",
        "Ivy Ignition", "Blaze Barnes", "Kit Kickstart", "Oscar Overtake", "Queenie Quartz"
    };

    private final String name;
    private final Car car;
    private final double skill;

    public Opponent(String name, Car car, double skill) {
        this.name = name;
        this.car = car;
        this.skill = Math.max(0.0, Math.min(1.0, skill));
    }

    public String getName() { return name; }
    public Car getCar() { return car; }
    public double getSkill() { return skill; }

    /**
     * The speed (pixels per second) this opponent tries to hold during a race.
     * It depends on the car's speed stat, the skill level and a little
     * randomness, so every race is slightly different.
     */
    public double getTopSpeed(Random rng) {
        double skillFactor = 0.86 + 0.12 * skill;
        double luck = 1.0 + (rng.nextDouble() - 0.5) * 0.03;
        return car.getTopSpeedPx() * skillFactor * luck;
    }

    @Override
    public String toString() {
        return name + " - " + car.getName();
    }
}
