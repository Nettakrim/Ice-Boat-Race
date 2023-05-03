package com.nettakrim.ice_boat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityDismountEvent;

public class BoatListener implements Listener {
    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        //event.setCancelled(true);
    }
}