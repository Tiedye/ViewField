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
    
    public static int getItem(int[][] grid, Vec2i p) {
        return grid[p.x][p.y];
    }
    
    public static void setItem(int[][] grid, Vec2i p, int item) {
        grid[p.x][p.y] = item;
    }
    
    public static void orItem(int[][] grid, Vec2i p, Integer item) {
        grid[p.x][p.y] |= item;
    }
    
}
