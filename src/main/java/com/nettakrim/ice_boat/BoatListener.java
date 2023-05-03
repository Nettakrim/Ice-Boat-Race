package com.nettakrim.ice_boat;

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

    @EventHandler
    public void useJump(PlayerDropItemEvent event) {
 
        Bukkit.getLogger().info("hi!");
        
        if(event.getItemDrop().getItemStack().getType() == Material.FEATHER){
            Player player = event.getPlayer();
            if (player.isInsideVehicle()) {
                Bukkit.getLogger().info("hi2!");
                
                Entity vehicle = player.getVehicle();
                Vector v = vehicle.getVelocity();
                v.setY(1);
                vehicle.setVelocity(v);

                event.getItemDrop().remove();
            }
        }
    }
}