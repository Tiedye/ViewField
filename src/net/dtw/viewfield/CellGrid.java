/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.viewfield;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import net.dtw.util.AABBi;
import net.dtw.util.Vec2i;

/**
 *
 * @author Daniel
 */
public class CellGrid extends Component {

    private HashSet<Vec2i> opaqueCells;
    private double[][] baseGrid;
    private double[][] activeGrid;
    
    public double scale;

    private AABBi bounds;

    private int width;
    private int height;

    private ArrayList<MonochromePointEmission> lights;
    private int currentLight;

    public CellGrid(int width, int height, double scale) {
        this.scale = scale;
        // one cell buffer on every side
        this.width = width + 2;
        this.height = height + 2;
        bounds = new AABBi(height + 1, 1, width + 1, 1);
        opaqueCells = new HashSet<>();
        baseGrid = new double[width + 2][height + 2];
        activeGrid = new double[width + 2][height + 2];
        lights = new ArrayList<>();
        currentLight = 0;

    }

    public CellGrid(boolean[][] solidCells) {
        this(solidCells.length, solidCells[0].length, 5.0);
        for (int x = 0; x < width - 2; x++) {
            for (int y = 0; y < height - 2; y++) {
                if (solidCells[x][y]) {
                    opaqueCells.add(new Vec2i(x + 1, y + 1));
                }
            }
        }
    }

    public void setSolid(Vec2i p) {
        opaqueCells.add(p.add(1, 1));
    }

    public void setEmpty(Vec2i p) {
        opaqueCells.remove(p.add(1, 1));
    }
    
    public void toggleCell(Vec2i p) {
        if (opaqueCells.contains(p.add(1, 1))) {
            opaqueCells.remove(p.add(1, 1));
        }else{
            opaqueCells.add(p.add(1, 1));
        }
    }

    public void addLight(MonochromePointEmission light) {
        lights.add(light);
    }

