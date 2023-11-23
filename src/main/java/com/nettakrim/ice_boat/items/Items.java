package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.joml.Vector2f;

import java.util.ArrayList;

public class Items {
    private final IceBoat plugin;

    private ArrayList<Material> itemPool;
    private int timeSinceBoxSpawn;

    public Items(IceBoat plugin) {
        this.plugin = plugin;
    }

    public void createItemPool() {
        itemPool = new ArrayList<>();
        addItemsToPool(Material.FEATHER, "levitation");
        addItemsToPool(Material.ENDER_PEARL, "teleporter");
        addItemsToPool(Material.INK_SAC, "blindness");
        addItemsToPool(Material.BLAZE_POWDER, "melter");
        addItemsToPool(Material.SNOWBALL, "snow");
    }

    private void addItemsToPool(Material material, String key) {
        int amount = plugin.getConfig().getInt("items."+key+"BoxWeight");
        if (amount == 0) return;
        for (int x = 0; x < amount; x++) {
            itemPool.add(material);
        }
    }

    public void giveStartingItems(Player player, Entity vehicle) {
        PlayerInventory inventory = player.getInventory();
        FileConfiguration config = plugin.getConfig();
        inventory.clear();
        if (vehicle.getPassengers().size() == 0) {
            inventory.addItem(new ItemStack(Material.FEATHER, config.getInt("items.levitationItems")));
            inventory.addItem(new ItemStack(Material.ENDER_PEARL, config.getInt("items.teleporterItems")));
            inventory.addItem(new ItemStack(Material.INK_SAC, config.getInt("items.blindnessItems")));
            inventory.addItem(new ItemStack(Material.BLAZE_POWDER, config.getInt("items.melterItems")));
            inventory.addItem(new ItemStack(Material.SNOWBALL, config.getInt("items.snowItems")));
        }
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
}
