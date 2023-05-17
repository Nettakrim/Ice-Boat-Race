package com.nettakrim.ice_boat.items;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.nettakrim.ice_boat.IceBoat;

public class LevitationEffect extends BukkitRunnable {
    
    private final IceBoat plugin;

    private final Player player;
    private final World world;

    private float startDuration;
    private long duration;
    private double driftCorrection;

    public LevitationEffect(IceBoat plugin, Player player, long duration) {
        this.plugin = plugin;
        this.player = player;
        this.world = player.getWorld();
        
        start(duration);
    }

    private void start(long duration) {
        this.startDuration = duration;
        this.duration = duration;

        Entity vehicle = player.getVehicle();
        if (!vehicle.isOnGround()) {
            Vector v = vehicle.getVelocity();
            v.setY(0);
            vehicle.setVelocity(v);
        }

        vehicle.setGravity(false);

        plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, player.getLocation(), true, 0.95f, 1.05f);

        this.driftCorrection = player.getLocation().getY()-0.1;
    }

    @Override
    public void run() {
        Location location = player.getLocation();
        world.spawnParticle(Particle.FIREWORKS_SPARK, location, 0, 0, 0.1, 0);
        if (location.getY() < driftCorrection) {
            Entity vehicle = player.getVehicle();
            Vector v = vehicle.getVelocity();
            v.setY(0);
            vehicle.setVelocity(v);
            driftCorrection -= 0.1;
        }
        duration--;
        player.setExp(((float)duration)/startDuration);
        player.setLevel((int)(duration/20)+1);
        if (duration <= 0) {
            stop(player.getVehicle(), true);
        }
    }

    public void stop(Entity vehicle, boolean makeNoise) {
        player.setExp(0);
        player.setLevel(0);
        vehicle.setGravity(true);
        if (makeNoise) {
            world.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1, null);
            plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_FIREWORK_ROCKET_BLAST, player.getLocation(), true, 0.95f, 1.05f);
        }
        cancel();
    }
}
