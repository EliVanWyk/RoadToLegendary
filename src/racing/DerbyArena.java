package racing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The rules and physics of the demolition derby. A round arena, 7 cars,
 * the last car still driving wins. (The drawing is done by DerbyPlayScreen.)
 *
 *  - cars slide: they keep their speed sideways unless the tyres can grip
 *  - hits hurt: heavier cars hit harder, the front of a car is tougher than its side or back
 *  - after 45 seconds the wall starts closing in, so nobody can hide forever
 */
final class DerbyArena {

    static final double R0 = 290;
    static final double CR = 15;       // collision circle radius
    static final double DAMAGE = 0.034;  // how much a hit hurts
    static final double HALF = 13;     // each car is two circles, 13 pixels in front of and behind its middle

    static final int EV_COUNT = 0, EV_GO = 1, EV_HIT = 2, EV_WALL = 3, EV_WRECK = 4, EV_BOOST = 5, EV_SHRINK = 6;

    static final class Ev {
        final int type;
        final double x, y, value;
        final DCar car;
        Ev(int type, double x, double y, double value, DCar car) { this.type = type; this.x = x; this.y = y; this.value = value; this.car = car; }
    }

    static final class DCar {
        final DerbyCar spec;
        final boolean isPlayer;
        final String label;
        final Color color;
        double x, y, h, vx, vy, spin;
        double hp, maxHp;
        boolean wrecked;
        int place, kos;
        double boostT, boostCool, wreckedAt = -1;
        // controls (the player's come from the keyboard, the AI computes its own)
        double steer, throttle;
        boolean handbrake, wantBoost;
        // AI memory
        double aggro, retarget, backoff, flee;
        DCar target, lastHit;
        double lastHitTime = -10;

        DCar(DerbyCar spec, boolean isPlayer, String label, Color color, double hpScale) {
            this.spec = spec;
            this.isPlayer = isPlayer;
            this.label = label;
            this.color = color;
            this.maxHp = spec.hp * hpScale;
            this.hp = maxHp;
        }

        double speed() { return Math.hypot(vx, vy); }
        double fx() { return Math.sin(h); }
        double fy() { return -Math.cos(h); }
    }

    final List<DCar> cars = new ArrayList<DCar>();
    final List<Ev> events = new ArrayList<Ev>();
    DCar player;
    double time;
    double countdown = 3.5;
    double radius = R0;
    boolean over;
    private int lastStage = 4;
    private double overTimer = -1;
    private boolean shrinkWarned;
    private final Random rng = new Random();
    private static final String[] NAMES = { "Crusher", "Dent", "Scrap", "Boomer", "Slammer", "Rattle", "Bruiser", "Junker", "Wreck", "Smash" };

    DerbyArena(DerbyCar playerCar) {
        ArrayList<DerbyCar> all = DerbyCar.catalog();
        player = new DCar(playerCar, true, "YOU", playerCar.color, 1.0);
        cars.add(player);
        ArrayList<String> names = new ArrayList<String>();
        for (String n : NAMES) names.add(n);
        java.util.Collections.shuffle(names, rng);
        for (int i = 0; i < 6; i++) {
            DerbyCar spec = all.get(rng.nextInt(all.size()));
            Color c = tint(spec.color, rng);
            DCar ai = new DCar(spec, false, names.get(i), c, 0.9 + rng.nextDouble() * 0.2);
            ai.aggro = 0.5 + rng.nextDouble() * 0.5;
            cars.add(ai);
        }
        // the cars start in a ring and look at the middle
        for (int i = 0; i < cars.size(); i++) {
            double ang = Math.PI / 2 + i * 2 * Math.PI / cars.size();
            DCar c = cars.get(i);
            c.x = Math.cos(ang) * 205;
            c.y = Math.sin(ang) * 205;
            double fx = -c.x, fy = -c.y;
            c.h = Math.atan2(fx, -fy);
        }
    }

    private static Color tint(Color c, Random r) {
        int d = r.nextInt(60) - 30;
        return new Color(clamp255(c.getRed() + d), clamp255(c.getGreen() + r.nextInt(60) - 30), clamp255(c.getBlue() + r.nextInt(60) - 30));
    }

