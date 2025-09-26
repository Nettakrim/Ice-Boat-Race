package com.nettakrim.ice_boat.generation;

import com.nettakrim.ice_boat.FloatMath;
import com.nettakrim.ice_boat.IceBoat;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector2f;

import java.util.ArrayList;

public class Generation {
    private final IceBoat plugin;

    public Generation(IceBoat plugin) {
        this.plugin = plugin;
    }

    private int height;
    private int startHeight;
    private int endHeight;

    private ArrayList<Path> paths;
    private BukkitTask pathDecay;

    private final End defaultEnd = new End(new Vector2f(0,0), new Vector2f(1,0));

    private final Path.RandomPathBuilder[] pathBuilders = {
            (End end, float lengthScale) -> BezierPath.buildRandom(IceBoat.random, end, lengthScale)
    };

    private boolean finishLineSpawned;

    private BukkitTask winParticles;


    public int getCurrentHeight() {
        return  height;
    }

    public Path getPath(int height) {
        for (Path path : paths) {
            if (path.height == height) {
                return path;
            }
        }
        return null;
    }

    private void pathDecay(float decaySpeed, int decayDistance) {
        for (Path path : paths) {
            int offset = path.height-height;
            if (offset >= decayDistance) {
                float speed = decaySpeed*(float)(offset/decayDistance);
                path.decay(IceBoat.random, plugin.world, speed, offset-decayDistance);
            }
        }
    }

    public void generateIfLowEnough(int testHeight, Player player) {
        if (testHeight <= height+1 && !finishLineSpawned) {
            float p = ((float)(height-endHeight))/((float)(startHeight-endHeight)-1);
            plugin.progress.setProgress(FloatMath.clamp(1-p, 0, 1));
            plugin.progress.setTitle(player.getName()+" is in The Lead");
            if (height > endHeight) {
                generate();
                plugin.playSoundLocallyToAll(Sound.BLOCK_NOTE_BLOCK_CHIME, player.getLocation(), 0.8f, 1.25f);
            } else {
                finishLineSpawned = true;
                plugin.playSoundLocallyToAll(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, player.getLocation(), 0.8f, 1.25f);
            }
        }
    }

    public void generateStartingArea() {
        startHeight = plugin.getConfig().getInt("game.startHeight");
        endHeight = plugin.getConfig().getInt("game.endHeight");
        height = startHeight;
        finishLineSpawned = false;

        paths = new ArrayList<>();

        float radius = plugin.getConfig().getInt("generation.firstPathRadius");

        BezierPath path = BezierPath.build(defaultEnd, 40f, new Vector2f(50f, (IceBoat.random.nextFloat()-0.5f)*20f));

        path.generate(plugin.world, radius, height, 0.75f, false);
        paths.add(path);

        int expand = (int)radius+2;
        int minBorder = (int)((radius-1.5f)*(radius-1.5f));
        int maxBorder = (int)(radius*radius);
        for (int x = -expand; x < expand; x++) {
            for (int y = -expand; y < expand; y++) {
                int dist = x*x + y*y;
                if (dist > minBorder && dist < maxBorder) {
                    plugin.world.getBlockAt(x, startHeight+1, y).setType(Material.LIGHT_BLUE_STAINED_GLASS);
                    plugin.world.getBlockAt(x, startHeight+2, y).setType(Material.LIGHT_BLUE_STAINED_GLASS);
                }
            }
        }

        height--;
    }

    public void roundStart() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        float decaySpeed = (float)plugin.getConfig().getDouble("game.decaySpeed");
        int decayDistance = plugin.getConfig().getInt("game.decayDistance");
        pathDecay = scheduler.runTaskTimer(plugin, () -> pathDecay(decaySpeed, decayDistance), 0L, 0L);

        generate();

        int expand = plugin.getConfig().getInt("generation.firstPathRadius") +2;
        for (int x = -expand; x < expand; x++) {
            for (int y = -expand; y < expand; y++) {
                plugin.world.getBlockAt(x, startHeight+1, y).setType(Material.AIR);
                plugin.world.getBlockAt(x, startHeight+2, y).setType(Material.AIR);
            }
        }
    }

    public void roundEnd() {
        if (pathDecay != null) {
            pathDecay.cancel();
        }

        if (winParticles != null) {
            winParticles.cancel();
        }
    }

    public void clearMap() {
        for (Path path : paths) {
            path.clear(plugin.world);
        }
    }

    private void generate() {
        float radiusStart = (float)plugin.getConfig().getDouble("generation.pathRadiusStart");
        float radiusEnd = (float)plugin.getConfig().getDouble("generation.pathRadiusEnd");
        float radiusShrink = (float)plugin.getConfig().getDouble("generation.radiusShrink");
        float turnZoneStart = (float)plugin.getConfig().getDouble("generation.turnZoneStart");
        float turnZoneEnd = (float)plugin.getConfig().getDouble("generation.turnZoneEnd");
        float lengthScale = (float)plugin.getConfig().getDouble("generation.lengthScale");

        float t = (((float)(height-endHeight))/(startHeight-endHeight));
        float radius = FloatMath.lerpClamped(radiusEnd, radiusStart, FloatMath.clamp(FloatMath.lerp(t, t*t, radiusShrink), 0, 1));

        int maxAttempts = 25;
        Path path = getRandomValidPath(radius, turnZoneStart, turnZoneEnd-turnZoneStart, maxAttempts, lengthScale);

        boolean isFinishLine = height <= endHeight+1;
        path.generate(plugin.world, radius, height, 0.5f, isFinishLine);
        paths.add(path);

        if (isFinishLine) {
            Location location = new Location(plugin.world, path.exit.point.x+(path.exit.angle.x*radius*0.75f), height+2, path.exit.point.y+(path.exit.angle.y*radius*0.75f));
            winParticles = Bukkit.getScheduler().runTaskTimer(plugin, () -> plugin.world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 3, 2, 0.25, 2, 0.1, null, true), 0L, 0L);
        }

        if (height > endHeight+2) {
            Vector2f pos = path.getPosition(IceBoat.random.nextFloat(0.3f, 0.7f));
            plugin.items.tryCreateItemBox(pos, turnZoneEnd, height);
        }

        height--;
    }

    private Path.RandomPathBuilder getPathBuilder() {
        return pathBuilders[IceBoat.random.nextInt(pathBuilders.length)];
    }

    private Path getRandomValidPath(float radius, float turnZoneStart, float turnZoneSize, int maxAttempts, float lengthScale) {
        int attempts = 0;
        Path path = null;

        End lastEnd;
        if (paths.isEmpty()) {
            lastEnd = defaultEnd;
        } else {
            lastEnd = paths.get(paths.size()-1).exit;
        }

        while(attempts < maxAttempts) {
            path = getPathBuilder().buildRandom(lastEnd, lengthScale);
            if (path.passGenerationChecks(paths, turnZoneStart, turnZoneSize, radius*(1.25f-((float)attempts)/maxAttempts)*2)) {
                return path;
            } else {
                attempts++;
                if (attempts == maxAttempts) {
                    height-=2;
                }
            }
        }
        return path;
    }
}
