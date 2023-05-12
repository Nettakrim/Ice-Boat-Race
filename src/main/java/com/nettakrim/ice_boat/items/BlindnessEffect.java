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

    private final IceBoat plugin;

    private final Player owner;
    private final Location location;
    private final World world;

    private long duration;
    private BukkitTask loopTask;

    private boolean finished;

    private double rangeX = 3.5;
    private double rangeY = 1;
    private int effectDuration;

    public BlindnessEffect(IceBoat plugin, Player player, long startup, long duration, int effectDuration) {
        this.plugin = plugin;
        this.owner = player;
        this.location = player.getLocation();
        this.world = player.getWorld();
        location.add(0, rangeY*1.5, 0);
        run(startup, duration, effectDuration);
    }

    public void run(long startup, long duration, int effectDuration) {
        this.duration = duration;
        this.effectDuration = effectDuration;

        plugin.playSoundGloballyToPlayer(owner, Sound.ENTITY_SQUID_SQUIRT, location, true, 0.9f, 1.1f);

        world.spawnParticle(Particle.SQUID_INK, location, 50, 0.25, rangeY, 0.25, 0, null, true);

        this.loopTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            loop();
        }, startup, 0L);
    }

    public void loop() {
        world.spawnParticle(Particle.SQUID_INK, location, 5, rangeX, rangeY, rangeX, 0, null, true);

        for (Player other : plugin.players) {
            if (other == owner) continue;
            Location offset = other.getLocation().subtract(location);
            if (Math.abs(offset.getY()+rangeY) < rangeY && Math.abs(offset.getX()) < rangeX) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, effectDuration, 0, true, true, false));
                other.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, effectDuration, 0, true, true, false));
            }
        }

        if (plugin.random.nextFloat() < 0.1f) {
            plugin.playSoundLocallyToAll(Sound.ENTITY_SQUID_AMBIENT, location, 0.75f, 1.25f);
        }

        duration--;
        if (duration <= 0) {
            loopTask.cancel();
            end();
        }
    }

    public void end() {
        plugin.playSoundLocallyToAll(Sound.BLOCK_CONDUIT_DEACTIVATE, location, 0.9f, 1.1f);
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
