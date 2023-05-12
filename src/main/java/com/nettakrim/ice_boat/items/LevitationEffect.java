package com.nettakrim.ice_boat.items;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.nettakrim.ice_boat.IceBoat;

public class LevitationEffect {
    
    private final IceBoat plugin;

    private final Entity vehicle;
    private final Player player;
    private final World world;

    private long duration;
    private BukkitTask loopTask;
    private double driftCorrection;

    private boolean finished;

    public LevitationEffect(IceBoat plugin, Entity vehicle, Player player, long duration) {
        this.plugin = plugin;
        this.vehicle = vehicle;
        this.player = player;
        this.world = player.getWorld();
        run(duration);
    }

    public void run(long duration) {
        this.duration = duration;

        if (!vehicle.isOnGround()) {
            Vector v = vehicle.getVelocity();
            v.setY(0);
            vehicle.setVelocity(v);
        }

        vehicle.setGravity(false);

        plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, player.getLocation(), true, 0.95f, 1.05f);

        this.loopTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            loop();
        }, 0L, 0L);

        this.driftCorrection = player.getLocation().getY()-0.1;
    }

    public void loop() {
        Location location = player.getLocation();
        world.spawnParticle(Particle.FIREWORKS_SPARK, location, 0, 0, 0.1, 0);
        if (location.getY() < driftCorrection) {
            Vector v = vehicle.getVelocity();
            v.setY(0);
            vehicle.setVelocity(v);
            driftCorrection -= 0.1;
        }
        duration--;
        if (duration <= 0) {
            loopTask.cancel();
            end(true);
        }
    }

    public void end(boolean playSound) {
        if (playSound) {
            world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1, null);
            plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, player.getLocation(), true, 0.95f, 1.05f);
        }
        vehicle.setGravity(true);
        finished = true;
    }

    public void cancel(boolean playSound) {
        loopTask.cancel();
        end(playSound);
    }

    public static boolean isFinished(LevitationEffect effect) {
        return effect == null || effect.finished;
    }
}
