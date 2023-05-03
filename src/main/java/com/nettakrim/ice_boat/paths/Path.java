package com.nettakrim.ice_boat.paths;

import org.bukkit.Material;
import org.bukkit.World;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.FloatMath;

public abstract class Path {
    @FunctionalInterface
    public interface RandomPathBuilder {
        Path buildRandom(End lastEnd, float lengthScale);
    }

    public final End entrance;
    public final End exit;

    private int expand;
    private int height;

    protected Path(End entrance, End exit) {
        this.entrance = entrance;
        this.exit = exit;
    }

    public abstract float getDistanceField(Vector2f pos);

    public abstract Approximation getApproximation();

    public void generate(World world, float radius, int height, float blueIceSize) {
        Approximation approximation = getApproximation();
        this.expand = ((int)radius)+1;
        this.height = height;
        for (int x = approximation.minX-expand; x < approximation.maxX+expand; x++) {
            for (int y = approximation.minY-expand; y < approximation.maxY+expand; y++) {
                float distance = getDistanceField(new Vector2f(x, y));
                if (distance < radius) {
                    world.getBlockAt(x, height, y).setType(distance < radius*blueIceSize ? Material.BLUE_ICE : Material.PACKED_ICE);
                    if (distance < radius-1.5f) {
                        world.getBlockAt(x, height+1, y).setType(Material.AIR);
                    }
                }
            }
        }
    }

    public void clear(World world) {
        Approximation approximation = getApproximation();
        for (int x = approximation.minX-expand; x < approximation.maxX+expand; x++) {
            for (int y = approximation.minY-expand; y < approximation.maxY+expand; y++) {
                world.getBlockAt(x, height, y).setType(Material.AIR);
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

        //https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
        private float pointToLineDistanceSquared(Vector2f p, Vector2f a, Vector2f b) {
            float A = p.x - a.x;
            float B = p.y - a.y;
            float C = b.x - a.x;
            float D = b.y - a.y;
          
            float dot = A * C + B * D;
            float len_sq = C * C + D * D;
            float param = -1;
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
