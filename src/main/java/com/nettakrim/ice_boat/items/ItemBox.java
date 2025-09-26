package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ItemBox extends BukkitRunnable {

    private final IceBoat plugin;

    private final String itemKey;
    private final Location location;

    private ItemDisplay display;
    private Location displayLocation;

    private int delay;
    private int activeTicks;
    private float rotation;

    public ItemBox(IceBoat plugin, String itemKey, Location location, int delay, double displayHeight) {
        this.plugin = plugin;
        this.itemKey = itemKey;
        this.location = location;
        start(delay, displayHeight, plugin.players.size());
    }

    private void start(int delay, double displayHeight, int players) {
        displayLocation = location.clone();
        displayLocation.add(0, displayHeight, 0);
        display = (ItemDisplay) location.getWorld().spawnEntity(displayLocation, EntityType.ITEM_DISPLAY);
        display.setViewRange(1000);
        Quaternionf identity = new Quaternionf(0,0,0,1);
        Transformation transformation = new Transformation(new Vector3f(0,0,0), identity, new Vector3f(0.6f, 0.6f, 0.6f), identity);
        display.setTransformation(transformation);
        display.setItemStack(new ItemStack(Material.OBSIDIAN));
        display.addScoreboardTag("ItemBox");
        this.delay = delay;
        if (players == 2) {
            activeTicks = -80;
        } else if (players < 2) {
            unlock();
        }
    }

    @Override
    public void run() {
        if (rotation%20 == 0) {
            Transformation transformation = display.getTransformation();
            display.setInterpolationDuration(21);
            display.setInterpolationDelay(-1);
            transformation.getRightRotation().set(new Quaternionf().rotationY((rotation/30f*(float)Math.PI)));
            transformation.getTranslation().set(0, Math.sin(rotation/30)/15, 0);
            display.setTransformation(transformation);
        }
        rotation+=1;

        if (!location.getBlock().isSolid()) {
            breakBox();
            cancel();
        }

        if (activeTicks <= 0) {
            location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, displayLocation, 1, 0.5, 0.4, 0.5, 0, null, true);
            location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, displayLocation, 1, 0.5, 0.4, 0.5, 0, null, false);
            if (plugin.generation.getCurrentHeight() < location.getY()-3) {
                unlock();
            } else if (activeTicks < 0) {
                activeTicks++;
                if (activeTicks == 0) {
                    unlock();
                }
            }
            return;
        }

        if (activeTicks == delay) {
            display.setItemStack(new ItemStack(Material.GOLD_BLOCK));
            plugin.playSoundLocallyToAll(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, displayLocation, 0.8f, 1.2f);
        }

        if (activeTicks <= delay) {
            activeTicks++;
            return;
        }

        for (Player player : plugin.players) {
            Location playerPos = player.getLocation();
            //location is 0.5 blocks below playerPos
            if (playerPos.getY() > location.getY() && playerPos.getY() < location.getY()+1 && location.distanceSquared(playerPos) < 8) {
                player.getInventory().addItem(plugin.items.getItem(itemKey, 1));
                breakBox();
                cancel();
                return;
            }
        }
    }

    private void unlock() {
        display.setItemStack(new ItemStack(Material.CRYING_OBSIDIAN));
        for (double r = 0; r < 360; r++) {
            double rad = Math.toRadians(r);
            double d = IceBoat.random.nextDouble(0.75, 1.5);
            double x = Math.sin(rad) * d;
            double z = Math.cos(rad) * d;
            double t = IceBoat.random.nextDouble(0.8, 1.2);
            location.getWorld().spawnParticle(Particle.PORTAL, displayLocation, 0, x, -0.5, z, t, null, r%2 == 0);
        }
        activeTicks = 1;
    }

    private void breakBox() {
        plugin.playSoundLocallyToAll(Sound.BLOCK_AMETHYST_BLOCK_BREAK, displayLocation, 0.8f, 1.2f);
        BlockData blockData = display.getItemStack().getType().createBlockData();
        location.getWorld().spawnParticle(Particle.BLOCK, displayLocation, 50, 0.35, 0.35, 0.35, 1, blockData, true);
    }

    @Override
    public void cancel() {
        super.cancel();
        display.remove();
    }
}
