/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.util;

/**
 *
 * @author Daniel
 */
public class Vec2d {
    
    public double x;
    public double y;

    public Vec2d(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vec2d() {
        this(0, 0);
    }
    
    public Vec2d sum(Vec2d v) {
        return new Vec2d(x + v.x, y + v.y);
    }
    
    public Vec2d diff(Vec2d v) {
        return new Vec2d(x - v.x, y - v.y);
    }
    
    public Vec2d scale(double n) {
        return new Vec2d(x*n, y*n);
    }
    
    public Vec2d rotate(double r) {
        return new Vec2d(Math.cos(r)*x - Math.sin(r)*y, Math.sin(r)*x + Math.cos(r)*y);
    }
    
    public double magnitude() {
        return Math.hypot(x, y);
    }
    
    public double dot(Vec2d v) {
        return x*v.x + y*v.y;
    }
    
    public double cross(Vec2d v) {
        return x*v.y - y*v.x;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ')';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Vec2d other = (Vec2d) obj;
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        return true;
    }
    
    public Vec2i toVec2i(){
        return new Vec2i((int)x, (int)y);
    }
    
}
