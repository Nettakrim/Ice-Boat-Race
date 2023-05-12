package com.nettakrim.ice_boat;

import java.util.ArrayList;
import java.util.HashMap;
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

import com.nettakrim.ice_boat.listeners.BoatListener;
import com.nettakrim.ice_boat.listeners.ConnectionListener;
import com.nettakrim.ice_boat.listeners.ItemListener;
import com.nettakrim.ice_boat.paths.BezierPath;
import com.nettakrim.ice_boat.paths.End;
import com.nettakrim.ice_boat.paths.Path;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.TitlePart;

public class IceBoat extends JavaPlugin {
    public World world;

    @Override
    public void onEnable() {
        new BoatListener(this);
        new ItemListener(this);
        new ConnectionListener(this);

        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        world = Bukkit.getWorld(getConfig().getString("world.worldName"));
    }

    public Random random = new Random();

    public GameState gameState = GameState.LOBBY;
    public ArrayList<Player> players;

    public static enum GameState {
        LOBBY,
        WAITING,
        PLAYING,
        ENDING
    }

    private int height;
    private int startHeight;
    private int endHeight;
    private int deathDistance;
    private ArrayList<Path> paths;

    private End defaultEnd = new End(new Vector2f(0,0), new Vector2f(1,0));

    private Path.RandomPathBuilder[] pathBuilders = {
        (End end, float lengthScale) -> BezierPath.buildRandom(random, end, lengthScale)
    };

    public HashMap<UUID, PlayerData> playerDatas;
    private BukkitTask pathDecay;

    private BukkitTask countDownTask;
    private int countDown;
    private float countDownLength;
    public BossBar progress;

    private boolean gameNearlyOver;

    private BukkitTask winParticles;

    private ArrayList<Boat> waitingBoats;

    public boolean temporaryAllowDismount = false;

    public void teleportIntoGame(Player player) {
        if (gameState != GameState.WAITING) {
            startRound();
        }
  
        Location location;
        int boats = waitingBoats.size();

        if (boats < world.getPlayerCount()) {
            int types = Boat.Type.values().length;
            int b = boats%9;
            float position = ((b+1)/2) * ((b%2)-0.5f) * 5;
            location = new Location(world, (-2.5f*(boats/9))+2.5f, height+2, position+0.5f, -90, 0);
            Boat boat = (Boat)world.spawnEntity(location, EntityType.BOAT);
            boat.setBoatType(Boat.Type.values()[random.nextInt(types)]);
            waitingBoats.add(boat);
        } else {
            location = waitingBoats.get(boats-1).getLocation();
        }

        location.subtract(5, 0, 0);
        player.teleport(location);
        location.add(0,0.5,0);
        playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location, true, 0.85f, 1.15f);
        world.spawnParticle(Particle.REVERSE_PORTAL, location, 50);

