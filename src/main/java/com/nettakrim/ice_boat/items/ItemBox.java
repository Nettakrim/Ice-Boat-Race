package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

public class ItemBox extends BukkitRunnable {

    private final IceBoat plugin;

    private final Material item;
    private final Location location;

    private ArmorStand armorStand;
    private Location particleLocation;

    private int delay;
    private int activeTicks;
    private double headRotation;

    public ItemBox(IceBoat plugin, Material item, Location location, int delay) {
        this.plugin = plugin;
        this.item = item;
        this.location = location;
        start(delay);
    }

    private void start(int delay) {
        armorStand = (ArmorStand)location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.OBSIDIAN));
        armorStand.setMarker(true);
        armorStand.setInvisible(true);
        armorStand.addScoreboardTag("ItemBox");
        particleLocation = location.clone();
        particleLocation.add(0, 1.75, 0);
        this.delay = delay;
    }

    @Override
    public void run() {
        armorStand.setHeadPose(new EulerAngle(0, headRotation, 0));
        headRotation = (headRotation+0.1)%(Math.PI*2);

        if (activeTicks == 0) {
            location.getWorld().spawnParticle(Particle.REVERSE_PORTAL, particleLocation, 2, 0.5, 0.4, 0.5, 0);
            if (plugin.getHeight() < location.getY()-3) {
                armorStand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.CRYING_OBSIDIAN));
                for (double r = 0; r < 360; r++) {
                    double rad = Math.toRadians(r);
                    double d = plugin.random.nextDouble(0.75, 1.5);
                    double x = Math.sin(rad) * d;
                    double z = Math.cos(rad) * d;
                    double t = plugin.random.nextDouble(0.8, 1.2);
                    location.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 0, x, -0.5, z, t);
                }
                activeTicks = 1;
            }
            return;
        }

        if (activeTicks == delay) {
            armorStand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.GOLD_BLOCK));
            plugin.playSoundLocallyToAll(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, particleLocation, 0.8f, 1.2f);
        }

        if (activeTicks <= delay) {
            activeTicks++;
            return;
        }

        for (Player player : plugin.players) {
            Location playerPos = player.getLocation();
            //location is 0.5 blocks below playerPos
            if (playerPos.getY() > location.getY() && playerPos.getY() < location.getY()+1 && location.distanceSquared(playerPos) < 8) {
                player.getInventory().addItem(new ItemStack(item));
                plugin.playSoundGloballyToPlayer(player, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, particleLocation, true, 0.8f, 1.2f);
                BlockData blockData = Material.GOLD_BLOCK.createBlockData();
                location.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLocation, 50, 0.35, 0.35, 0.35, blockData);
                cancel();
                return;
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        armorStand.remove();
    }
}
