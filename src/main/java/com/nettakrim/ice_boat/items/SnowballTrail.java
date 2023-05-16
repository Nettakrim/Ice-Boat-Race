package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class SnowballTrail extends BukkitRunnable {

    private final IceBoat plugin;

    private final Entity snowball;
    private final World world;

    public SnowballTrail(IceBoat plugin, Entity snowball) {
        this.plugin = plugin;
        this.snowball = snowball;
        this.world = snowball.getWorld();
    }

    @Override
    public void run() {
        if (snowball.isDead()) {
            cancel();
            return;
        }

        Location location = snowball.getLocation();
        Block blockPos = location.getBlock();
        int ox = blockPos.getX();
        int oy = blockPos.getY();
        int oz = blockPos.getZ();
        for (int x=-1; x<=1; x++) {
            for (int y=-6; y<=0; y++) {
                for (int z=-1; z<=1; z++) {
                    if (Math.abs(x*z) != 0) continue;
                    Block block = world.getBlockAt(x+ox, y+oy, z+oz);
                    if (block.isSolid()) {
                        block.setType(Material.SNOW_BLOCK);
                    }
                }
            }
        }

        BlockData blockData = Material.SNOW_BLOCK.createBlockData();
        location.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 1, 0.1, 0.1, 0.1, blockData);
    }
}
