package com.nettakrim.ice_boat.paths;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.FloatMath;

public abstract class Path {
    @FunctionalInterface
    public interface RandomPathBuilder {
        Path buildRandom(End lastEnd, float lengthScale);
    }

    public final End entrance;
    public final End exit;
    public Approximation approximation;

    private int expand;
    public int height;

    private int blocks;

    protected Path(End entrance, End exit) {
        this.entrance = entrance;
        this.exit = exit;
    }

    public abstract float getDistanceField(Vector2f pos);

    protected abstract Approximation createApproximation();

    public abstract Vector2f getPosition(float percentage);

    public void generate(World world, float radius, int height, float blueIceSize, boolean isFinishLine) {
        this.expand = ((int)radius)+1;
        this.height = height;
        this.blocks = 0;

        int exitX = (int)(exit.point.x);
        int exitY = (int)(exit.point.y);
        float angleX = exit.angle.x;
        float angleY = exit.angle.y;

        for (int x = approximation.minX-expand; x < approximation.maxX+expand; x++) {
            for (int y = approximation.minY-expand; y < approximation.maxY+expand; y++) {
                boolean cutoff = ((exitX-x)*angleX)+((exitY-y)*angleY) < 0 && Math.max(Math.abs(exitX-x), Math.abs(exitY-y)) < expand+1;
                if (cutoff) continue;
                float distance = getDistanceField(new Vector2f(x, y));
                if (distance < radius) {
                    Material material = Material.PACKED_ICE;
                    if (distance < radius*blueIceSize) {
                        material = Material.BLUE_ICE;
                    }

                    world.getBlockAt(x, height, y).setType(material);
                    blocks++;
                }
            }
        }

        if (isFinishLine) {
            expand++;
            int winCircleBounds = ((int)(radius+0.5f))+1;
            float winCircle = (radius+0.5f)*(radius+0.5f);
            int offsetX = Math.round(exit.point.x + exit.angle.x);
            int offsetY = Math.round(exit.point.y + exit.angle.y);
            for (int x = -winCircleBounds; x < winCircleBounds; x++) {
                for (int y = -winCircleBounds; y < winCircleBounds; y++) {
                    int dist = x*x + y*y;
                    if (dist < winCircle) {
                        Block block = world.getBlockAt(x+offsetX, height, y+offsetY);
                        if (block.getType() == Material.AIR) {
                            block.setType(Material.LIME_WOOL);
                        }
                    }
                }
            }
        }
    }

    public void clear(World world) {
        if (blocks == 0) return;
        for (int x = approximation.minX-expand; x < approximation.maxX+expand; x++) {
            for (int y = approximation.minY-expand; y < approximation.maxY+expand; y++) {
                world.getBlockAt(x, height, y).setType(Material.AIR);
            }
        }
        blocks = 0;
    }

    public void decay(Random random, World world, float decaySpeed, int decayStage) {
        if (blocks < 10) {
            if (blocks > 0) {
                clear(world);
            }
            return;
        }

        float areaToDecay = (approximation.getArea(expand)/256)*decaySpeed;
        int decayBlocks = (int)areaToDecay;
        if (random.nextFloat() < areaToDecay % 1) decayBlocks++;
        for (int a = 0; a < decayBlocks; a++) {
            int x = random.nextInt(approximation.minX-expand, approximation.maxX+expand);
            int y = random.nextInt(approximation.minY-expand, approximation.maxY+expand);
            Block block = world.getBlockAt(x, height, y);
            if (block.isSolid()) {
                Material material = block.getType();
                if (melt(block, material, decayStage)) {
                    meltAdjacent(world, material, x+1, y, height);
                    meltAdjacent(world, material, x-1, y, height);
                    meltAdjacent(world, material, x, y+1, height);
                    meltAdjacent(world, material, x, y-1, height);
                }
            } else if (decayStage >= 1) {
                meltAdjacent(world, Material.AIR, x+1, y, height);
                meltAdjacent(world, Material.AIR, x-1, y, height);
                meltAdjacent(world, Material.AIR, x, y+1, height);
                meltAdjacent(world, Material.AIR, x, y-1, height);
            }
        }
    }

    private boolean melt(Block block, Material material, int decayStage) {
        if (material == Material.BLUE_ICE) {
            block.setType(Material.PACKED_ICE);
            return true;
        } else if (material == Material.PACKED_ICE && decayStage >= 1) {
            block.setType(Material.ICE);
            return true;
        } else if (material == Material.ICE && decayStage >= 2) {
            block.setType(Material.AIR);
            blocks--;
            return true;
        }
        return false;
    }

