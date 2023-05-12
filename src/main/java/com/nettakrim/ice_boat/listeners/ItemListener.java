package com.nettakrim.ice_boat.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.PlayerData;
import com.nettakrim.ice_boat.IceBoat.GameState;
import com.nettakrim.ice_boat.items.BlindnessEffect;
import com.nettakrim.ice_boat.items.LevitationEffect;

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
            if (player.isInsideVehicle()) {
                event.setCancelled(true);
            }
            return;
        }
        
        if (player.isInsideVehicle()) {
            Entity vehicle = player.getVehicle();
            plugin.temporaryAllowDismount = true;
            event.setCancelled(true);
            teleportInBoat((Boat)vehicle, player, event.getTo());
            teleportEffect(event.getTo(), player);
            plugin.temporaryAllowDismount = false;
        }
    }

    @EventHandler
    public void useItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() != plugin.world) return;

        if (plugin.gameState == GameState.WAITING) {
            event.setCancelled(true);
            return;
        }
        if (plugin.gameState != GameState.PLAYING) return;
        if (!player.isInsideVehicle()) return;

        Material item = event.getItemDrop().getItemStack().getType();

        Entity vehicle = player.getVehicle();

        if(item == Material.FEATHER){
            PlayerData playerData = plugin.playerDatas.get(player.getUniqueId());
            playerData.cancelLevitation(false);
            LevitationEffect levitation = new LevitationEffect(plugin, vehicle, player, plugin.getConfig().getLong("items.levitationDuration"));
            playerData.levitationEffect = levitation;
            levitation.runTaskTimer(plugin, 0L, 0L);

        } else if (item == Material.ENDER_PEARL) {
            if (vehicle.isOnGround()) {
                event.setCancelled(true);
                return;
            }
            plugin.temporaryAllowDismount = true;

            PlayerData playerData = plugin.playerDatas.get(player.getUniqueId());
            Location location = playerData.lastSafeLocation;

            teleportInBoat((Boat)vehicle, player, location);
            teleportEffect(location, player);
            plugin.temporaryAllowDismount = false;

        } else if (item == Material.INK_SAC) {
            BlindnessEffect blindness =  new BlindnessEffect(plugin, player, plugin.getConfig().getLong("items.blindnessLingerDuration"), plugin.getConfig().getInt("items.blindnessEffectDuration"));
            blindness.runTaskTimer(plugin, 15L, 0L);
        }

        int amount = event.getItemDrop().getItemStack().getAmount();
        if (amount > 1) player.getInventory().addItem(new ItemStack(item, amount-1));

        event.getItemDrop().remove();
    }

    public void teleportInBoat(Boat old, Player player, Location location) {
        //spawning a new boat is probably not ideal, but i couldnt figure out how to not
        Boat newVehicle = (Boat)(player.getWorld().spawnEntity(location, EntityType.BOAT));
        newVehicle.setBoatType(old.getBoatType());
        old.remove();
        player.teleport(location);
        newVehicle.addPassenger(player);
    }

    public void teleportEffect(Location location, Player player) {
        Location up = location.clone().add(0,0.5,0);
        plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location, true, 0.85f, 1.15f);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, up, 50);
    }
}
