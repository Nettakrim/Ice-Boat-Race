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
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom() == IceBoat.world) {
            removeFromGame(player);
        } else if (player.getWorld() == IceBoat.world) {
            joinGame(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() == IceBoat.world) {
            joinGame(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() == IceBoat.world) {
            removeFromGame(player);
        }
    }

    private void removeFromGame(Player player) {
        if (!IceBoat.instance.players.contains(player)) return;
        BoatListener.temporaryAllowDismount = true;

        if (IceBoat.gameState == GameState.PLAYING) {
            IceBoat.instance.killPlayer(player);
        } else {
            IceBoat.instance.waitingPlayerLeave(player);
            if (player.isInsideVehicle()) player.getVehicle().remove();
        }
        IceBoat.instance.progress.removePlayer(player);
        BoatListener.temporaryAllowDismount = false;
    }

    private void joinGame(Player player) {
        BoatListener.temporaryAllowDismount = true;
        if (IceBoat.gameState == GameState.PLAYING) {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(new Location(player.getWorld(), 0, IceBoat.config.getInt("startHeight")+5, 0));
            IceBoat.instance.progress.addPlayer(player);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(new Location(player.getWorld(), 0, IceBoat.config.getDouble("spawnHeight"), 0));
        }
        BoatListener.temporaryAllowDismount = false;
    }
}
