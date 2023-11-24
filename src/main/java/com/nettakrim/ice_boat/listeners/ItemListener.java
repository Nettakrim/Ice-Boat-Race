package com.nettakrim.ice_boat.listeners;

import com.nettakrim.ice_boat.items.SnowballTrail;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.IceBoat.GameState;

public class ItemListener implements Listener {
    
    private final IceBoat plugin;

    public ItemListener(IceBoat plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() != plugin.world || event.getTo().getWorld() != plugin.world) return;
        
        if (plugin.gameState != GameState.PLAYING) {
            return;
        }
        
        if (player.isInsideVehicle()) {
            Entity vehicle = player.getVehicle();
            plugin.temporaryAllowDismount = true;
            event.setCancelled(true);
            plugin.items.teleportBoatWithPassengers((Boat)vehicle, event.getTo());
            plugin.teleportEffect(event.getTo(), player);
            plugin.temporaryAllowDismount = false;
        }
    }

    @EventHandler
    public void useItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() != plugin.world) return;

        if (plugin.gameState != GameState.PLAYING) {
            event.setCancelled(true);
            return;
        }
        if (!player.isInsideVehicle()) return;

        Material item = event.getItemDrop().getItemStack().getType();

        Entity vehicle = player.getVehicle();

        plugin.items.useItem(item, player, vehicle, event);

        if (event.isCancelled()) return;

        int amount = event.getItemDrop().getItemStack().getAmount();
        if (amount > 1) player.getInventory().addItem(new ItemStack(item, amount-1));

        event.getItemDrop().remove();
    }

    @EventHandler
    public void cancelCraft(CraftItemEvent event) {
        if (event.getWhoClicked().getWorld() != plugin.world) return;
        if (plugin.getConfig().getBoolean("items.disableCraftingSecret")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void throwItem(ProjectileLaunchEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;

        if (plugin.gameState != GameState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (entity instanceof Snowball snowball) {
            SnowballTrail snowballTrail = new SnowballTrail(plugin, snowball);
            snowballTrail.runTaskTimer(plugin, 5L, 0L);
            plugin.resetClearEntities.add(entity);
        }
    }
}
