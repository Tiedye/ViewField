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
public class AABBi {
    
    private int top;
    private int bottom;
    private int right;
    private int left;
    
    private boolean zero;

    public AABBi(int top, int bottom, int right, int left) {
        if (top < bottom || right < left) zero = true;
        else {
            this.top = top;
            this.bottom = bottom;
            this.right = right;
            this.left = left;
        }
    }
    
    public boolean inBounds(Vec2i p) {
        if (zero) return false;
        return top > p.y && bottom <= p.y && right > p.x && left <= p.x;
    }
    
    public AABBi intersect (AABBi ab) {
        return new AABBi(top < ab.top ? top : ab.top, bottom > ab.bottom ? bottom : ab.bottom, right < ab.right ? right : ab.right, left > ab.left ? left : ab.left);
    }
    
    public AABBi translate(Vec2i v){
        return new AABBi(top + v.y, bottom + v.y, right + v.x, left + v.x);
    }
    
}
