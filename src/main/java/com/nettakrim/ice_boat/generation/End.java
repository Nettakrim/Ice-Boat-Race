package com.nettakrim.ice_boat.generation;

import org.joml.Vector2f;

public class End {
    public End(Vector2f point, Vector2f angle) {
        this.point = point;
        angle.normalize();
        this.angle = angle;
    }

    public final Vector2f point;
    public final Vector2f angle;
}
