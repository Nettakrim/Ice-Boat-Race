package com.nettakrim.ice_boat.listeners;

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

    public static boolean temporaryAllowDismount = false;

    @EventHandler
    public void onMount(EntityMountEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != IceBoat.world) return;

        if (IceBoat.gameState != GameState.WAITING) return;

        if (!(entity instanceof Player)) return;
        Player player = (Player)entity;
        IceBoat.instance.waitingPlayerJoin(player);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != IceBoat.world) return;

        if (!(entity instanceof Player)) return;
        Player player = (Player)entity;

        if (IceBoat.gameState == GameState.WAITING) {
            IceBoat.instance.waitingPlayerLeave(player);
        }

        if (IceBoat.gameState == GameState.ENDING) {
            event.setCancelled(true);
        }

        if (IceBoat.gameState != GameState.PLAYING) return;
        
        if (!temporaryAllowDismount) {
            event.setCancelled(true);
            int index = IceBoat.getPlayerIndex(player);
            LevitationEffect levitation = IceBoat.instance.levitationTimers[index];
            if (!LevitationEffect.isFinished(levitation)) {
                levitation.cancel(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() != IceBoat.world) return;

        if (IceBoat.gameState == GameState.LOBBY || IceBoat.gameState == GameState.WAITING) {
            Location location = player.getLocation();
            location.subtract(0, 1, 0);
            Material material = location.getBlock().getType();
            if (material == Material.SEA_LANTERN || material == Material.WARPED_PLANKS) {
                IceBoat.instance.teleportIntoGame(player);
            }
        }

        if (IceBoat.gameState != GameState.PLAYING) return;

        if (player.isInsideVehicle()) {
            Location location = player.getLocation();
            location.subtract(0, 1, 0);
            Block block = location.getBlock();
            if (block.isSolid()) {
                IceBoat.instance.generateIfLowEnough(location.getWorld(), block.getY(), player);
                Material material = block.getType();
                if (material == Material.BLUE_ICE) {
                    IceBoat.instance.lastSafeLocation[IceBoat.getPlayerIndex(player)] = player.getVehicle().getLocation();
                } else if (material == Material.LIME_WOOL) {
                    IceBoat.instance.endRound(player.getWorld(), player);
                }
            } else {
                IceBoat.instance.killIfLowEnough(location.getY(), player);
            }
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != IceBoat.world) return;

        if (IceBoat.gameState != GameState.PLAYING) return;

        if (event.getCause().equals(DamageCause.SUFFOCATION)) event.setCancelled(true);
    }
}