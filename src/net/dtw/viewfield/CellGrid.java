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
import net.dtw.util.GridUtils;
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

    private ArrayList<MonochromePointEmission> lOld;
    private int currentLightOld;

    private HashSet<MonochromePointEmission> lights;
    private HashSet<MonochromePointEmission> unrenderedLights;


    public CellGrid(int width, int height, double scale) {
        this.scale = scale;
        // one cell buffer on every side
        this.width = width + 2;
        this.height = height + 2;
        bounds = new AABBi(height + 1, 1, width + 1, 1);
        opaqueCells = new HashSet<>();
        baseGrid = new double[width + 2][height + 2];
        activeGrid = new double[width + 2][height + 2];
        lOld = new ArrayList<>();
        currentLightOld = 0;

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

    public void reRenderScene(){
        // reset the grid
        // reset the rendered lights
        activeGrid = new double[width][height];
        unrenderedLights = new HashSet<>();
        unrenderedLights.addAll(lights);
        renderRemainingScene();
    }

    public void renderRemainingScene(){
        for(MonochromePointEmission light: unrenderedLights){
            renderLight(light);
        }
    }

    private enum CellState {
        EMPTY,
        LIT,
        SRAY,
        ERAY
    }

    private void renderLight(MonochromePointEmission light) {

        CellState[][] cells = new CellState[width][height];
        Double[][] slopeGrid = new Double[width][height];
        Double[][] slopeOverflowGrid = new Double[width][height];

        Vec2i dV = new Vec2i(-1, 1); // quadrant vector
        //  \     |     /
        //    3   |   0
        //      \ | /
        // ----------------
        //      / | \
        //    2   |   1
        //  /     |     \

        for (int i = 0; i < 4; i++) {
            // dV is the quadrant vector
            dV = dV.rotate(Math.PI/2);

            Vec2i sC = new Vec2i((int) light.position.x, (int) light.position.y); // quadrant corner (start cell)
            if ((double) sC.x == light.position.x && dV.x == -1) {
                sC.x--;
            }
            if ((double) sC.y == light.position.y && dV.y == -1) {
                sC.y--;
            }

            AABBi bound = bounds.intersect(bounds.translate(sC));

            ArrayDeque<Vec2i> evaluationQueue = new ArrayDeque<>();
            evaluationQueue.add(sC.copy());

            while (!evaluationQueue.isEmpty()) {
                Vec2i cell = evaluationQueue.remove();

                while (bound.inBounds(cell)) {
                    CellState state = GridUtils.getItem(cells, cell);
                    Vec2i cTY = cell.sum(dV.projectY());
                    Vec2i cTX = cell.sum(dV.projectX());
                    Vec2i cRX = cell.sum(dV.reflectX());
                    Vec2i cTV = cell.sum(dV);
                    cell.add(0, 1);
                    if (opaqueCells.contains(cell)){
                        // if the cell is opaque try to create new eRay
                        if (state != CellState.SRAY && !opaqueCells.contains(cTX) && !opaqueCells.contains(cRX)) {
                            // if the cell is an sRay, then we're done
                            GridUtils.setItem(cells, cTX, CellState.ERAY);
                            double slope = (cTX.y + (dV.y == -1? 1 : 0) - light.position.y) / (cTX.x + (dV.x == -1 ? 1 : 0) - light.position.x);
                            GridUtils.setItem(slopeGrid, cTX, slope);
                            GridUtils.setItem(slopeOverflowGrid, cTX, slope);
                        }
                        break;
                    } else {
                        if (state == CellState.EMPTY) GridUtils.setItem(cells, cell, CellState.LIT);
                        if (state != CellState.SRAY) {
                            // if this isnt in a sRay, try to create new sRays
                            if (!opaqueCells.contains(cTX) && opaqueCells.contains(cRX)) {
                                GridUtils.setItem(cells, cTX, CellState.SRAY);
                                evaluationQueue.add(cTX);
                                double slope = (cTX.y + (dV.y == -1? 1 : 0) - light.position.y) / (cTX.x + (dV.x == -1 ? 1 : 0) - light.position.x);
                                GridUtils.setItem(slopeGrid, cTX, slope);
                                GridUtils.setItem(slopeOverflowGrid, cTX, slope);
                            }
                        }
                        if (state != CellState.LIT) {
                            // extend rays
                            double slope = GridUtils.getItem(slopeGrid, cell);
                            double slopeOverflow = GridUtils.getItem(slopeOverflowGrid, cell);
                            if (Math.abs(slopeOverflow) > 1.0) {
                                GridUtils.setItem(slopeGrid, cTY, slope);
                                GridUtils.setItem(slopeOverflowGrid, cTY, slopeOverflow - 1);
                                GridUtils.setItem(cells, cTY, state);
                            } else if (Math.abs(slopeOverflow) < 1.0) {
                                GridUtils.setItem(slopeGrid, cTX, slope);
                                GridUtils.setItem(slopeOverflowGrid, cTX, slopeOverflow + slope);
                                GridUtils.setItem(cells, cTX, state);
                                if (state == CellState.SRAY) evaluationQueue.add(cTX);
                                break;
                            } else if (Math.abs(slopeOverflow) == 1.0) {
                                if (state == CellState.SRAY) {
                                    // genetated by this stucture
                                    // .....*00000
                                    // ...../00000
                                    // ..../000000
                                    // ....*000000
                                    // ....*000000
                                    if (!opaqueCells.contains(cTY) && !opaqueCells.contains(cTV)) {
                                        GridUtils.setItem(slopeGrid, cTV, slope);
                                        GridUtils.setItem(slopeOverflowGrid, cTV, slope);
                                        GridUtils.setItem(cells, cTV, state);
                                        evaluationQueue.add(cTV);
                                    }
                                } else if (state == CellState.ERAY) {
                                    // genetated by this stucture
                                    // ...*0000000
                                    // ...*0/*0000
                                    // ...*/..0000
                                    // .......0000
                                    // .......0000
                                    if (!opaqueCells.contains(cTX) && !opaqueCells.contains(cTV)) {
                                        GridUtils.setItem(slopeGrid, cTV, slope);
                                        GridUtils.setItem(slopeOverflowGrid, cTV, slope);
                                        GridUtils.setItem(cells, cTV, state);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (cells[x][y] != CellState.EMPTY) {
                    activeGrid[x][y] += light.intensity;
                }
            }
        }
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
                g.setColor(new Color((float)Math.sqrt(activeGrid[x+1][y+1]), (float)Math.sqrt(activeGrid[x+1][y+1]), (float)Math.sqrt(activeGrid[x+1][y+1])));
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
