package com.nettakrim.ice_boat.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.IceBoat.GameState;
import com.nettakrim.ice_boat.items.LevitationEffect;

public class BoatListener implements Listener {

    private final IceBoat plugin;

    public BoatListener(IceBoat plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;

        if (plugin.gameState != GameState.WAITING) return;

        if (!(entity instanceof Player)) return;
        Player player = (Player)entity;
        plugin.waitingPlayerJoin(player);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;

        if (!(entity instanceof Player)) return;
        Player player = (Player)entity;

        if (plugin.gameState == GameState.WAITING) {
            plugin.waitingPlayerLeave(player);
        }

        if (!plugin.temporaryAllowDismount) {
            if (plugin.gameState == GameState.ENDING) {
                event.setCancelled(true);
            }

            if (plugin.gameState != GameState.PLAYING) return;

            event.setCancelled(true);
            int index = plugin.getPlayerIndex(player);
            LevitationEffect levitation = plugin.levitationTimers[index];
            if (!LevitationEffect.isFinished(levitation)) {
                levitation.cancel(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() != plugin.world) return;

        Location location = player.getLocation();
        location.subtract(0, 1, 0);
        Block block = location.getBlock();
        Material material = location.getBlock().getType();

        if (material == Material.SEA_LANTERN || material == Material.WARPED_PLANKS) {
            if (plugin.gameState == GameState.LOBBY || plugin.gameState == GameState.WAITING) {
                plugin.teleportIntoGame(player);
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(new Location(player.getWorld(), 0, plugin.getConfig().getInt("game.startHeight")+5, 0));
            }
        }

        if (plugin.gameState != GameState.PLAYING) return;

        if (player.isInsideVehicle()) {
            if (block.isSolid()) {
                plugin.generateIfLowEnough(block.getY(), player);
                if (material == Material.BLUE_ICE) {
                    plugin.lastSafeLocation[plugin.getPlayerIndex(player)] = player.getVehicle().getLocation();
                } else if (material == Material.LIME_WOOL) {
                    plugin.endRound(player, true);
                }
            } else {
                plugin.killIfLowEnough(location.getY(), player);
            }
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;

        if (plugin.gameState != GameState.PLAYING) return;

        if (event.getCause().equals(DamageCause.SUFFOCATION)) event.setCancelled(true);
    }
}