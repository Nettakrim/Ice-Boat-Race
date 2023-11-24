package com.nettakrim.ice_boat.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.IceBoat.GameState;

public class ConnectionListener implements Listener {

    private final IceBoat plugin;

    public ConnectionListener(IceBoat plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom() == plugin.world) {
            removeFromGame(player);
        } else if (player.getWorld() == plugin.world) {
            joinGame(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() == plugin.world) {
            joinGame(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() == plugin.world) {
            removeFromGame(player);
        }
    }

    private void removeFromGame(Player player) {
        if (plugin.progress != null) {
            plugin.progress.removePlayer(player);
        }
        if (!plugin.players.contains(player)) return;
        plugin.temporaryAllowDismount = true;

        if (plugin.gameState == GameState.PLAYING) {
            plugin.killPlayer(player, true);
        } else {
            plugin.waitingPlayerLeave(player);
            if (player.isInsideVehicle()) player.getVehicle().remove();
        }
        plugin.temporaryAllowDismount = false;
    }

    private void joinGame(Player player) {
        plugin.temporaryAllowDismount = true;
        if (plugin.gameState == GameState.PLAYING) {
            plugin.progress.addPlayer(player);
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(new Location(player.getWorld(), 0.5, plugin.getConfig().getDouble("world.spawnHeight"), 0.5, -90, 0));
        plugin.temporaryAllowDismount = false;
    }
}
