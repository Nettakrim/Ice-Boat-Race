package com.nettakrim.ice_boat;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.commands.StartCommand;
import com.nettakrim.ice_boat.paths.BezierPath;
import com.nettakrim.ice_boat.paths.End;
import com.nettakrim.ice_boat.paths.Path;
import com.nettakrim.ice_boat.paths.Path.Approximation;

public class IceBoat extends JavaPlugin {
    public static IceBoat instance;

    private Random random = new Random();
    private int height;
    private ArrayList<Path> paths;

    private End defaultEnd = new End(new Vector2f(0,0), new Vector2f(1,0));

    private Path.RandomPathBuilder[] pathBuilders = {
        (End end, float lengthScale) -> BezierPath.buildRandom(random, end, lengthScale)
    };

    public BukkitTask[] levitationTimers;
    public Location[] lastSafeLocation;

    public void start(World world) {
        height = 256;
        paths = new ArrayList<Path>();
        int players = world.getPlayerCount();
        levitationTimers = new BukkitTask[players];
        lastSafeLocation = new Location[players];
        generateStart(world, players);
    }

    public static int getPlayerIndex(Player player) {
        return instance.getPlayerIndex(player.getUniqueId());
    }

    public int getPlayerIndex(UUID player) {
        return 0;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BoatListener(), this);

        this.getCommand("start").setExecutor(new StartCommand());

        instance = this;
    }

    public int getCurrentHeight() {
        return height;
    }

    public Path.RandomPathBuilder getPathBuilder() {
        return pathBuilders[random.nextInt(pathBuilders.length)];
    }

    public void generateIfLowEnough(World world, int testHeight) {
        if (testHeight <= height+1) {
            generate(world);
        }
    }

    public void generateStart(World world, int players) {
        float radius = 15f;

        BezierPath path = BezierPath.build(defaultEnd, 40f, new Vector2f(50f, (random.nextFloat()-0.5f)*20f));

        path.generate(world, radius, height);
        paths.add(path);

        int types = Boat.Type.values().length;

        for (float i = 0; i<players; i++) {
            float position = ((i-(((float)players-1)/2f))/Math.max(players-1, 1))*radius*1.5f;
            Boat boat = (Boat)world.spawnEntity(new Location(world, 0, height+1, position, -90, 0), EntityType.BOAT);
            boat.setBoatType(Boat.Type.values()[random.nextInt(types)]);
        }

        height--;
    }

    public void generate(World world) {
        float radius = FloatMath.clamp((((float)height)/5f)-40f, 2f, 12f);
        float turnWidth = 50;
        float safeZone = 50;
        int maxAttempts = 25;
        float lengthScale = 40;

        Path path = getRandomValidPath(radius, safeZone, turnWidth, maxAttempts, lengthScale);

        path.generate(world, radius, height);
        paths.add(path);

        height--;
    }

    private Path getRandomValidPath(float radius, float safeZone, float turnWidth, int maxAttempts, float lengthScale) {
        int attempts = 0;
        Path path = null;

        End lastEnd = null;
        if (paths.size() == 0) {
            lastEnd = defaultEnd;
        } else {
            lastEnd = paths.get(paths.size()-1).exit;
        }

        while(attempts < maxAttempts) {
            path = getPathBuilder().buildRandom(lastEnd, lengthScale);
            if (passChecks(path, safeZone, turnWidth, radius*2f)) {
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

    private boolean passChecks(Path path, float safeZone, float turnWidth, float distanceThreshold) {
        int size = paths.size();
        Approximation approximation = path.getApproximation();
        if (size >= 1) {
            if (!passTurnCheck(path, safeZone, turnWidth)) return false;

            float minimumDistance = approximation.minimumDistance(paths.get(size-1).getApproximation());
            if (minimumDistance < distanceThreshold) return false;
            if (size >= 2) {
                minimumDistance = approximation.minimumDistance(paths.get(size-2).getApproximation());
                if (minimumDistance < distanceThreshold) return false;
            }
        }
        return true;
    }

    private boolean passTurnCheck(Path path, float safeZone, float turnWidth) {
        Vector2f v = new Vector2f(path.entrance.point).normalize();
        float angle = v.dot(new Vector2f(new Vector2f(path.exit.point).sub(new Vector2f(path.entrance.point))).normalize());
        float length = path.exit.point.length();

        return angle < FloatMath.clamp(1-(length-safeZone)/turnWidth, -0.5f, 1);
    }
}
