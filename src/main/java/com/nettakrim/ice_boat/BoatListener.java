package com.nettakrim.ice_boat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class BoatListener implements Listener {

    @EventHandler
    public void onEntityTeleport(PlayerTeleportEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            teleportInBoat((Boat)vehicle, player, event.getTo());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isInsideVehicle()) {
            Location location = player.getLocation();
            location.subtract(0, 1, 0);
            Block block = location.getWorld().getBlockAt(location);
            if (block.isSolid()) {
                IceBoat.instance.generateIfLowEnough(location.getWorld(), block.getY());
                IceBoat.instance.lastSafeLocation[IceBoat.getPlayerIndex(player)] = player.getVehicle().getLocation();
            }
        }
    }

    @EventHandler
    public void useItem(PlayerDropItemEvent event) {
        BukkitScheduler scheduler = Bukkit.getScheduler();

        Player player = event.getPlayer();
        if (!player.isInsideVehicle()) return;

        Material item = event.getItemDrop().getItemStack().getType();

        Entity vehicle = player.getVehicle();

        if(item == Material.FEATHER){
            if (!vehicle.isOnGround()) {
                Vector v = vehicle.getVelocity();
                v.setY(0);
                vehicle.setVelocity(v);
            }
            vehicle.setGravity(false);

            BukkitTask task = scheduler.runTaskLater(IceBoat.instance, () -> {
                vehicle.setGravity(true);
            }, 100L);

            int index = IceBoat.getPlayerIndex(player);
            BukkitTask previous = IceBoat.instance.levitationTimers[index];
            if (previous != null) {
                previous.cancel();
            }
            IceBoat.instance.levitationTimers[index] = task;

            event.getItemDrop().remove();

        } else if (item == Material.ENDER_PEARL) {
            if (vehicle.isOnGround()) {
                event.setCancelled(true);
                return;
            }

            int index = IceBoat.getPlayerIndex(player);
            Location location = IceBoat.instance.lastSafeLocation[index];

            teleportInBoat((Boat)vehicle, player, location);

            event.getItemDrop().remove();

        } else if (item == Material.INK_SAC) {
            for (Player other : player.getWorld().getPlayers()) {
                if (other != player && other.getLocation().distance(player.getLocation()) < 10) {
                    other.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, true, true, false));
                }
            }

            event.getItemDrop().remove();
        }
    }

    public void teleportInBoat(Boat old, Player player, Location location) {
        //spawning a new boat is probably not ideal, but i couldnt figure out how to not
        Boat newVehicle = (Boat)(player.getWorld().spawnEntity(location, EntityType.BOAT));
        newVehicle.setBoatType(old.getBoatType());
        old.remove();
        player.teleport(location);
        newVehicle.addPassenger(player);
    }
}