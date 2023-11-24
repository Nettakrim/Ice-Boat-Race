package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import com.nettakrim.ice_boat.PlayerData;
import com.nettakrim.ice_boat.generation.Path;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.joml.Vector2f;

import java.util.*;

public class Items {
    private final IceBoat plugin;

    public Items(IceBoat plugin) {
        this.plugin = plugin;
    }

    private HashMap<String, ItemStack> items;
    private ArrayList<String> itemPool;
    private int timeSinceBoxSpawn;

    private final Style itemName = Style.style().decoration(TextDecoration.ITALIC, false).color(TextColor.color(255,255,255)).build();
    private final Style itemDescription = Style.style().decoration(TextDecoration.ITALIC, false).color(TextColor.color(192,192,192)).build();

    public void createItemPool() {
        items = new HashMap<>();
        itemPool = new ArrayList<>();

        createItem("levitation", Material.FEATHER);
        createItem("teleporter", Material.ENDER_PEARL);
        createItem("blindness", Material.INK_SAC);
        createItem("melter", Material.BLAZE_POWDER);
        createItem("snow", Material.SNOWBALL);
    }

    private void createItem(String key, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();

        String name = plugin.getConfig().getString("items."+key+"Name");
        if (name != null) {
            itemMeta.displayName(Component.text(name).style(itemName));
        }

        String description = plugin.getConfig().getString("items."+key+"Description");
        if (description != null) {
            ArrayList<Component> list = new ArrayList<>();
            for (String s : description.split("\\n")) {
                list.add(Component.text(s).style(itemDescription));
            }
            itemMeta.lore(list);
        }

        item.setItemMeta(itemMeta);

        items.put(key, item);

        int amount = plugin.getConfig().getInt("items."+key+"BoxWeight");
        if (amount == 0) return;
        for (int x = 0; x < amount; x++) {
            itemPool.add(key);
        }
    }

    public void giveStartingItems(Player player, Entity vehicle) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        if (vehicle.getPassengers().size() == 0) {
            for (Map.Entry<String, ItemStack> entry : items.entrySet()) {
                addStartingItems(inventory, entry.getValue(), entry.getKey());
            }
        }
    }

    private void addStartingItems(PlayerInventory inventory, ItemStack itemStack, String key) {
        int amount = plugin.getConfig().getInt("items."+key+"Items");
        if (amount == 0) {
            return;
        }
        ItemStack newItem = itemStack.clone();
        newItem.setAmount(amount);
        inventory.addItem(newItem);
    }

    public ItemStack getItem(String key, int amount) {
        ItemStack itemStack = items.get(key).clone();
        itemStack.setAmount(amount);
        return itemStack;
    }

    public void tryCreateItemBox(Vector2f pos, float turnZoneEnd, float height) {
        FileConfiguration config = plugin.getConfig();
        if ((1-(IceBoat.random.nextFloat()*IceBoat.random.nextFloat()))/((timeSinceBoxSpawn+1)/2f) < (pos.length()/turnZoneEnd)*config.getDouble("items.boxSpawnRate")) {
            Location location = new Location(plugin.world, pos.x, height + 0.5, pos.y);
            ItemBox itemBox = new ItemBox(plugin, itemPool.get(IceBoat.random.nextInt(itemPool.size())), location, config.getInt("items.boxDelay"), config.getDouble("items.boxHeight"));
            itemBox.runTaskTimer(plugin, 0L, 0L);
            plugin.resetClearRunnables.add(itemBox);
            timeSinceBoxSpawn = 0;
        } else {
            timeSinceBoxSpawn++;
        }
    }

    public void useItem(Material item, Player player, Entity vehicle, Cancellable event) {
        switch (item) {
            case FEATHER -> levitationItemUse(player);
            case ENDER_PEARL -> teleporterItemUse(event, player, vehicle);
            case INK_SAC -> blindnessItemUse(player);
            case BLAZE_POWDER -> metlerItemUse(event, player);
            case SNOWBALL -> snowItemUse(player, false);
            //with the range of items available, some new items can be crafted!
            //while crafting could just be disabled, and easily so, this is more fun
            case ENDER_EYE -> {
                metlerItemUse(event, player);
                plugin.killPlayer(player, true, Component.text(player.getName()).append(Component.text(" Tried to use an Unstable Teleporter!")));
            }
            case SNOW_BLOCK -> snowItemUse(player, true);
            case BLACK_DYE -> {
                blindnessItemUse(player);
                turnIntoSquid(player, vehicle);
            }
        }
    }

    private void levitationItemUse(Player player) {
        PlayerData playerData = plugin.playerDatas.get(player.getUniqueId());
        playerData.cancelLevitation(true, null);
        LevitationEffect levitation = new LevitationEffect(plugin, player, plugin.getConfig().getLong("items.levitationDuration"));
        playerData.levitationEffect = levitation;
        levitation.runTaskTimer(plugin, 0L, 0L);
    }

    private void teleporterItemUse(Cancellable event, Player player, Entity vehicle) {
        if (vehicle.isOnGround()) {
            event.setCancelled(true);
            return;
        }
        plugin.temporaryAllowDismount = true;

        PlayerData playerData = plugin.playerDatas.get(player.getUniqueId());
        Location location = playerData.lastSafeLocation;

        teleportBoatWithPassengers((Boat) vehicle, location);
        plugin.teleportEffect(location, player);
        plugin.temporaryAllowDismount = false;
    }

    private void blindnessItemUse(Player player) {
        BlindnessEffect blindness = new BlindnessEffect(plugin, player, plugin.getConfig().getLong("items.blindnessLingerDuration"), plugin.getConfig().getInt("items.blindnessEffectDuration"));
        blindness.runTaskTimer(plugin, 15L, 0L);
        plugin.resetClearRunnables.add(blindness);
    }

    private void metlerItemUse(Cancellable event, Player player) {
        Path path = plugin.generation.getPath(player.getLocation().getBlockY());
        if (path == null) {
            event.setCancelled(true);
            return;
        }
        TrackMelter trackMelter = new TrackMelter(plugin, player.getLocation(), path, (int)((path.radius+10)/3), plugin.getConfig().getLong("items.melterDuration"));
        trackMelter.runTaskTimer(plugin, 0L, 0L);
        plugin.resetClearRunnables.add(trackMelter);
    }

    private void snowItemUse(Player player, boolean spawnGolem) {
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
        squid.customName(Component.text(player.getName()));
        squid.setCustomNameVisible(true);
        plugin.resetClearEntities.add(squid);
        plugin.resetClearEntities.add(vehicle);
        vehicle.removePassenger(player);
        vehicle.addPassenger(squid);
        plugin.killPlayer(player, false, Component.text(player.getName()).append(Component.text(" Turned into a Squid!")));
        player.setSpectatorTarget(squid);
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
