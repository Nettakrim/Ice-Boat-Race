package com.nettakrim.ice_boat.listeners;

import com.nettakrim.ice_boat.items.SnowballTrail;
import com.nettakrim.ice_boat.items.TrackMelter;
import com.nettakrim.ice_boat.paths.Path;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.PlayerData;
import com.nettakrim.ice_boat.IceBoat.GameState;
import com.nettakrim.ice_boat.items.BlindnessEffect;
import com.nettakrim.ice_boat.items.LevitationEffect;

import java.util.List;

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
            teleportBoatWithPassengers((Boat)vehicle, event.getTo());
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

        switch (item) {
            case FEATHER -> levitationItemDrop(player);
            case ENDER_PEARL -> teleporterItemDrop(event, player, vehicle);
            case INK_SAC -> blindnessItemDrop(player);
            case BLAZE_POWDER -> metlerItemDrop(event, player);
            case SNOWBALL -> snowItemDrop(player, false);
            //with the range of items available, some new items can be crafted!
            //while crafting could just be disabled, and easily so, this is more fun
            case ENDER_EYE -> {
                metlerItemDrop(event, player);
                plugin.killPlayer(player, true, Component.text(player.getName()).append(Component.text(" Tried to use an Unstable Teleporter!")));
            }
            case SNOW_BLOCK -> snowItemDrop(player, true);
            case BLACK_DYE -> {
                blindnessItemDrop(player);
                turnIntoSquid(player, vehicle);
            }
        }

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

    private void levitationItemDrop(Player player) {
        PlayerData playerData = plugin.playerDatas.get(player.getUniqueId());
        playerData.cancelLevitation(true, null);
        LevitationEffect levitation = new LevitationEffect(plugin, player, plugin.getConfig().getLong("items.levitationDuration"));
        playerData.levitationEffect = levitation;
        levitation.runTaskTimer(plugin, 0L, 0L);
    }

    private void teleporterItemDrop(PlayerDropItemEvent event, Player player, Entity vehicle) {
        if (vehicle.isOnGround()) {
            event.setCancelled(true);
            return;
        }
        plugin.temporaryAllowDismount = true;

        PlayerData playerData = plugin.playerDatas.get(player.getUniqueId());
        Location location = playerData.lastSafeLocation;

        teleportBoatWithPassengers((Boat)vehicle, location);
        plugin.teleportEffect(location, player);
        plugin.temporaryAllowDismount = false;
    }

    private void blindnessItemDrop(Player player) {
        BlindnessEffect blindness = new BlindnessEffect(plugin, player, plugin.getConfig().getLong("items.blindnessLingerDuration"), plugin.getConfig().getInt("items.blindnessEffectDuration"));
        blindness.runTaskTimer(plugin, 15L, 0L);
        plugin.resetClearRunnables.add(blindness);
    }

    private void metlerItemDrop(PlayerDropItemEvent event, Player player) {
        Path path = plugin.getPath(player.getLocation().getBlockY());
        if (path == null) {
            event.setCancelled(true);
            return;
        }
        TrackMelter trackMelter = new TrackMelter(plugin, player.getLocation(), path, (int)((path.radius+10)/3), plugin.getConfig().getLong("items.melterDuration"));
        trackMelter.runTaskTimer(plugin, 0L, 0L);
        plugin.resetClearRunnables.add(trackMelter);
    }

    private void snowItemDrop(Player player, boolean spawnGolem) {
        Location location = player.getLocation();
        location.subtract(location.getDirection().setY(0).normalize().multiply(plugin.getConfig().getDouble("items.snowDropBackwards")));
        int ox = location.getBlock().getX();
        int oy = location.getBlock().getY();
        int oz = location.getBlock().getZ();
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <=0; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) + Math.abs(z) >= 5) continue;
                    Block block = player.getWorld().getBlockAt(x+ox, y+oy, z+oz);
                    if (block.isSolid() && block.getType() != Material.LIME_WOOL) {
                        block.setType(Material.SNOW_BLOCK);
                    }
                }
            }
        }

        location.add(0, 1, 0);
        BlockData blockData = Material.SNOW_BLOCK.createBlockData();
        location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 50, 2.5, 1, 2.5, blockData);
        plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_SNOW_GOLEM_DEATH, location, true, 0.85f, 1.15f);

        if (spawnGolem) {
            Snowman snowman = (Snowman)location.getWorld().spawnEntity(location, EntityType.SNOWMAN);
            plugin.resetClearEntities.add(snowman);
            plugin.playSoundGloballyToPlayer(player, Sound.ENTITY_IRON_GOLEM_HURT, location, true, 0.85f, 1.15f);
        }
    }

    private void turnIntoSquid(Player player, Entity vehicle) {
        plugin.temporaryAllowDismount = true;
        Entity squid = player.getWorld().spawnEntity(player.getLocation(), EntityType.SQUID);
        squid.setInvulnerable(true);
        plugin.resetClearEntities.add(squid);
        plugin.resetClearEntities.add(vehicle);
        vehicle.removePassenger(player);
        vehicle.addPassenger(squid);
        plugin.killPlayer(player, false, Component.text(player.getName()).append(Component.text(" Turned into a Squid!")));
        plugin.temporaryAllowDismount = false;
    }

    public void teleportBoatWithPassengers(Boat boat, Location location) {
        //spawning a new boat is probably not ideal, but i couldnt figure out how to not
        Boat newVehicle = (Boat)(boat.getWorld().spawnEntity(location, EntityType.BOAT));
        newVehicle.setBoatType(boat.getBoatType());
        List<Entity> oldPassengers = boat.getPassengers();
        newVehicle.setGravity(boat.hasGravity());
        boat.remove();
        for (Entity entity : oldPassengers) {
            entity.teleport(location);
            newVehicle.addPassenger(entity);
        }
    }
}
