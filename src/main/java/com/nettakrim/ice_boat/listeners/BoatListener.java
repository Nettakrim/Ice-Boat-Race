package com.nettakrim.ice_boat.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.IceBoat.GameState;

public class BoatListener implements Listener {

    private final IceBoat plugin;

    private final int startHeight;

    public BoatListener(IceBoat plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startHeight = plugin.getConfig().getInt("game.startHeight")+5;
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        Entity entity = event.getMount();
        if (entity.getWorld() != plugin.world) return;
        if (plugin.gameState != GameState.WAITING) return;

        if (!(entity instanceof Player player)) return;
        plugin.waitingPlayerJoin(player, event.getEntity());
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;

        if (!(entity instanceof Player player)) return;

        if (plugin.gameState == GameState.WAITING) {
            plugin.waitingPlayerLeave(player);
        }

        if (!plugin.temporaryAllowDismount) {
            if (plugin.gameState == GameState.ENDING) {
                event.setCancelled(true);
            }

            if (plugin.gameState != GameState.PLAYING) return;

            event.setCancelled(true);
            plugin.playerDatas.get(player.getUniqueId()).cancelLevitation(true, event.getDismounted());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld() != plugin.world) return;

        Location location = player.getLocation();
        location.subtract(0, 1, 0);
        Block block = location.getBlock();
        Material material = location.getBlock().getType();

        if (material == Material.SEA_LANTERN || material == Material.WARPED_PLANKS) {
            if (plugin.gameState == GameState.LOBBY || plugin.gameState == GameState.WAITING) {
                plugin.teleportIntoGame(player);
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(new Location(player.getWorld(), 0, startHeight, 0));
            }
        }

        if (plugin.gameState != GameState.PLAYING) return;

        if (player.isInsideVehicle()) {
            plugin.updatePlayerPosition(player, block, material, location);
        } else if (!plugin.temporaryAllowDismount && player.getLocation().getY() < startHeight) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;

        if (plugin.gameState != GameState.PLAYING) return;

        if (event.getCause().equals(DamageCause.SUFFOCATION)) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityHit(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity.getWorld() != plugin.world) return;
        event.setCancelled(true);

        if (entity instanceof Snowman) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
            EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;

            if (byEntityEvent.getDamager() instanceof Player player) {
                player.getInventory().addItem(plugin.items.getItem("snow", 1));

                BlockData blockData = Material.SNOW_BLOCK.createBlockData();
                Location location = entity.getLocation();
                location.add(0,1,0);
                location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 50, 0.5, 1, 0.5, blockData);
                plugin.playSoundLocallyToAll(Sound.ENTITY_SNOW_GOLEM_DEATH, location, 0.85f, 1.15f);

                entity.remove();
            }
        }
    }

    @EventHandler
    public void onVehicleHit(VehicleDamageEvent event) {
        if (event.getVehicle().getWorld() != plugin.world) return;
        event.setCancelled(true);
    }
}