    public void calulateCells() {
        for (; currentLight < lights.size(); currentLight++) {
            MonochromePointEmission light = lights.get(currentLight);

            HashSet<Vec2i> lightFill = new HashSet<>();

            Vec2i dV = new Vec2i(-1, 1); // direction vector

            // fill specs
            ArrayDeque<Vec2i> sFillQueue = new ArrayDeque<>();
            HashSet<Vec2i> eFillCells = new HashSet<>(width);

            for (int i = 0; i < 4; i++) {
                // dV is the quadrant vector
                dV = dV.rotate(Math.PI/2);

                // ray start specs
                ArrayDeque<Vec2i> sRayQueue = new ArrayDeque<>();
                ArrayDeque<Vec2i> eRayQueue = new ArrayDeque<>();
                
                //  \     |     /
                //    3   |   0
                //      \ | /
                // ----------------
                //      / | \
                //    2   |   1
                //  /     |     \
                
                Vec2i sC = new Vec2i((int) light.position.x, (int) light.position.y); // quadrant corner (start cell)
                if ((double) sC.x == light.position.x && dV.x == -1) {
                    sC.x--;
                }
                if ((double) sC.y == light.position.y && dV.y == -1) {
                    sC.y--;
                }
                // Math floor round to negative infinity, this rounds to zero
                
                
                for (Vec2i p = sC.copy(); bounds.inBounds(p); p.x += dV.x) {
                    if (!opaqueCells.contains(p)) {
                        sFillQueue.offer(p.copy());
                    } else {
                        break;
                    }
                }
                while (!sFillQueue.isEmpty()) {
                    // do fill
                    int stopRow = 0;
                    ArrayDeque<Vec2i> nextSFillQueue = new ArrayDeque<>();
                    while (!sFillQueue.isEmpty()) {
                        for (Vec2i fillPoint = sFillQueue.poll(); bounds.inBounds(fillPoint); fillPoint.y += dV.y) {
                            if (!opaqueCells.contains(fillPoint)) {
                                if (fillPoint.y == stopRow) {
                                    nextSFillQueue.offer(fillPoint.copy());
                                    break;
                                }
                                if (eFillCells.contains(fillPoint)) {
                                    lightFill.add(fillPoint.copy());
                                    break;
                                }
                                if (opaqueCells.contains(fillPoint.sum(dV.projectX()))) {
                                    if (!opaqueCells.contains(fillPoint.sum(dV)) && !opaqueCells.contains(fillPoint.sum(dV.projectY()))) {
                                        sRayQueue.offer(fillPoint.sum(dV));
                                    }
                                }else if(opaqueCells.contains(fillPoint.sum(dV.projectY()))) {
                                    if (!opaqueCells.contains(fillPoint.sum(dV)) && !opaqueCells.contains(fillPoint.sum(dV.projectX()))) {
                                        eRayQueue.offer(fillPoint.sum(dV));
                                    }
                                }
                                lightFill.add(fillPoint.copy());
                            } else {
                                stopRow = fillPoint.y;
                                break;
                            }
                        }
                    }
                    sFillQueue = nextSFillQueue;
                    // do ray tracing
                    while (!sRayQueue.isEmpty()) {
                        Vec2i sRay = sRayQueue.poll(); 
                        double slope = (sRay.y + (dV.y == -1? 1 : 0) - light.position.y) / (sRay.x + (dV.x == -1 ? 1 : 0) - light.position.x);
                        double slopeOverflow = slope;
                        outer: while (bounds.inBounds(sRay)) {
                            if (!opaqueCells.contains(sRay)) {
                                sFillQueue.offer(sRay.copy());
                            } else {
                                break;
                            }
                            if (Math.abs(slopeOverflow) > height) break;
                            while(Math.abs(slopeOverflow) > 1) {
                                if (opaqueCells.contains(sRay)) break outer;
                                sRay = sRay.sum(dV.projectY());
                                slopeOverflow -= dV.x * dV.y;
                            } 
                            if (Math.abs(slopeOverflow) == 1.0){
                                if (opaqueCells.contains(sRay.sum(dV.projectY())) || opaqueCells.contains(sRay.sum(dV.projectX()))) break;
                                sRay = sRay.sum(dV);
                                slopeOverflow = slope;
                            } else {
                                sRay = sRay.sum(dV.projectX());
                                slopeOverflow += slope;
                            }
                        }
                    }
                    while (!eRayQueue.isEmpty()) {
                        Vec2i eRay = eRayQueue.poll(); 
                        double slope = (eRay.y + (dV.y == -1? 1 : 0) - light.position.y) / (eRay.x + (dV.x == -1 ? 1 : 0) - light.position.x);
                        double slopeOverflow = slope;
                        outer: while (bounds.inBounds(eRay)) {
                            if (Math.abs(slopeOverflow) > height) break;
                            while(Math.abs(slopeOverflow) > 1) {
                                if (opaqueCells.contains(eRay)) break outer;
                                eRay = eRay.sum(dV.projectY());
                                slopeOverflow -= dV.x * dV.y;
                            } 
                            if (!opaqueCells.contains(eRay)) {
                                eFillCells.add(eRay.copy());
                            } else {
                                break;
                            }
                            if (Math.abs(slopeOverflow) == 1.0){
                                if (opaqueCells.contains(eRay.sum(dV.projectY())) || opaqueCells.contains(eRay.sum(dV.projectX()))) break;
                                eRay = eRay.sum(dV);
                                slopeOverflow = slope;
                            } else {
                                eRay = eRay.sum(dV.projectX());
                                slopeOverflow += slope;
                            }
                        }
                    }
                }
            }
            
            for (Vec2i p:lightFill) {
                activeGrid[p.x][p.y] += light.intensity/Math.sqrt(p.toVec2d().diff(light.position).magnitude());
                //activeGrid[p.x][p.y] += light.intensity;
            }
            
        }
    }

    public void reCalculateCells() {
        currentLight = 0;
        activeGrid = new double[width][height];
        /*for (int i = 0; i < width; i++) {
            System.arraycopy(baseGrid[i], 0, activeGrid[i], 0, height);
        }*/
        calulateCells();
    }
    
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int y = height - 1; y > 1; y--){
            for (int x = 1; x < width - 1; x++) {
                output.append(activeGrid[x][y-1] > 0.0 ? "1 " : "0 ");
            }
            output.append('\n');
        }
        return output.toString();
    }

    @Override
    public void paint(Graphics g) {
        for (int y = 0; y < height - 2; y++){
            for (int x = 0; x < width - 2; x++) {
                try {
                g.setColor(new Color((float)activeGrid[x+1][y+1], (float)activeGrid[x+1][y+1], (float)activeGrid[x+1][y+1]));
                } catch (RuntimeException e) {
                    g.setColor(Color.white);
                }
                g.fillRect((int)(x*scale), (int)(y*scale), (int)scale, (int)scale);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int)((width-2)*scale), (int)((height-2)*scale));
    }
    
    

}
