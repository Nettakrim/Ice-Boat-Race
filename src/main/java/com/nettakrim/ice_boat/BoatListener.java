package com.nettakrim.ice_boat;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

public class BoatListener implements Listener {
    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        //event.setCancelled(true);
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
            }
        }
    }

    private HashMap<UUID,BukkitTask> levitationTimers = new HashMap<UUID,BukkitTask>();

    @EventHandler
    public void useJump(PlayerDropItemEvent event) {
        BukkitScheduler scheduler = Bukkit.getScheduler();

        if(event.getItemDrop().getItemStack().getType() == Material.FEATHER){
            Player player = event.getPlayer();
            
            if (player.isInsideVehicle()) {
                
                Entity vehicle = player.getVehicle();
                Vector v = vehicle.getVelocity();
                v.setY(0.1f);

                vehicle.setGravity(false);
                vehicle.setVelocity(v);

                event.getItemDrop().remove();

                BukkitTask task = scheduler.runTaskLater(IceBoat.instance, () -> {
                    vehicle.setGravity(true);
                }, 100L);

                BukkitTask previous = levitationTimers.put(player.getUniqueId(), task);
                if (previous != null) {
                    previous.cancel();
                }
            }
        }
    }
}