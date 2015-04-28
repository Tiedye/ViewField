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
public class Vec2i {
    
    public int x;
    public int y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public Vec2i() {
        this(0, 0);
    }
    
    public Vec2i sum(Vec2i v) {
        return new Vec2i(x + v.x, y + v.y);
    }
    
    public Vec2i add(int x, int y) {
        return new Vec2i(this.x + x, this.y + y);
    }
    
    public Vec2i diff(Vec2i v) {
        return new Vec2i(x - v.x, y - v.y);
    }
    
    public Vec2i scale(int n) {
        return new Vec2i(x*n, y*n);
    }
    
    public Vec2i rotate(double r) {
        return new Vec2i((int)Math.round(Math.cos(r)*x + Math.sin(r)*y), (int)Math.round(-Math.sin(r)*x + Math.cos(r)*y));
    }
    
    public Vec2i project(Vec2i v) {
        throw new Error("Not yet implemented.");
    }
    
    public Vec2i projectX() {
        return new Vec2i(x, 0);
    }
    
    public Vec2i projectY() {
        return new Vec2i(0, y);
    }
    
    public double magnitude() {
        return Math.hypot(x, y);
    }
    
    public int dot(Vec2i v) {
        return x*v.x + y*v.y;
    }
    
    public int cross(Vec2i v) {
        return x*v.y - y*v.x;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")i";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.x;
        hash = 29 * hash + this.y;
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
        final Vec2i other = (Vec2i) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return true;
    }
    
    public Vec2i copy() {
        return new Vec2i(x, y);
    }
    
    public Vec2d toVec2d() {
        return new Vec2d(x, y);
    }
}
