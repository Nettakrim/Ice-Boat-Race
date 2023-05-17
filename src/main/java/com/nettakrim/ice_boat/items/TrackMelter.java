package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.paths.Path;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

public class TrackMelter extends BukkitRunnable {

    private final IceBoat plugin;

    private final Location location;
    private final World world;
    private final Path path;

    private int radius;
    private int ox;
    private int oy;

    private long startDuration;
    private long duration;

    public TrackMelter(IceBoat plugin, Location location, Path path, int radius, long duration) {
        this.plugin = plugin;
        this.location = location;
        this.world = location.getWorld();
        this.path = path;
        start(radius, duration);
    }

    public void start(int radius, long duration) {
        this.radius = radius;
        ox = location.getBlockX();
        oy = location.getBlockZ();
        this.duration = duration;
        this.startDuration = duration;
        plugin.playSoundLocallyToAll(Sound.ENTITY_GHAST_SHOOT, location, 0.8f, 1.2f);
    }

    @Override
    public void run() {
        int x = plugin.random.nextInt(-radius, radius+1);
        int y = plugin.random.nextInt(-radius, radius+1);
        if (x*x + y*y < radius*radius) {
            path.melt(world, x+ox, y+oy, (int)((startDuration-duration)/30));
        }
        world.spawnParticle(Particle.FLAME, location, 5, radius, 1, radius, 0.1);
        duration--;
        if (duration <= 0) cancel();
        if (plugin.random.nextFloat()<0.2) {
            plugin.playSoundLocallyToAll(Sound.BLOCK_FIRE_AMBIENT, location, 0.8f, 1.2f);
        }
    }
}
