package com.nettakrim.ice_boat.paths;

import java.util.Random;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class BezierPath extends Path {
    public static BezierPath buildRandom(Random random, End entrance) {
        float entranceLength = (random.nextFloat()+1)*20f;
        Vector2f exit = new Vector2f(1,1);
        while (exit.lengthSquared() > 1 || exit.normalize().dot(entrance.angle) < -0.5f) {
            exit = new Vector2f(random.nextFloat()-0.5f, random.nextFloat()-0.5f);
        }
        //exit should never be too near angle*entranceLength
        exit.normalize();
        exit.mul((random.nextFloat()+1)*20f);
        exit.add(entrance.point);

        return BezierPath.build(entrance, entranceLength, exit);
    }

    public static BezierPath build(End entrance, float entranceLength, Vector2f exitPoint) {
        Vector2f controlPoint = new Vector2f(entrance.angle);
        controlPoint.mul(entranceLength);
        controlPoint.add(entrance.point);
        Vector2f exitAngle = new Vector2f(exitPoint);
        exitAngle.sub(controlPoint);
        End exit = new End(exitPoint, exitAngle);
        return new BezierPath(entrance, controlPoint, exit);
    }

    private BezierPath(End entrance, Vector2f controlPoint, End exit) {
        super(entrance, exit);
        this.controlPoint = controlPoint;
    }

    private final Vector2f controlPoint;

    @Override
    public float getDistanceField(Vector2f pos) {
        return sdf(pos, entrance.point, controlPoint, exit.point);
    }

    //https://www.shadertoy.com/view/MlKcDD
    private float sdf(Vector2f pos, Vector2f A, Vector2f B, Vector2f C) {
        Vector2f a = new Vector2f(B).sub(A);
        Vector2f b = new Vector2f(A).sub(new Vector2f(B).mul(2)).add(C);
        Vector2f c = new Vector2f(a).mul(2.0f);
        Vector2f d = new Vector2f(A).sub(pos);

        float kk = 1.0f/dot(b,b);
        float kx = kk * dot(a,b);
        float ky = kk * (2.0f*dot(a,a)+dot(d,b))/3.0f;
        float kz = kk * dot(d,a);      
    
        float res = 0.0f;
    
        float p  = ky - kx*kx;
        float q  = kx*(2.0f*kx*kx - 3.0f*ky) + kz;
        float p3 = p*p*p;
        float q2 = q*q;
        float h  = q2 + 4.0f*p3;
    
        if( h>=0.0f ) 
        {   // 1 root
            h = sqrt(h);
            Vector2f x = (new Vector2f(h,-h).sub(new Vector2f(q,q))).mul(0.5f);
    
            Vector2f uv = new Vector2f(
                (float)(sign(x.x)*Math.pow(Math.abs(x.x), 1.0f/3.0f)),
                (float)(sign(x.y)*Math.pow(Math.abs(x.y), 1.0f/3.0f))
            );
            float t = clamp( uv.x+uv.y-kx, 0.0f, 1.0f );
            Vector2f q3 = new Vector2f(d).add((new Vector2f(c).add(new Vector2f(b).mul(t))).mul(t));
            res = dot(q3,q3);
        }
        else 
        {   // 3 roots
            float z = sqrt(-p);
            float v = ((float)Math.acos((double)(q/(p*z*2.0f))))/3.0f;
            float m = (float)Math.cos((double)v);
            float n = (float)(Math.sin((double)v)*1.732050808);
            Vector3f t = clamp(new Vector3f(m+m,-n-m,n-m).mul(z).sub(kx, kx, kx), 0.0f, 1.0f );

            Vector2f qx = new Vector2f(d).add((new Vector2f(c).add(new Vector2f(b).mul(t.x))).mul(t.x));
            float dx = dot(qx, qx);

            Vector2f qy = new Vector2f(d).add((new Vector2f(c).add(new Vector2f(b).mul(t.y))).mul(t.y));
            float dy = dot(qy, qy);

            if( dx<dy ) { 
                res=dx;
            } else {
                res=dy;
            }
        }
        
        return sqrt(res);
    }

    private float sign(float x) {
        if (x < 0) return -1;
        if (x > 0) return 1;
        return 0;
    }

    private Vector3f clamp(Vector3f v, float min, float max) {
        return new Vector3f(clamp(v.x, min, max), clamp(v.y, min, max), clamp(v.z, min, max));
    }

    private float clamp(float x, float min, float max) {
        if (x <= min) return min;
        if (x >= max) return max;
        return x;
    }

    private float sqrt(float x) {
        return (float)Math.sqrt((double)x);
    }

    private float dot(Vector2f a, Vector2f b) {
        return new Vector2f(a).dot(b);
    }

    @Override
    public Approximation getApproximation() {
        int minX = (int)Math.min(Math.min(entrance.point.x, exit.point.x), controlPoint.x);
        int minY = (int)Math.min(Math.min(entrance.point.y, exit.point.y), controlPoint.y);
        int maxX = (int)Math.max(Math.max(entrance.point.x, exit.point.x), controlPoint.x);
        int maxY = (int)Math.max(Math.max(entrance.point.y, exit.point.y), controlPoint.y);
        return new Approximation(minX, minY, maxX, maxY, new Vector2f[]{entrance.point, controlPoint, exit.point});
    }
}
