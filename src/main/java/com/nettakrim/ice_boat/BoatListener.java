package com.nettakrim.ice_boat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;

import com.nettakrim.ice_boat.items.BlindnessEffect;
import com.nettakrim.ice_boat.items.LevitationEffect;

public class BoatListener implements Listener {
    private boolean temporaryAllowDismount = false;

    public static boolean allowDismount = false;

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        Player player = (Player)entity;
        if (!allowDismount && !temporaryAllowDismount) {
            event.setCancelled(true);
            int index = IceBoat.getPlayerIndex(player);
            LevitationEffect levitation = IceBoat.instance.levitationTimers[index];
            if (!LevitationEffect.isFinished(levitation)) {
                levitation.cancel();
            }
        }
    }

    @EventHandler
    public void onEntityTeleport(PlayerTeleportEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        temporaryAllowDismount = true;
        if (vehicle != null) {
            teleportInBoat((Boat)vehicle, player, event.getTo());
            teleportEffect(event.getTo(), player);
        }
        temporaryAllowDismount = false;
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
                if (block.getType() == Material.BLUE_ICE) {
                    IceBoat.instance.lastSafeLocation[IceBoat.getPlayerIndex(player)] = player.getVehicle().getLocation();
                }
            }
        }
    }

    @EventHandler
    public void useItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!player.isInsideVehicle()) return;

        Material item = event.getItemDrop().getItemStack().getType();

        Entity vehicle = player.getVehicle();

        if(item == Material.FEATHER){
            int index = IceBoat.getPlayerIndex(player);
            LevitationEffect previous = IceBoat.instance.levitationTimers[index];
            if (!LevitationEffect.isFinished(previous)) {
                previous.cancel();
            }
            IceBoat.instance.levitationTimers[index] = new LevitationEffect(vehicle, player, 100L);

        } else if (item == Material.ENDER_PEARL) {
            if (vehicle.isOnGround()) {
                event.setCancelled(true);
                return;
            }
            temporaryAllowDismount = true;

            int index = IceBoat.getPlayerIndex(player);
            Location location = IceBoat.instance.lastSafeLocation[index];

            teleportInBoat((Boat)vehicle, player, location);
            teleportEffect(location, player);

        } else if (item == Material.INK_SAC) {
            new BlindnessEffect(player, 15L, 300L, 40);

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
        IceBoat.playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, up, 50);
    }
}