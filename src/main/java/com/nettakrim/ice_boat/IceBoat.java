package com.nettakrim.ice_boat;

import java.util.*;

import com.nettakrim.ice_boat.generation.Generation;
import com.nettakrim.ice_boat.items.Items;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import com.nettakrim.ice_boat.listeners.BoatListener;
import com.nettakrim.ice_boat.listeners.ConnectionListener;
import com.nettakrim.ice_boat.listeners.ItemListener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.TitlePart;

public class IceBoat extends JavaPlugin {
    public World world;

    public Generation generation;
    public Items items;

    @Override
    public void onEnable() {
        new BoatListener(this);
        new ItemListener(this);
        new ConnectionListener(this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        generation = new Generation(this);
        items = new Items(this);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        world = Bukkit.getWorld(getConfig().getString("world.worldName"));
    }

    public static final Random random = new Random();

    public GameState gameState = GameState.LOBBY;
    public ArrayList<Player> players;

    public enum GameState {
        LOBBY,
        WAITING,
        PLAYING,
        ENDING
    }

    private int deathDistance;


    public HashMap<UUID, PlayerData> playerDatas;

    private BukkitTask countDownTask;
    private int countDown;
    private float countDownLength;
    public BossBar progress;

    private ArrayList<Boat> waitingBoats;

    public ArrayList<BukkitRunnable> resetClearRunnables;
    public ArrayList<Entity> resetClearEntities;

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
            float position = (float)((b+1)/2) * ((b%2)-0.5f) * 5;
            location = new Location(world, (-2.5f*(float)(boats/9))+2.5f, generation.getCurrentHeight()+2, position+0.5f, -90, 0);
            Boat boat = (Boat)world.spawnEntity(location, EntityType.BOAT);
            boat.setBoatType(Boat.Type.values()[random.nextInt(types)]);
            waitingBoats.add(boat);
        } else {
            location = waitingBoats.get(boats-1).getLocation();
        }

        location.subtract(5, 0, 0);
        player.teleport(location);
        location.add(0,1,0);
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

        players = new ArrayList<>();
        waitingBoats = new ArrayList<>();
        resetClearRunnables = new ArrayList<>();
        resetClearEntities = new ArrayList<>();

        deathDistance = getConfig().getInt("game.deathDistance");

        generation.generateStartingArea();

        if (progress == null) {
            progress = Bukkit.createBossBar("Sit in a Boat!", BarColor.GREEN, BarStyle.SOLID);
        } else {
            progress.setTitle("Sit in a Boat!");
            progress.setProgress(1);
        }

        items.createItemPool();
    }

    public void waitingPlayerJoin(Player player, Entity vehicle) {
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

        items.giveStartingItems(player, vehicle);
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
        countDownTask = scheduler.runTaskTimer(this, this::countdownLoop, 20L, 20L);
    }

    private void cancelCountdown() {
        if (countDownTask != null) {
            countDownTask.cancel();
        }
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
        playerDatas = new HashMap<>(playerCount);
        for (Player player : players) {
            playerDatas.put(player.getUniqueId(), new PlayerData(player));
        }

        generation.roundStart();

        progress.setTitle("GO!");
        progress.setProgress(0);
        countDownTask.cancel();
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

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(this, this::returnToLobby, 100L);

        for (PlayerData playerData : playerDatas.values()) {
            playerData.clear();
        }

        if (winner != null) {
            Location location = winner.getLocation();
            location.add(0,1,0);
            world.spawnParticle(Particle.VILLAGER_HAPPY, location, 64, 4, 2, 4, 0.1, null, true);
            playSoundLocallyToAll(Sound.ENTITY_PLAYER_LEVELUP, location, 0.95f, 1.05f);

            if (winner.isInsideVehicle()) winner.getVehicle().setGravity(false);
        }

        for (Player player : players) {
            if (player != winner) {
                player.setGameMode(GameMode.SPECTATOR);
                if (player.isInsideVehicle()) {
                    player.getVehicle().remove();
                }
            }
        }

        generation.roundEnd();
    }

    private void returnToLobby() {
        gameState = GameState.LOBBY;
        generation.clearMap();
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

        for (Entity entity : resetClearEntities) {
            entity.remove();
        }
        for (BukkitRunnable runnable : resetClearRunnables) {
            runnable.cancel();
        }
    }

    public void updatePlayerPosition(Player player, Block block, Material material, Location location) {
        if (block.isSolid()) {
            generation.generateIfLowEnough(block.getY(), player);
            if (material == Material.BLUE_ICE) {
                playerDatas.get(player.getUniqueId()).lastSafeLocation = player.getVehicle().getLocation();
            } else if (material == Material.LIME_WOOL) {
                endRound(player, true);
            }
        } else {
            killIfLowEnough(location.getY(), player);
        }
    }

    private void killIfLowEnough(double testHeight, Player player) {
        if (testHeight < generation.getCurrentHeight()-deathDistance) {
            killPlayer(player, true);
        }
    }

    public void killPlayer(Player player, boolean explode) {
        killPlayer(player, explode, Component.text(player.getName()).append(Component.text(" Exploded!")));
    }

    public void killPlayer(Player player, boolean explode, TextComponent message) {
        if (player.isInsideVehicle()) {
            Entity vehicle = player.getVehicle();
            //save the second passenger
            if (vehicle.getPassengers().size() > 1) {
                Entity other = vehicle.getPassengers().get(1);
                Location location = playerDatas.get(player.getUniqueId()).lastSafeLocation;
                other.teleport(location);
                Boat newVehicle = (Boat)(vehicle.getWorld().spawnEntity(location, EntityType.BOAT));
                if (other instanceof Player otherPlayer) {
                    for (ItemStack itemStack : otherPlayer.getInventory().getContents()) {
                        otherPlayer.getInventory().addItem(itemStack);
                    }
                    teleportEffect(location, otherPlayer);
                }
                newVehicle.setBoatType(((Boat)vehicle).getBoatType());
                newVehicle.addPassenger(other);
            }
            vehicle.remove();
        }

        playerDatas.get(player.getUniqueId()).cancelLevitation(false, null);

        player.setGameMode(GameMode.SPECTATOR);
        players.remove(player);
        player.getInventory().clear();

        if (explode) {
            Location location = player.getLocation().add(0, 1, 0);
            world.spawnParticle(Particle.SMOKE_LARGE, location, 50, 0, 0, 0, 0.5, null, true);
            world.spawnParticle(Particle.EXPLOSION_LARGE, location, 10, 1, 1, 1, 1, null, true);
            playSoundLocallyToAll(Sound.ENTITY_GENERIC_EXPLODE, location, 0.9f, 1.1f);
        }

        if (message != null) {
            world.sendMessage(message);
        }

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

    public void teleportEffect(Location location, Player player) {
        Location up = location.clone().add(0,0.5,0);
        playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location, true, 0.85f, 1.15f);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, up, 50);
    }
}
