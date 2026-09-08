package com.sram.hexatype;

import java.util.Random;

/**
 * Shots and particles: the cosmetic layer of the simulation. Operates on the core's lists
 * rather than owning them, since a shot has to home in on a live enemy.
 */
final class Fx {

    private Fx() {}

    static void updateShots(GameCore c, float dt, Layout L) {
        for (int i = c.shots.size() - 1; i >= 0; i--) {
            GameCore.Shot s = c.shots.get(i);
            if (s.target != null && c.enemies.contains(s.target)) {
                // Home in: the word keeps drifting while the shot is in the air.
                s.tx = s.kill ? c.enemyCentreX(s.target) : c.tileX(s.target, s.tileIndex, L);
                s.ty = s.target.y;
            } else if (s.atBoss && c.boss.body != null) {
                // The same homing for a shot at the boss, which drifts and wobbles just as much.
                s.tx = c.boss.body.centreX() + s.bossDx;
                s.ty = c.boss.body.centreY() + s.bossDy;
            }
            s.t += dt / s.dur;
            if (s.t >= 1f) {
                if (!s.shieldBounce) c.impact(s, L);
                c.shots.remove(i);
            }
        }
    }

    static void explode(GameCore c, Random rnd, float x, float y, float spread, int n,
            int color) {
        for (int i = 0; i < n; i++) {
            GameCore.Particle p = new GameCore.Particle();
            double a = rnd.nextFloat() * 6.283f;
            float v = spread * (2.5f + rnd.nextFloat() * 4f);
            p.x = x;
            p.y = y;
            p.vx = v * (float) Math.cos(a);
            p.vy = v * (float) Math.sin(a);
            p.max = 0.28f + rnd.nextFloat() * 0.42f;
            p.life = p.max;
            p.size = spread * (0.10f + rnd.nextFloat() * 0.16f);
            p.color = color;
            c.particles.add(p);
        }
    }

    static void explodeUp(GameCore c, Random rnd, float x, float y, float spread, int n,
            int color) {
        for (int i = 0; i < n; i++) {
            GameCore.Particle p = new GameCore.Particle();
            p.x = x;
            p.y = y;
            p.vx = (rnd.nextFloat() - 0.5f) * spread * 7f;
            p.vy = -spread * (4f + rnd.nextFloat() * 6f);
            p.max = 0.48f + rnd.nextFloat() * 0.42f;
            p.life = p.max;
            p.size = spread * (0.10f + rnd.nextFloat() * 0.18f);
            p.color = i % 3 == 0 ? 0xFFFFFFFF : color;
            c.particles.add(p);
        }
    }

    /**
     * One sparkle, for the trail that follows a finger during FLING. Slower and longer-lived
     * than an explosion mote, so a drag leaves a readable ribbon rather than a puff.
     */
    static void sparkle(GameCore c, Random rnd, float x, float y, float size, int color) {
        GameCore.Particle p = new GameCore.Particle();
        p.x = x + (rnd.nextFloat() - 0.5f) * size * 1.6f;
        p.y = y + (rnd.nextFloat() - 0.5f) * size * 1.6f;
        p.vx = (rnd.nextFloat() - 0.5f) * size * 2.2f;
        p.vy = (rnd.nextFloat() - 0.5f) * size * 2.2f;
        p.max = 0.40f + rnd.nextFloat() * 0.40f;
        p.life = p.max;
        p.size = size * (0.22f + rnd.nextFloat() * 0.30f);
        p.color = color;
        c.particles.add(p);
    }

    static void updateParticles(GameCore c, float dt) {
        for (int i = c.particles.size() - 1; i >= 0; i--) {
            GameCore.Particle p = c.particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                c.particles.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 0.94f;
            p.vy = p.vy * 0.94f + 220f * dt;
        }
    }
}
