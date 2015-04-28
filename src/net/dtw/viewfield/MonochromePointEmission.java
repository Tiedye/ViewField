/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.viewfield;

import net.dtw.util.Vec2d;

/**
 *
 * @author Daniel
 */
public class MonochromePointEmission {
    
    public Vec2d position;
    public double intensity;

    public MonochromePointEmission(Vec2d position, double intensity) {
        this.position = position;
        this.intensity = intensity;
    }

    public MonochromePointEmission() {
        this (new Vec2d(), 0.0);
    }
    
}
