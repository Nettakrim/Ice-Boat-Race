package com.nettakrim.ice_boat.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.IceBoat.GameState;

public class ConnectionListener implements Listener {

    //@EventHandler
	//public void onQuit(PlayerQuitEvent event) {
	//	Player player = event.getPlayer();
    //    if (IceBoat.instance.players.contains(player)) {
    //        if (IceBoat.gameState == GameState.PLAYING) {
    //            IceBoat.instance.killPlayer(player);
    //        } else {
    //            IceBoat.instance.waitingPlayerLeave(player);
    //            if (player.isInsideVehicle()) player.getVehicle().remove();
    //        }
    //    }
	//}

    //@EventHandler
	//public void onJoin(PlayerJoinEvent event) {
	//	Player player = event.getPlayer();
    //    if (IceBoat.gameState == GameState.PLAYING) {
    //        player.setGameMode(GameMode.SPECTATOR);
    //    } else {
    //        player.setGameMode(GameMode.ADVENTURE);
    //        player.teleport(new Location(player.getWorld(), 0, IceBoat.config.getDouble("spawnHeight"), 0));
    //    }
	//}
}
