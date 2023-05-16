package com.nettakrim.ice_boat.listeners;

import com.nettakrim.ice_boat.items.SnowballTrail;
import com.nettakrim.ice_boat.items.TrackMelter;
import com.nettakrim.ice_boat.paths.Path;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
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
            BlindnessEffect blindness = new BlindnessEffect(plugin, player, plugin.getConfig().getLong("items.blindnessLingerDuration"), plugin.getConfig().getInt("items.blindnessEffectDuration"));
            blindness.runTaskTimer(plugin, 15L, 0L);

        } else if (item == Material.BLAZE_POWDER) {
            Path path = plugin.getPath(player.getLocation().getBlockY());
            if (path == null) {
                event.setCancelled(true);
                return;
            }
            TrackMelter trackMelter = new TrackMelter(plugin, player.getLocation(), path, (int)((path.radius+10)/3), plugin.getConfig().getLong("items.melterDuration"));
            trackMelter.runTaskTimer(plugin, 0L, 0L);

        } else if (item == Material.SNOWBALL) {
            Location location = player.getLocation();
            location.subtract(location.getDirection().setY(0).multiply(plugin.getConfig().getDouble("items.snowDropBackwards")));
            int ox = location.getBlock().getX();
            int y = location.getBlock().getY();
            int oz = location.getBlock().getZ();
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x)+Math.abs(z) >= 5) continue;
                    Block block = player.getWorld().getBlockAt(x+ox, y, z+oz);
                    if (block.isSolid()) {
                        block.setType(Material.SNOW_BLOCK);
                    }
                }
            }

            location.add(0, 1, 0);
            BlockData blockData = Material.SNOW_BLOCK.createBlockData();
            location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 50, 2.5, 1, 2.5, blockData);
            plugin.playSoundGloballyToPlayer(player, Sound.BLOCK_SNOW_BREAK, location, true, 0.85f, 1.15f);
        }

        int amount = event.getItemDrop().getItemStack().getAmount();
        if (amount > 1) player.getInventory().addItem(new ItemStack(item, amount-1));

        event.getItemDrop().remove();
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

    public void teleportEffect(Location location, Player player) {
        Location up = location.clone().add(0,0.5,0);
        plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_ENDERMAN_TELEPORT, location, true, 0.85f, 1.15f);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, up, 50);
    }
}
