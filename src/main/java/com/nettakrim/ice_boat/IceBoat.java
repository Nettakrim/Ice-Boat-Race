package com.nettakrim.ice_boat;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.commands.ResetCommand;
import com.nettakrim.ice_boat.commands.StartCommand;
import com.nettakrim.ice_boat.items.LevitationEffect;
import com.nettakrim.ice_boat.paths.BezierPath;
import com.nettakrim.ice_boat.paths.End;
import com.nettakrim.ice_boat.paths.Path;

public class IceBoat extends JavaPlugin {
    public static IceBoat instance;
    public static FileConfiguration config;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BoatListener(), this);

        this.getCommand("start").setExecutor(new StartCommand());
        this.getCommand("reset").setExecutor(new ResetCommand());

        instance = this;

        config = this.getConfig();
        config.addDefault("startHeight",     256);
        config.addDefault("endHeight",       200);
        config.addDefault("firstPathRadius", 15D);
        config.addDefault("pathRadiusStart", 12D);
        config.addDefault("pathRadiusEnd",   2D);
        config.addDefault("radiusShrink",    0.5D);
        config.addDefault("turnZoneStart",   50D);
        config.addDefault("turnZoneEnd",     100D);
        config.addDefault("lengthScale",     40D);
        config.addDefault("decaySpeed",      0.5D);
        config.addDefault("decayDistance",   4);
        config.options().copyDefaults(true);
        saveConfig();
    }

    public Random random = new Random();
    private int height;
    private ArrayList<Path> paths;

    private End defaultEnd = new End(new Vector2f(0,0), new Vector2f(1,0));

    private Path.RandomPathBuilder[] pathBuilders = {
        (End end, float lengthScale) -> BezierPath.buildRandom(random, end, lengthScale)
    };

    public LevitationEffect[] levitationTimers;
    public Location[] lastSafeLocation;
    public ArrayList<UUID> playerIndexes;
    public BukkitTask pathDecay;

    public void start(World world) {
        height = config.getInt("startHeight");
        paths = new ArrayList<Path>();
        int players = world.getPlayerCount();
        levitationTimers = new LevitationEffect[players];
        lastSafeLocation = new Location[players];
        createPlayerIndexes(world, players);
        generateStart(world, players);

        BukkitScheduler scheduler = Bukkit.getScheduler();
        pathDecay = scheduler.runTaskTimer(instance, () -> {pathDecay(world);}, 0L, 0L);
    }

    public void clear(World world) {
        for (Path path : paths) {
            path.clear(world);
        }
        pathDecay.cancel();
    }

    public void pathDecay(World world) {
        float decaySpeed = (float)config.getDouble("decaySpeed");
        int decayDistance = config.getInt("decayDistance");
        for (Path path : paths) {
            int offset = path.height-height;
            if (offset >= decayDistance) {
                float speed = decaySpeed*(offset/decayDistance);
                path.decay(random, world, speed, offset-decayDistance);
            }
        }
    }

    public static int getPlayerIndex(Player player) {
        return instance.getPlayerIndex(player.getUniqueId());
    }

    public void createPlayerIndexes(World world, int players) {
        playerIndexes = new ArrayList<UUID>();
        for (Player player : world.getPlayers()) {
            playerIndexes.add(player.getUniqueId());
        }
    }

    public int getPlayerIndex(UUID player) {
        return playerIndexes.indexOf(player);
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
        float radius = config.getInt("firstPathRadius");

        BezierPath path = BezierPath.build(defaultEnd, 40f, new Vector2f(50f, (random.nextFloat()-0.5f)*20f));

        path.generate(world, radius, height, 0.75f);
        paths.add(path);

        int types = Boat.Type.values().length;

        for (float i = 0; i<players; i++) {
            float position = ((i-(((float)players-1)/2f))/Math.max(players-1, 1))*radius*1.4f;
            Boat boat = (Boat)world.spawnEntity(new Location(world, 0, height+1, position, -90, 0), EntityType.BOAT);
            boat.setBoatType(Boat.Type.values()[random.nextInt(types)]);
        }

        height--;
    }

    public void generate(World world) {
        float radiusStart = (float)config.getDouble("pathRadiusStart");
        float radiusEnd = (float)config.getDouble("pathRadiusEnd");
        float radiusShrink = (float)config.getDouble("radiusShrink");
        int startHeight = config.getInt("startHeight");
        int endHeight = config.getInt("endHeight");
        float turnZoneStart = (float)config.getDouble("turnZoneStart");
        float turnZoneEnd = (float)config.getDouble("turnZoneEnd");
        float lengthScale = (float)config.getDouble("lengthScale");

        float t = (((float)(height-endHeight))/(startHeight-endHeight));
        float radius = FloatMath.lerpClamped(radiusEnd, radiusStart, FloatMath.clamp(FloatMath.lerp(t, t*t, radiusShrink), 0, 1));

        int maxAttempts = 25;
        Path path = getRandomValidPath(radius, turnZoneStart, turnZoneEnd-turnZoneStart, maxAttempts, lengthScale);

        path.generate(world, radius, height, 0.5f);
        paths.add(path);

        height--;
    }

    private Path getRandomValidPath(float radius, float turnZoneStart, float turnZoneSize, int maxAttempts, float lengthScale) {
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
            if (passTurnCheck(path, turnZoneStart, turnZoneSize) && passDistanceCheck(path, turnZoneStart, turnZoneSize, radius*(1.25f-((float)attempts)/maxAttempts)*2)) {
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

    private boolean passDistanceCheck(Path path, float turnZoneStart, float turnZoneSize, float distanceThreshold) {
        int size = paths.size();
        if (size >= 1) {
            float minimumDistance = path.approximation.minimumDistance(paths.get(size-1).approximation);
            if (minimumDistance < distanceThreshold) return false;
            if (size >= 2) {
                minimumDistance = path.approximation.minimumDistance(paths.get(size-2).approximation);
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

    public static void playSoundGloballyToPlayer(Player player, Sound sound, Location location) {
        player.playSound(location, sound, 1000, 1);
        for (Player other : player.getWorld().getPlayers()) {
            if (other != player) {
                other.playSound(location, sound, 10, 1);
            }
        }
    }

    public static void playSoundLocallyToAll(World world, Sound sound, Location location) {
        for (Player player : world.getPlayers()) {
            player.playSound(location, sound, 5, 1);
        }
    }
}
