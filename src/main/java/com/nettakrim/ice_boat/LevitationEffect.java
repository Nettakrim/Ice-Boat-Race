package com.nettakrim.ice_boat;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class LevitationEffect {
    private final Entity vehicle;
    private final Player player;

    private BukkitTask endTask;
    private BukkitTask loopTask;

    private boolean finished;

    public LevitationEffect(Entity vehicle, Player player) {
        this.vehicle = vehicle;
        this.player = player;
    }

    public LevitationEffect run() {
        if (!vehicle.isOnGround()) {
            Vector v = vehicle.getVelocity();
            v.setY(0);
            vehicle.setVelocity(v);
        }

        vehicle.setGravity(false);

        BoatListener.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, player.getLocation());

        endTask = Bukkit.getScheduler().runTaskLater(IceBoat.instance, () -> {
            end();
            loopTask.cancel();
        }, 100L);

        World world = player.getWorld();

        loopTask = Bukkit.getScheduler().runTaskTimer(IceBoat.instance, () -> {
            world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 0, 0, 0.1, 0);
        }, 0L, 0L);

        return this;
    }

    public void end() {
        vehicle.setGravity(true);
        BoatListener.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, player.getLocation());
        finished = true;
    }

    public void cancel() {
        endTask.cancel();
        loopTask.cancel();
        end();
    }

    public static boolean isFinished(LevitationEffect effect) {
        return effect == null || effect.finished;
    }
}
