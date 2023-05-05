package com.nettakrim.ice_boat.items;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.nettakrim.ice_boat.IceBoat;

public class LevitationEffect {
    private final Entity vehicle;
    private final Player player;
    private final World world;

    private long duration;
    private BukkitTask loopTask;

    private boolean finished;

    public LevitationEffect(Entity vehicle, Player player, long duration) {
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

        IceBoat.playSoundGloballyToPlayer(player, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, player.getLocation());

        this.loopTask = Bukkit.getScheduler().runTaskTimer(IceBoat.instance, () -> {
            loop();
        }, 0L, 0L);
    }

    public void loop() {
        world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 0, 0, 0.1, 0);
        duration--;
        if (duration <= 0) {
            loopTask.cancel();
            end();
        }
    }

    public void end() {
        world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1, null);
        vehicle.setGravity(true);
        IceBoat.playSoundGloballyToPlayer(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, player.getLocation());
        finished = true;
    }

    public void cancel() {
        loopTask.cancel();
        end();
    }

    public static boolean isFinished(LevitationEffect effect) {
        return effect == null || effect.finished;
    }
}
