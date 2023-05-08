package com.nettakrim.ice_boat;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.items.LevitationEffect;
import com.nettakrim.ice_boat.listeners.BoatListener;
import com.nettakrim.ice_boat.listeners.ConnectionListener;
import com.nettakrim.ice_boat.listeners.ItemListener;
import com.nettakrim.ice_boat.paths.BezierPath;
import com.nettakrim.ice_boat.paths.End;
import com.nettakrim.ice_boat.paths.Path;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class IceBoat extends JavaPlugin {
    public static IceBoat instance;
    public static FileConfiguration config;
    public static World world;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BoatListener(), this);
        getServer().getPluginManager().registerEvents(new ItemListener(), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(), this);

        instance = this;

        config = this.getConfig();
        //generation
        config.addDefault("startHeight",     250);
        config.addDefault("endHeight",       230);
        config.addDefault("firstPathRadius", 15D);
        config.addDefault("pathRadiusStart", 12D);
        config.addDefault("pathRadiusEnd",   2D);
        config.addDefault("radiusShrink",    0.5D);
        config.addDefault("turnZoneStart",   50D);
        config.addDefault("turnZoneEnd",     100D);
        config.addDefault("lengthScale",     40D);
        config.addDefault("decaySpeed",      0.5D);
        config.addDefault("decayDistance",   4);

        //gameplay
        config.addDefault("countDownLength", 5);
        config.addDefault("minPlayers",      2);
        config.addDefault("spawnHeight",     320D);
        config.addDefault("deathDistance",   32);

        //items
        config.addDefault("levitationItems",         4);
        config.addDefault("levitationDuration",      100L);
        config.addDefault("teleporterItems",         5);
        config.addDefault("blindnessItems",          2);
        config.addDefault("blindnessLingerDuration", 300L);
        config.addDefault("blindnessEffectDuration", 40);

        config.addDefault("worldName", "world");

        config.options().copyDefaults(true);
        saveConfig();

        world = Bukkit.getWorld(config.getString("worldName"));
    }

    public Random random = new Random();

    public static GameState gameState = GameState.LOBBY;
    public ArrayList<Player> players;

    public enum GameState {
        LOBBY,
        WAITING,
        PLAYING,
        ENDING
    }

    private int height;
    private int startHeight;
    private int endHeight;
    private ArrayList<Path> paths;

    private End defaultEnd = new End(new Vector2f(0,0), new Vector2f(1,0));

    private Path.RandomPathBuilder[] pathBuilders = {
        (End end, float lengthScale) -> BezierPath.buildRandom(random, end, lengthScale)
    };

    public LevitationEffect[] levitationTimers;
    public Location[] lastSafeLocation;
    public ArrayList<UUID> playerIndexes;
    public BukkitTask pathDecay;

    private BukkitTask countDownTask;
    private float countDown;
    private float countDownLength;
    private BossBar progress;

    private boolean gameNearlyOver;
    private int deathDistance;

    public BukkitTask winParticles;

    private ArrayList<Boat> waitingBoats;

    public void teleportIntoGame(Player player) {
        if (gameState != GameState.WAITING) {
            startRound();
        }
  
        int types = Boat.Type.values().length;

        int boats = waitingBoats.size();
        float position = (((boats+1)/2) * FloatMath.sign(boats%2) * 3);
        int range = 10;
        Location location = new Location(world, ((int)position)/range, height+2, position%range, -90, 0);

        Boat boat = (Boat)world.spawnEntity(location, EntityType.BOAT);
        boat.setBoatType(Boat.Type.values()[random.nextInt(types)]);
        waitingBoats.add(boat);

        location.subtract(5, 0, 0);
        player.teleport(location);
        location.add(0,0.5,0);
        IceBoat.playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location, true);
        world.spawnParticle(Particle.REVERSE_PORTAL, location, 50);

        progress.addPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, true, false, false));
    }

    public void startRound() {
        gameState = GameState.WAITING;

        if (waitingBoats != null) {
            for (Boat boat : waitingBoats) {
                if (boat.getPassengers().size() == 0) {
                    boat.remove();
                }
            }
        }

        players = new ArrayList<Player>();
        waitingBoats = new ArrayList<Boat>();

        startHeight = config.getInt("startHeight");
        endHeight  = config.getInt("endHeight");
        height = startHeight;
        deathDistance = config.getInt("deathDistance");
        gameNearlyOver = false;

        paths = new ArrayList<Path>();
        generateStart();

        if (progress == null) {
            progress = Bukkit.createBossBar("Sit in a Boat!", BarColor.GREEN, BarStyle.SOLID);
        } else {
            progress.setTitle("Sit in a Boat!");
            progress.setVisible(true);
            progress.setProgress(1);
        }
    }

    public void waitingPlayerJoin(Player player) {
        players.add(player);
        if (players.size() == config.getInt("minPlayers")) {
            startCountdown();
        }
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.FEATHER, config.getInt("levitationItems")));
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, config.getInt("teleporterItems")));
        player.getInventory().addItem(new ItemStack(Material.INK_SAC, config.getInt("blindnessItems")));
    }

    public void waitingPlayerLeave(Player player) {
        if (players.size() == config.getInt("minPlayers")) {
            cancelCountdown();
        }
        players.remove(player);
        player.getInventory().clear();
    }

    public void startCountdown() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        countDownLength = config.getInt("countDownLength");
        countDown = countDownLength;
        countDownTask = scheduler.runTaskTimer(instance, () -> {
            countdownLoop();
        }, 20L, 20L);
    }

    public void cancelCountdown() {
        countDownTask.cancel();
        progress.setProgress(1);
    }

    public void countdownLoop() {
        countDown -= 1;
        progress.setProgress(countDown/countDownLength);
        if (countDown == 0) {
            countdownEnd();
        }
    }

    public void countdownEnd() {
        gameState = GameState.PLAYING;

        for (Player player : world.getPlayers()) {
            if (!player.isInsideVehicle()) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }

        for (Boat boat : waitingBoats) {
            if (boat.getPassengers().size() == 0) {
                boat.remove();
            }
        }

        int playerCount = players.size();
        levitationTimers = new LevitationEffect[playerCount];
        lastSafeLocation = new Location[playerCount];
        createPlayerIndexes();

        BukkitScheduler scheduler = Bukkit.getScheduler();
        pathDecay = scheduler.runTaskTimer(instance, () -> {pathDecay();}, 0L, 0L);

        progress.setTitle("GO!");
        progress.setProgress(0);
        generate();
        countDownTask.cancel();

        int expand = ((int)config.getInt("firstPathRadius"))+2;
        for (int x = -expand; x < expand; x++) {
            for (int y = -expand; y < expand; y++) {
                world.getBlockAt(x, startHeight+1, y).setType(Material.AIR);
                world.getBlockAt(x, startHeight+2, y).setType(Material.AIR);
            }
        }
    }

    public void endRound(Player winner, boolean reachedFinishLine) {
        gameState = GameState.ENDING;

        if (winner != null) {
            progress.setTitle(winner.getName()+" Won !");
            TextComponent textComponent;
            if (reachedFinishLine) {
                textComponent = Component.text(winner.getName()).append(Component.text(" Reached the Finish Line!"));
            } else {
                textComponent = Component.text("Leaving ").append(Component.text(winner.getName())).append(Component.text(" As the Winner!"));
            }
            world.sendMessage(textComponent);
        } else {
            progress.setTitle("Game Over !");
        }
        if (pathDecay != null) pathDecay.cancel();

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(instance, () -> {returnToLobby();}, 100L);

        if (playerIndexes != null && winner != null) {
            LevitationEffect levitation = levitationTimers[getPlayerIndex(winner)];
            if (!LevitationEffect.isFinished(levitation)) {
                levitation.cancel(false);
            }
        }

        if (winner != null) {
            Location location = winner.getLocation();
            location.add(0,1,0);
            world.spawnParticle(Particle.VILLAGER_HAPPY, location, 64, 4, 2, 4, 0.1, null, true);
            playSoundLocallyToAll(Sound.ENTITY_PLAYER_LEVELUP, location);
        }

        if (winParticles != null) winParticles.cancel();

        if (winner != null && winner.isInsideVehicle()) winner.getVehicle().setGravity(false);
        for (Player player : players) {
            if (player != winner) {
                player.setGameMode(GameMode.SPECTATOR);
                if (player.isInsideVehicle()) {
                    player.getVehicle().remove();
                }
            }
        }
    }

    public void returnToLobby() {
        gameState = GameState.LOBBY;
        for (Path path : paths) {
            path.clear(world);
        }
        progress.setVisible(false);
        double height = config.getDouble("spawnHeight");
        for (Player player : players) {
            if (player.isInsideVehicle()) {
                player.getVehicle().remove();
            }
        }
        for (Player player : world.getPlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(new Location(world, 0, height, 0));
            player.getInventory().clear();
        }
    }

    public void pathDecay() {
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

    public void createPlayerIndexes() {
        playerIndexes = new ArrayList<UUID>();
        for (Player player : players) {
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

    public void generateIfLowEnough(int testHeight, Player player) {
        if (testHeight <= height+1 && !gameNearlyOver) {
            float p = ((float)(height-endHeight))/((float)(startHeight-endHeight)-1);
            progress.setProgress(FloatMath.clamp(1-p, 0, 1));
            progress.setTitle(player.getName()+" is in The Lead");
            if (height > endHeight) generate();
            else gameNearlyOver = true;
        }
    }

    public void generateStart() {
        float radius = config.getInt("firstPathRadius");

        int expand = (int)radius+2;
        int minBorder = (int)((radius-1.5f)*(radius-1.5f));
        int maxBorder = (int)(radius*radius);
        for (int x = -expand; x < expand; x++) {
            for (int y = -expand; y < expand; y++) {
                int dist = x*x + y*y;
                if (dist > minBorder && dist < maxBorder) {
                    world.getBlockAt(x, startHeight+1, y).setType(Material.LIGHT_BLUE_STAINED_GLASS);
                    world.getBlockAt(x, startHeight+2, y).setType(Material.LIGHT_BLUE_STAINED_GLASS);
                }
            }
        }

        BezierPath path = BezierPath.build(defaultEnd, 40f, new Vector2f(50f, (random.nextFloat()-0.5f)*20f));

        path.generate(world, radius, height, 0.75f, false);
        paths.add(path);

        height--;
    }

    public void generate() {
        float radiusStart = (float)config.getDouble("pathRadiusStart");
        float radiusEnd = (float)config.getDouble("pathRadiusEnd");
        float radiusShrink = (float)config.getDouble("radiusShrink");
        float turnZoneStart = (float)config.getDouble("turnZoneStart");
        float turnZoneEnd = (float)config.getDouble("turnZoneEnd");
        float lengthScale = (float)config.getDouble("lengthScale");

        float t = (((float)(height-endHeight))/(startHeight-endHeight));
        float radius = FloatMath.lerpClamped(radiusEnd, radiusStart, FloatMath.clamp(FloatMath.lerp(t, t*t, radiusShrink), 0, 1));

        int maxAttempts = 25;
        Path path = getRandomValidPath(radius, turnZoneStart, turnZoneEnd-turnZoneStart, maxAttempts, lengthScale);

        boolean isFinishLine = height <= endHeight+1;
        path.generate(world, radius, height, 0.5f, isFinishLine);
        paths.add(path);

        Location location = new Location(world, path.exit.point.x+(path.exit.angle.x*radius*0.75f), height+2, path.exit.point.y+(path.exit.angle.y*radius*0.75f));
        if (isFinishLine) {
            winParticles = Bukkit.getScheduler().runTaskTimer(this, () -> {
                world.spawnParticle(Particle.TOTEM, location, 3, 2, 0.25, 2, 0.1, null, true);
            }, 0L, 0L);
        }

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

    public void killIfLowEnough(double testHeight, Player player) {
        if (testHeight < height-deathDistance) {
            killPlayer(player);
        }
    }

    public void killPlayer(Player player) {
        if (player.isInsideVehicle()) player.getVehicle().remove();
        player.setGameMode(GameMode.SPECTATOR);
        players.remove(player);
        world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 50, 0, 0, 0, 0.5, null, true);
        world.spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 10, 1, 1, 1, 1, null, true);
        playSoundLocallyToAll(Sound.ENTITY_GENERIC_EXPLODE, player.getLocation());

        TextComponent textComponent = Component.text(player.getName()).append(Component.text(" Exploded!"));
        world.sendMessage(textComponent);

        if (players.size() == 1) {
            endRound(players.get(0), false);
        } else if (players.size() == 0) {
            endRound(null, false);
        }
    }

    public static void playSoundGloballyToPlayer(Player player, Sound sound, Location location, boolean playLocallyToOthers) {
        player.playSound(location, sound, 1000, 1);
        if (!playLocallyToOthers) return;
        for (Player other : player.getWorld().getPlayers()) {
            if (other != player) {
                other.playSound(location, sound, 10, 1);
            }
        }
    }

    public static void playSoundLocallyToAll(Sound sound, Location location) {
        for (Player player : world.getPlayers()) {
            player.playSound(location, sound, 5, 1);
        }
    }
}
