package com.nettakrim.ice_boat.paths;

import org.joml.Vector2f;

public class End {
    public End(Vector2f point, Vector2f angle) {
        this.point = point;
        angle.normalize();
        this.angle = angle;
    }

    public Vector2f point;
    public Vector2f angle;
}
