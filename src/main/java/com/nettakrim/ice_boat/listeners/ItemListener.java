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
import com.nettakrim.ice_boat.IceBoat.GameState;
import com.nettakrim.ice_boat.items.BlindnessEffect;
import com.nettakrim.ice_boat.items.LevitationEffect;

public class ItemListener implements Listener {
    
    @EventHandler
    public void onEntityTeleport(PlayerTeleportEvent event) {
        if (IceBoat.gameState != GameState.PLAYING) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        BoatListener.temporaryAllowDismount = true;
        if (vehicle != null) {
            teleportInBoat((Boat)vehicle, player, event.getTo());
            teleportEffect(event.getTo(), player);
        }
        BoatListener.temporaryAllowDismount = false;
    }

    @EventHandler
    public void useItem(PlayerDropItemEvent event) {
        if (IceBoat.gameState == GameState.WAITING) {
            event.setCancelled(true);
            return;
        }
        if (IceBoat.gameState != GameState.PLAYING) return;
        Player player = event.getPlayer();
        if (!player.isInsideVehicle()) return;

        Material item = event.getItemDrop().getItemStack().getType();

        Entity vehicle = player.getVehicle();

        if(item == Material.FEATHER){
            int index = IceBoat.getPlayerIndex(player);
            LevitationEffect previous = IceBoat.instance.levitationTimers[index];
            if (!LevitationEffect.isFinished(previous)) {
                previous.cancel(true);
            }
            IceBoat.instance.levitationTimers[index] = new LevitationEffect(vehicle, player, IceBoat.config.getLong("levitationDuration"));

        } else if (item == Material.ENDER_PEARL) {
            if (vehicle.isOnGround()) {
                event.setCancelled(true);
                return;
            }
            BoatListener.temporaryAllowDismount = true;

            int index = IceBoat.getPlayerIndex(player);
            Location location = IceBoat.instance.lastSafeLocation[index];

            teleportInBoat((Boat)vehicle, player, location);
            teleportEffect(location, player);

        } else if (item == Material.INK_SAC) {
            new BlindnessEffect(player, 15L, IceBoat.config.getLong("blindnessLingerDuration"), IceBoat.config.getInt("blindnessEffectDuration"));

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
        IceBoat.playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location, true);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, up, 50);
    }
}
