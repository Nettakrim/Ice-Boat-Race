package com.nettakrim.ice_boat;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class FloatMath {
    public static float sign(float x) {
        if (x < 0) return -1;
        if (x > 0) return 1;
        return 0;
    }

    public static Vector3f clamp(Vector3f v, float min, float max) {
        return new Vector3f(clamp(v.x, min, max), clamp(v.y, min, max), clamp(v.z, min, max));
    }

    public static float clamp(float x, float min, float max) {
        if (x <= min) return min;
        if (x >= max) return max;
        return x;
    }

    public static float sqrt(float x) {
        return (float)Math.sqrt((double)x);
    }

    public static float dot(Vector2f a, Vector2f b) {
        return new Vector2f(a).dot(b);
    }

    public static float lerp(float a, float b, float t) {
        return (1-t)*a + t*b;
    }  

    public static Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(a).mul(1-t).add(new Vector2f(b).mul(t));
    }  
}