    private static int clamp255(int v) { return Math.max(20, Math.min(240, v)); }

    int alive() {
        int n = 0;
        for (DCar c : cars) if (!c.wrecked) n++;
        return n;
    }

    /** Moves everything forward by dt seconds. The player's controls are passed in. */
    void update(double dt, int steer, int throttle, boolean boost, boolean handbrake) {
        if (over) return;
        if (countdown > 0) {
            countdown -= dt;
            int st = countdown > 2.5 ? 3 : countdown > 1.5 ? 2 : countdown > 0.5 ? 1 : 0;
            if (st != lastStage) {
                lastStage = st;
                events.add(new Ev(st > 0 ? EV_COUNT : EV_GO, 0, 0, st, null));
            }
            return;
        }
        time += dt;

        // the wall closes in after a minute
        if (time > 45) {
            radius = Math.max(110, R0 - (time - 45) * 5);
            if (!shrinkWarned) {
                shrinkWarned = true;
                events.add(new Ev(EV_SHRINK, 0, 0, 0, null));
            }
        }

        player.steer = steer;
        player.throttle = throttle;
        player.handbrake = handbrake;
        player.wantBoost = boost;
        for (DCar c : cars) {
            if (c.wrecked) continue;
            if (!c.isPlayer) think(c, dt);
            drive(c, dt);
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < cars.size(); i++) {
                for (int j = i + 1; j < cars.size(); j++) collide(cars.get(i), cars.get(j));
            }
        }
        for (DCar c : cars) wall(c);
        for (DCar c : cars) {
            if (!c.wrecked && c.hp <= 0) wreck(c);
        }

