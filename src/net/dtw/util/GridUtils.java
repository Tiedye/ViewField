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
public class GridUtils {
    
    public static <T> T getItem(T[][] grid, Vec2i p) {
        return grid[p.x][p.y];
    }
    
    public static <T> void setItem(T[][] grid, Vec2i p, T item) {
        grid[p.x][p.y] = item;
    }
    
}