    private void meltAdjacent(World world, Material material, int x, int y, int height) {
        Block block = world.getBlockAt(x, height, y);
        if (!block.isSolid()) return;
        //melts adjacent if it is a higher tier ice than the one that just melted
        Material material2 = block.getType();
        if (material2 == Material.BLUE_ICE) {
            melt(block, material2, 2);
            return;
        }
        if (material == Material.BLUE_ICE) return;

        if (material2 == Material.PACKED_ICE) {
            melt(block, material2, 2);
        } else if (material2 == Material.ICE && material != Material.PACKED_ICE) {
            melt(block, material2, 2);
        }
    }

    public boolean passGenerationChecks(ArrayList<Path> paths, float turnZoneStart, float turnZoneSize, float distanceThreshold) {
        return passTurnCheck(turnZoneStart, turnZoneSize) && passDistanceCheck(paths, distanceThreshold);
    }

    private boolean passTurnCheck(float turnZoneStart, float turnZoneSize) {
        Vector2f v = new Vector2f(entrance.point).normalize();
        float angle = v.dot(new Vector2f(new Vector2f(exit.point).sub(new Vector2f(entrance.point))).normalize());
        float length = exit.point.length();

        return angle < FloatMath.clamp(1-(length-turnZoneStart)/turnZoneSize, -0.5f, 1);
    }

    private boolean passDistanceCheck(ArrayList<Path> paths, float distanceThreshold) {
        int size = paths.size();
        if (size >= 1) {
            float minimumDistance = approximation.minimumDistance(paths.get(size-1).approximation);
            if (minimumDistance < distanceThreshold) return false;
            if (size >= 2) {
                minimumDistance = approximation.minimumDistance(paths.get(size-2).approximation);
                return minimumDistance >= distanceThreshold;
            }
        }
        return true;
    }

    public static class Approximation {
        public final int minX;
        public final int minY;
        public final int maxX;
        public final int maxY;
        public final Vector2f[] points;

        public Approximation(int minX, int minY, int maxX, int maxY, Vector2f[] points) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.points = points;
        }

        public int getArea(int expand) {
            return ((maxX-minX)+(expand*2))*((maxY-minY)+(expand*2));
        }

        //https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
        private float pointToLineDistanceSquared(Vector2f p, Vector2f a, Vector2f b) {
            float A = p.x - a.x;
            float B = p.y - a.y;
            float C = b.x - a.x;
            float D = b.y - a.y;
          
            float dot = A * C + B * D;
            float len_sq = C * C + D * D;
            float param;
            param = FloatMath.clamp(dot / len_sq, 0f, 1f);
          
            float xx = FloatMath.lerp(a.x, b.x, param);
            float yy = FloatMath.lerp(a.y, b.y, param);
        
            float dx = p.x - xx;
            float dy = p.y - yy;
            return dx * dx + dy * dy;
        }

        private float pointsToLinesDistanceSquared(Vector2f[] points, Vector2f[] lines, int o) {
            float distanceSquared = -1;
            for (int i = o; i < points.length; i++) {
                for (int j = 1-o; j < lines.length-1; j++) {
                    float distance = pointToLineDistanceSquared(points[i], lines[j], lines[j+1]);
                    if (distanceSquared == -1 || distance < distanceSquared) {
                        distanceSquared = distance;
                    }
                }
            }
            return distanceSquared;
        }

        //https://stackoverflow.com/questions/3838329/how-can-i-check-if-two-segments-intersect
        // doesnt work with coliniar lines, but that *shouldnt* really ever happen here
        private boolean ccw(Vector2f a, Vector2f b, Vector2f c) {
            return (c.y-a.y) * (b.x-a.x) > (b.y-a.y) * (c.x-a.x);
        }

        private boolean linesIntersect(Vector2f a1, Vector2f a2, Vector2f b1, Vector2f b2) {
            return ccw(a1,b1,b2) != ccw(a2,b1,b2) && ccw(a1,a2,b1) != ccw(a1,a2,b2);
        }

        private boolean hasIntersection(Vector2f[] linesA, Vector2f[] linesB) {
            for (int i = 0; i < linesA.length-1; i++) {
                for (int j = 0; j < linesB.length-1; j++) {
                    if(linesIntersect(linesA[j], linesA[j+1], linesB[j], linesB[j+1])) return true;
                }
            }
            return false;
        }

        public float minimumDistance(Approximation other) {
            if (hasIntersection(points, other.points)) return 0;
            float distanceSquared = Math.min(
                pointsToLinesDistanceSquared(points, other.points, 1),
                pointsToLinesDistanceSquared(other.points, points, 0)
            );
            return FloatMath.sqrt(distanceSquared);
        }
    }
}
