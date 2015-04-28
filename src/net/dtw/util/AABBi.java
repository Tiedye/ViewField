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

    public AABBi(int top, int bottom, int right, int left) {
        if (top < bottom || right < left) throw new IllegalArgumentException("That's not possible.");
        this.top = top;
        this.bottom = bottom;
        this.right = right;
        this.left = left;
    }
    
    public boolean inBounds(Vec2i p) {
        return top > p.y && bottom <= p.y && right > p.x && left <= p.x;
    }
    
}
