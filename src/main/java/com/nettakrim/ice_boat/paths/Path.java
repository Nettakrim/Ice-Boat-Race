package com.nettakrim.ice_boat.paths;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.joml.Vector2f;

public abstract class Path {
    public final End entrance;
    public final End exit;

    protected Path(End entrance, End exit) {
        this.entrance = entrance;
        this.exit = exit;
    }

    public abstract float getDistanceField(Vector2f pos);

    public abstract Approximation getApproximation();

    public void generate(World world, float radius, int height) {
        Approximation approximation = getApproximation();
        Bukkit.getLogger().info(approximation.minX+" "+approximation.maxX+" / "+approximation.minY+" "+approximation.maxY);
        int expand = ((int)radius)+1;
        for (int x = approximation.minX-expand; x < approximation.maxX+expand; x++) {
            for (int y = approximation.minY-expand; y < approximation.maxY+expand; y++) {
                float distance = getDistanceField(new Vector2f(x, y));
                if (distance < radius) {
                    world.getBlockAt(x, height, y).setType(Material.PACKED_ICE);
                    world.getBlockAt(x, height+1, y).setType(Material.AIR);
                }
            }
        }
    }

    public class Approximation {
        public int minX;
        public int minY;
        public int maxX;
        public int maxY;
        public final Vector2f[] points;

        public Approximation(int minX, int minY, int maxX, int maxY, Vector2f[] points) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.points = points;
        }
    }
}
