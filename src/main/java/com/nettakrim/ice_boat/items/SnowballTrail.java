package com.nettakrim.ice_boat.items;

import com.nettakrim.ice_boat.IceBoat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class SnowballTrail extends BukkitRunnable {

    private final IceBoat plugin;

    private final Entity snowball;
    private final World world;

    private final int startHeight;

    public SnowballTrail(IceBoat plugin, Entity snowball) {
        this.plugin = plugin;
        this.snowball = snowball;
        this.world = snowball.getWorld();
        this.startHeight = plugin.getConfig().getInt("game.startHeight")+5;
    }

    @Override
    public void run() {
        if (snowball.isDead()) {
            cancel();
            return;
        }

        boolean big = false;
        boolean small = false;

        Location location = snowball.getLocation();
        Block blockPos = location.getBlock();
        int ox = blockPos.getX();
        int oy = blockPos.getY();
        int oz = blockPos.getZ();
        for (int x=-1; x<=1; x++) {
            for (int y=-6; y<=0; y++) {
                for (int z=-1; z<=1; z++) {
                    if (Math.abs(x*z) == 0) {
                        big = setBlock(x+ox, y+oy, z+oz) || big;
                    }
                }
            }
        }
        if (!big) {
            for (int y = -12; y < -6; y++) {
                small = setBlock(ox, y + oy, oz) || small;
            }
        }

        BlockData blockData = Material.SNOW_BLOCK.createBlockData();
        location.getWorld().spawnParticle(Particle.BLOCK, location, 1, 0.1, 0.1, 0.1, blockData);

        if ((big || small) && IceBoat.random.nextFloat() < (big ? 0.75 : 0.333)) {
            plugin.playSoundLocallyToAll(Sound.BLOCK_SNOW_BREAK, location, 0.8f, 1.2f);
        }
    }

    private boolean setBlock(int x, int y, int z) {
        if (y > startHeight) return false;
        Block block = world.getBlockAt(x, y, z);
        if (block.isSolid() && block.getType() != Material.LIME_WOOL) {
            block.setType(Material.SNOW_BLOCK);
            return true;
        }
        return false;
    }
}
