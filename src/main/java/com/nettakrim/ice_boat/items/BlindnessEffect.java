package com.nettakrim.ice_boat.items;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import com.nettakrim.ice_boat.IceBoat;

public class BlindnessEffect {
    private final Player owner;
    private final Location location;

    private long duration;
    private BukkitTask loopTask;

    private boolean finished;

    private double rangeX = 3.5;
    private double rangeY = 1;

    public BlindnessEffect(Player player, long startup, long duration) {
        this.owner = player;
        this.location = player.getLocation();
        location.add(0, rangeY/2, 0);
        run(startup, duration);
    }

    public void run(long startup, long duration) {
        this.duration = duration;

        IceBoat.playSoundGloballyToPlayer(owner, Sound.ENTITY_SQUID_SQUIRT, location);

        World world = owner.getWorld();

        world.spawnParticle(Particle.SQUID_INK, location, 50, 0.25, rangeY, 0.25, 0, null, true);

        this.loopTask = Bukkit.getScheduler().runTaskTimer(IceBoat.instance, () -> {
            loop(world);
        }, startup, 0L);
    }

    public void loop(World world) {
        world.spawnParticle(Particle.SQUID_INK, location, 5, rangeX, rangeY, rangeX, 0, null, true);

        for (Player other : owner.getWorld().getPlayers()) {
            Location offset = other.getLocation().subtract(location);
            if (Math.abs(offset.getY()) < rangeY && Math.abs(offset.getX()) < rangeX) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, true, true, false));
            }
        }

        if (IceBoat.instance.random.nextFloat() < 0.1f) {
            IceBoat.playSoundLocallyToAll(world, Sound.ENTITY_SQUID_AMBIENT, location);
        }

        duration--;
        if (duration <= 0) {
            loopTask.cancel();
            end();
        }
    }

    public void end() {
        IceBoat.playSoundLocallyToAll(owner.getWorld(), Sound.BLOCK_CONDUIT_DEACTIVATE, location);
        finished = true;
    }

    public void cancel() {
        loopTask.cancel();
        end();
    }

    public static boolean isFinished(BlindnessEffect effect) {
        return effect == null || effect.finished;
    }
}