        progress.addPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, true, false, false));

        if (countDownTask != null && !countDownTask.isCancelled()) {
            setCountDownTime((int)(countDownLength/2), true);
        }
    }

    private void startRound() {
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

        startHeight = getConfig().getInt("game.startHeight");
        endHeight  = getConfig().getInt("game.endHeight");
        height = startHeight;
        deathDistance = getConfig().getInt("game.deathDistance");
        gameNearlyOver = false;

        paths = new ArrayList<Path>();
        generateStart();

        if (progress == null) {
            progress = Bukkit.createBossBar("Sit in a Boat!", BarColor.GREEN, BarStyle.SOLID);
        } else {
            progress.setTitle("Sit in a Boat!");
            progress.setProgress(1);
        }
    }

    public void waitingPlayerJoin(Player player) {
        players.add(player);
        if (players.size() == getConfig().getInt("game.minPlayers")) {
            startCountdown();
        }
        if (players.size() == world.getPlayerCount()) {
            if (countDownTask == null || countDownTask.isCancelled()) {
                startCountdown();
            }
            setCountDownTime(3, false);
        }
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.FEATHER, getConfig().getInt("items.levitationItems")));
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, getConfig().getInt("items.teleporterItems")));
        player.getInventory().addItem(new ItemStack(Material.INK_SAC, getConfig().getInt("items.blindnessItems")));
    }

    public void waitingPlayerLeave(Player player) {
        if (players.size() <= getConfig().getInt("game.minPlayers")) {
            cancelCountdown();
        }
        players.remove(player);
        player.getInventory().clear();
    }

    private void startCountdown() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        countDown = getConfig().getInt("game.countDownLength");
        countDownLength = countDown;
        countDownTask = scheduler.runTaskTimer(this, () -> {
            countdownLoop();
        }, 20L, 20L);
    }

    private void cancelCountdown() {
        countDownTask.cancel();
        progress.setProgress(1);
    }

    private void setCountDownTime(int seconds, boolean useMax) {
        if (useMax) {
            countDown = Math.max(seconds, countDown);
        } else {
            countDown = Math.min(seconds, countDown);
        }
        countDown++;
        countdownLoop();
    }

    private void countdownLoop() {
        countDown--;
        progress.setProgress(((float)countDown)/countDownLength);
        if (countDown == 0) {
            countdownEnd();
        } else if (countDown <= 3) {
            for (Player player : players) {
                player.sendTitlePart(TitlePart.TITLE, Component.text(countDown));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 5, random.nextFloat(1.5f, 1.6f)-(countDown/5f));
            }
        }
    }

    private void countdownEnd() {
        gameState = GameState.PLAYING;

        for (Player player : world.getPlayers()) {
            if (!player.isInsideVehicle()) {
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                player.sendTitlePart(TitlePart.TITLE, Component.text("GO!"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5, random.nextFloat(1.1f, 1.2f));
            }
        }

        for (Boat boat : waitingBoats) {
            if (boat.getPassengers().size() == 0) {
                boat.remove();
            }
        }

        int playerCount = players.size();
        playerDatas = new HashMap<UUID, PlayerData>(playerCount);
        for (Player player : players) {
            playerDatas.put(player.getUniqueId(), new PlayerData(player));
        }

        BukkitScheduler scheduler = Bukkit.getScheduler();
        float decaySpeed = (float)getConfig().getDouble("game.decaySpeed");
        int decayDistance = getConfig().getInt("game.decayDistance");
        pathDecay = scheduler.runTaskTimer(this, () -> {pathDecay(decaySpeed, decayDistance);}, 0L, 0L);

        progress.setTitle("GO!");
        progress.setProgress(0);
        generate();
        countDownTask.cancel();

        int expand = ((int)getConfig().getInt("generation.firstPathRadius"))+2;
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
                textComponent = Component.text("Leaving ").append(Component.text(winner.getName())).append(Component.text(" as the Winner!"));
            }
            world.sendMessage(textComponent);
        } else {
            progress.setTitle("Game Over !");
        }
        if (pathDecay != null) pathDecay.cancel();

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(this, () -> {returnToLobby();}, 100L);

        if (winner != null) {
            playerDatas.get(winner.getUniqueId()).cancelLevitation(true);
        }

        if (winner != null) {
            Location location = winner.getLocation();
            location.add(0,1,0);
            world.spawnParticle(Particle.VILLAGER_HAPPY, location, 64, 4, 2, 4, 0.1, null, true);
            playSoundLocallyToAll(Sound.ENTITY_PLAYER_LEVELUP, location, 0.95f, 1.05f);
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

    private void returnToLobby() {
        gameState = GameState.LOBBY;
        for (Path path : paths) {
            path.clear(world);
        }
        progress.removeAll();
        double height = getConfig().getDouble("world.spawnHeight");
        for (Player player : players) {
            if (player.isInsideVehicle()) {
                player.getVehicle().remove();
            }
        }
        for (Player player : world.getPlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            Location location = new Location(world, 0.5, height, 0.5, random.nextFloat(360), 0);
            location.subtract(location.getDirection().multiply(0.1));
            player.teleport(location);
            player.getInventory().clear();
        }
    }

    private void pathDecay(float decaySpeed, int decayDistance) {
        for (Path path : paths) {
            int offset = path.height-height;
            if (offset >= decayDistance) {
                float speed = decaySpeed*(offset/decayDistance);
                path.decay(random, world, speed, offset-decayDistance);
            }
        }
    }

    public void generateIfLowEnough(int testHeight, Player player) {
        if (testHeight <= height+1 && !gameNearlyOver) {
            float p = ((float)(height-endHeight))/((float)(startHeight-endHeight)-1);
            progress.setProgress(FloatMath.clamp(1-p, 0, 1));
            progress.setTitle(player.getName()+" is in The Lead");
            if (height > endHeight) {
                generate();
                playSoundLocallyToAll(Sound.BLOCK_NOTE_BLOCK_CHIME, player.getLocation(), 0.8f, 1.25f);
            } else {
                gameNearlyOver = true;
                playSoundLocallyToAll(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, player.getLocation(), 0.8f, 1.25f);
            }
        }
    }

    private void generateStart() {
        float radius = getConfig().getInt("generation.firstPathRadius");

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

    private void generate() {
        float radiusStart = (float)getConfig().getDouble("generation.pathRadiusStart");
        float radiusEnd = (float)getConfig().getDouble("generation.pathRadiusEnd");
        float radiusShrink = (float)getConfig().getDouble("generation.radiusShrink");
        float turnZoneStart = (float)getConfig().getDouble("generation.turnZoneStart");
        float turnZoneEnd = (float)getConfig().getDouble("generation.turnZoneEnd");
        float lengthScale = (float)getConfig().getDouble("generation.lengthScale");

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

    private Path.RandomPathBuilder getPathBuilder() {
        return pathBuilders[random.nextInt(pathBuilders.length)];
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
        playSoundLocallyToAll(Sound.ENTITY_GENERIC_EXPLODE, player.getLocation(), 0.9f, 1.1f);

        TextComponent textComponent = Component.text(player.getName()).append(Component.text(" Exploded!"));
        world.sendMessage(textComponent);

        if (players.size() == 1) {
            endRound(players.get(0), false);
        } else if (players.size() == 0) {
            endRound(null, false);
        }
    }

    public void playSoundGloballyToPlayer(Player player, Sound sound, Location location, boolean playLocallyToOthers, float minPitch, float maxPitch) {
        float pitch = random.nextFloat(minPitch, maxPitch);
        player.playSound(location, sound, 1000, pitch);
        if (!playLocallyToOthers) return;
        for (Player other : player.getWorld().getPlayers()) {
            if (other != player) {
                other.playSound(location, sound, 10, pitch);
            }
        }
    }

    public void playSoundLocallyToAll(Sound sound, Location location, float minPitch, float maxPitch) {
        for (Player player : world.getPlayers()) {
            player.playSound(location, sound, 5, random.nextFloat(minPitch, maxPitch));
        }
    }
}