        // end of the derby
        if (overTimer < 0) {
            if (alive() <= 1) {
                for (DCar c : cars) if (!c.wrecked) c.place = 1;
                overTimer = 1.8;
            } else if (player.wrecked) {
                overTimer = 2.2;
            } else if (time > 140) {
                int better = 0;
                for (DCar c : cars) if (c != player && !c.wrecked && c.hp > player.hp) better++;
                player.place = 1 + better;
                overTimer = 0.1;
            }
        }
        if (overTimer >= 0) {
            overTimer -= dt;
            if (overTimer <= 0) over = true;
        }
    }

    // ------------------------------------------------------------------
    // physics
    // ------------------------------------------------------------------
    private void drive(DCar c, double dt) {
        double fx = c.fx(), fy = c.fy();
        double rx = Math.cos(c.h), ry = Math.sin(c.h);
        double fwd = c.vx * fx + c.vy * fy;
        double lat = c.vx * rx + c.vy * ry;

        // nitro
        if (c.boostCool > 0) c.boostCool -= dt;
        if (c.boostT > 0) c.boostT -= dt;
        if (c.wantBoost && c.boostT <= 0 && c.boostCool <= 0) {
            c.boostT = 1.2;
            c.boostCool = 6;
            events.add(new Ev(EV_BOOST, c.x, c.y, 0, c));
        }
        boolean boosting = c.boostT > 0;
        double accel = c.spec.accel * (boosting ? 1.9 : 1.0);
        double top = c.spec.top * (boosting ? 1.35 : 1.0);

        if (c.throttle > 0) {
            if (fwd < top) fwd = Math.min(top, fwd + accel * c.throttle * dt);
        } else if (c.throttle < 0) {
            fwd = Math.max(-top * 0.45, fwd + accel * 1.0 * c.throttle * dt);
        } else {
            fwd *= Math.max(0, 1 - 0.8 * dt);
        }
        if (fwd > top) fwd = Math.max(top, fwd - 160 * dt);
        fwd *= Math.max(0, 1 - 0.15 * dt);
        lat *= Math.exp(-(c.handbrake ? 1.1 : 7.5) * dt);                  // the handbrake lets the car slide
        if (c.handbrake) fwd *= Math.max(0, 1 - 0.7 * dt);

        double steerPower = Math.max(-1, Math.min(1, fwd / 80.0));
        double turnRate = c.spec.turn * (c.handbrake ? 1.5 : 1.0);
        c.h += (c.steer * turnRate * steerPower + c.spin) * dt;
        c.spin *= Math.max(0, 1 - 2.5 * dt);

        double nfx = Math.sin(c.h), nfy = -Math.cos(c.h);
        double nrx = Math.cos(c.h), nry = Math.sin(c.h);
        c.vx = nfx * fwd + nrx * lat;
        c.vy = nfy * fwd + nry * lat;
        c.x += c.vx * dt;
        c.y += c.vy * dt;
    }

    private void wall(DCar c) {
        double d = Math.hypot(c.x, c.y);
        double limit = radius - 22;
        if (d <= limit || d == 0) return;
        double nx = c.x / d, ny = c.y / d;
        c.x = nx * limit;
        c.y = ny * limit;
        double vout = c.vx * nx + c.vy * ny;
        if (vout > 0) {
            c.vx -= 1.3 * vout * nx;
            c.vy -= 1.3 * vout * ny;
            if (vout > 70 && !c.wrecked) {
                c.hp -= vout * 0.045;
                c.lastHit = null;
                events.add(new Ev(EV_WALL, c.x, c.y, vout, c));
            }
        }
    }

    private void collide(DCar a, DCar b) {
        if (a.wrecked && b.wrecked && a.speed() < 5 && b.speed() < 5) return;
        double bestPen = 0, nx = 0, ny = 0, cx = 0, cy = 0;
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                double ax = a.x + i * HALF * a.fx(), ay = a.y + i * HALF * a.fy();
                double bx = b.x + j * HALF * b.fx(), by = b.y + j * HALF * b.fy();
                double dx = bx - ax, dy = by - ay;
                double d = Math.hypot(dx, dy);
                double pen = 2 * CR - d;
                if (pen > bestPen) {
                    bestPen = pen;
                    if (d < 0.001) { nx = 1; ny = 0; } else { nx = dx / d; ny = dy / d; }
                    cx = (ax + bx) / 2;
                    cy = (ay + by) / 2;
                }
            }
        }
        if (bestPen <= 0) return;
        double ma = a.wrecked ? a.spec.mass * 3 : a.spec.mass;
        double mb = b.wrecked ? b.spec.mass * 3 : b.spec.mass;
        double ima = 1 / ma, imb = 1 / mb, tot = ima + imb;
        a.x -= nx * bestPen * ima / tot;
        a.y -= ny * bestPen * ima / tot;
        b.x += nx * bestPen * imb / tot;
        b.y += ny * bestPen * imb / tot;
        double vrel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
        if (vrel >= 0) return;
        double j = -(1 + 0.35) * vrel / tot;
        a.vx -= j * ima * nx;
        a.vy -= j * ima * ny;
        b.vx += j * imb * nx;
        b.vy += j * imb * ny;
        // spin from an off-centre hit
        a.spin -= ((cx - a.x) * ny - (cy - a.y) * nx) * j * ima * 0.0012;
        b.spin += ((cx - b.x) * ny - (cy - b.y) * nx) * j * imb * 0.0012;
        double impact = -vrel;
        if (impact < 60) return;
        events.add(new Ev(EV_HIT, cx, cy, impact, a.isPlayer ? a : b));
        hurt(a, b, impact, nx, ny);
        hurt(b, a, impact, -nx, -ny);
    }

    /** The victim takes damage. The front of a car is tough, the back is weak. */
    private void hurt(DCar victim, DCar attacker, double impact, double nx, double ny) {
        if (victim.wrecked) return;
        double dot = nx * victim.fx() + ny * victim.fy();       // +1: the hit lands on the victim's nose
        double pos = dot > 0.5 ? 0.7 : dot < -0.5 ? 1.4 : 1.1;
        double share = attacker.spec.mass / (attacker.spec.mass + victim.spec.mass);
        victim.hp -= impact * DAMAGE * share * 2 * pos;
        victim.lastHit = attacker;
        victim.lastHitTime = time;
    }

    private void wreck(DCar c) {
        int aliveBefore = alive();
        c.wrecked = true;
        c.hp = 0;
        c.place = aliveBefore;
        c.wreckedAt = time;
        c.vx *= 0.5;
        c.vy *= 0.5;
        c.spin += (rng.nextDouble() - 0.5) * 6;
        c.steer = 0;
        c.throttle = 0;
        if (c.lastHit != null && time - c.lastHitTime < 5 && !c.lastHit.wrecked) c.lastHit.kos++;
        events.add(new Ev(EV_WRECK, c.x, c.y, 0, c));
    }

    // ------------------------------------------------------------------
    // the computer drivers
    // ------------------------------------------------------------------
    void think(DCar c, double dt) {
        c.wantBoost = false;
        c.handbrake = false;
        double dist = Math.hypot(c.x, c.y);
        // keep away from the wall
        boolean nearWall = dist > radius - 75;

        c.retarget -= dt;
        if (c.retarget <= 0 || c.target == null || c.target.wrecked) {
            c.retarget = 0.6 + rng.nextDouble() * 0.6;
            DCar best = null;
            double bestScore = 1e9;
            for (DCar o : cars) {
                if (o == c || o.wrecked) continue;
                double d = Math.hypot(o.x - c.x, o.y - c.y);
                double score = d - (o.maxHp - o.hp) * 1.2 + (o.spec.mass > c.spec.mass * 1.6 ? 160 : 0);
                if (score < bestScore) { bestScore = score; best = o; }
            }
            c.target = best;
        }
        if (c.target == null) return;

        // run away when badly hurt or when a much heavier car is right on top of us
        DCar threat = null;
        double threatD = 1e9;
        for (DCar o : cars) {
            if (o == c || o.wrecked) continue;
            double d = Math.hypot(o.x - c.x, o.y - c.y);
            if (d < threatD) { threatD = d; threat = o; }
        }
        boolean hurt = c.hp < c.maxHp * 0.3;
        boolean flee = threat != null && ((hurt && threatD < 260) || (threat.spec.mass > c.spec.mass * 1.7 && threatD < 120 && c.hp < c.maxHp * 0.6));
        if (c.aggro > 0.85 && !hurt) flee = false;

        double tx, ty;
        if (c.backoff > 0) {
            c.backoff -= dt;
            tx = c.x - c.fx() * 100;
            ty = c.y - c.fy() * 100;
            c.throttle = -1;
            c.steer = 0;
            return;
        }
        if (flee && threat != null) {
            // drive to the far side of the arena, away from the threat
            double ax = c.x - threat.x, ay = c.y - threat.y;
            double len = Math.max(1, Math.hypot(ax, ay));
            tx = c.x + ax / len * 150 - c.x * 0.4;
            ty = c.y + ay / len * 150 - c.y * 0.4;
            if (threatD < 130) c.wantBoost = true;
        } else {
            DCar t = c.target;
            double lead = Math.min(0.5, Math.hypot(t.x - c.x, t.y - c.y) / 400.0);
            tx = t.x + t.vx * lead;
            ty = t.y + t.vy * lead;
        }
        if (nearWall && !flee) {
            // aim back towards the middle when close to the wall
            tx = tx * 0.3;
            ty = ty * 0.3;
        }
        double dx = tx - c.x, dy = ty - c.y;
        double want = Math.atan2(dx, -dy);
        double diff = want - c.h;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        c.steer = Math.max(-1, Math.min(1, diff * 2.2));
        double d = Math.hypot(dx, dy);
        c.throttle = Math.abs(diff) < 1.3 ? (d < 150 ? 0.75 : 1) : 0.5;
        if (Math.abs(diff) > 1.0 && c.speed() > 120 && rng.nextDouble() < 0.02) c.handbrake = true;
        if (!flee && d > 110 && d < 270 && Math.abs(diff) < 0.2 && rng.nextDouble() < 0.4) c.wantBoost = true;
        // after a big hit, back off for a moment and try again
        if (!flee && d < 42 && c.speed() < 90) c.backoff = 0.45 + rng.nextDouble() * 0.4;
    }
}
