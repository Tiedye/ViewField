/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.viewfield;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.dtw.util.AABBi;
import net.dtw.util.GridUtils;
import net.dtw.util.Vec2d;
import net.dtw.util.Vec2i;

/**
 *
 * @author Daniel
 */
public class MonochromePointEmission {
    
    private static final int Q1 = 3;
    private static final int Q2 = 2;
    private static final int Q3 = 0;
    private static final int Q4 = 1;
    
    private static final Vec2i[] QDV = {new Vec2i(1, 1), new Vec2i(1, -1), new Vec2i(-1, -1), new Vec2i(-1, 1)};

    private static final int CELLSTATE_EMPTY = 0;
    private static final int CELLSTATE_LIT = 1;
    private static final int CELLSTATE_ERAY = 2;
    private static final int CELLSTATE_SRAY = 4;

    public Vec2d position;
    public double intensity;
    
    private AABBi bounds;

    private int[][] cells;

    private Double[][] sSlopeGrid;
    private Double[][] sSlopeOverflowGrid;
    private Double[][] eSlopeGrid;
    private Double[][] eSlopeOverflowGrid;
    
    private Double[] blankDoubleLine;
    private int[] blankIntLine;
    
    private int renderAreaWidth;
    private int renderAreaHeight;
    
    private final boolean[] qRendered;

    private final ArrayDeque<Vec2i> evaluationQueue;
    
    public MonochromePointEmission(Vec2d position, double intensity) {
        this.position = position;
        this.intensity = intensity;
        qRendered = new boolean[4];
        evaluationQueue = new ArrayDeque<>();
    }

    public MonochromePointEmission() {
        this(new Vec2d(), 0.0);
    }
    
    public void setRenderBounds(AABBi bounds) {
        this.bounds = bounds;
        
        renderAreaWidth = bounds.getWidth()+2;
        renderAreaHeight = bounds.getHeight()+2;
        
        cells = new int[renderAreaWidth][renderAreaHeight];
        
        sSlopeGrid = new Double[renderAreaWidth][renderAreaHeight];
        sSlopeOverflowGrid = new Double[renderAreaWidth][renderAreaHeight];
        eSlopeGrid = new Double[renderAreaWidth][renderAreaHeight];
        eSlopeOverflowGrid = new Double[renderAreaWidth][renderAreaHeight];
        
        blankDoubleLine = new Double[renderAreaHeight];
        blankIntLine = new int[renderAreaHeight];
        
    }

    public void render(HashSet<Vec2i> opaqueCells) {
        Vec2i dV = new Vec2i(-1, 1); // quadrant vector
        //  \     |     /
        //    3   |   0
        //      \ | /
        // ----------------
        //      / | \
        //    2   |   1
        //  /     |     \

        // clear all of the cells information holders
        for (int x = 0; x < renderAreaWidth; x++) {
            System.arraycopy(blankIntLine, 0, cells[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, sSlopeGrid[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, sSlopeOverflowGrid[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, eSlopeGrid[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, eSlopeOverflowGrid[x], 0, renderAreaHeight);
        }        
        
        for (int q = Q1; q <= Q4; q++){
            qRendered[q] = false;
            renderQuadrant(q, opaqueCells);
        }
    }

    public void renderQuadrant(int q, HashSet<Vec2i> opaqueCells) {
        
        Vec2i dV = QDV[q];
        
        Vec2i sC = new Vec2i((int) position.x, (int) position.y); // quadrant corner (start cell)
        if ((double) sC.x == position.x && dV.x == -1) {
            sC.x--;
        }
        if ((double) sC.y == position.y && dV.y == -1) {
            sC.y--;
        }

        if (!bounds.inBounds(sC)) {
            return;
        }
        
        // TODO: investigate the effect of the lower row being rendered
        if(qRendered[q]){
            // clear that quadrant if it has been rendered
            boolean pX = (q & 0b01) != 0;
            boolean pY = (q & 0b10) != 0;
            int x = pX ? sC.x : 0;
            int y = pY ? sC.y : 0;
            int xe = (pX ? renderAreaWidth - x : sC.x) + x;
            int h = pY ? renderAreaHeight - y : sC.y; 
            // iterate along the x axis
            for (int i = x; i < xe; i++){
                System.arraycopy(blankIntLine, 0, cells[i], y, h);
                System.arraycopy(blankDoubleLine, 0, sSlopeGrid[i], y, h);
                System.arraycopy(blankDoubleLine, 0, sSlopeOverflowGrid[i], y, h);
                System.arraycopy(blankDoubleLine, 0, eSlopeGrid[i], y, h);
                System.arraycopy(blankDoubleLine, 0, eSlopeOverflowGrid[i], y, h);
            }
        }
        qRendered[q] = true;

        //AABBi bound = bounds.intersect(bounds.translate(sC.sum(new Vec2i(-1, -1))));
        Vec2i cI = sC.copy();
        GridUtils.orItem(cells, cI, CELLSTATE_SRAY);
        evaluationQueue.add(cI);
        GridUtils.setItem(sSlopeGrid, cI, 0.0);
        GridUtils.setItem(sSlopeOverflowGrid, cI, 0.0);

        while (!evaluationQueue.isEmpty()) {
            Vec2i cell = evaluationQueue.remove();

            if (!bounds.inBounds(cell)) {
                break;
            }

            while (bounds.inBounds(cell)) {
                int state = GridUtils.getItem(cells, cell);
                Vec2i cTY = cell.sum(dV.projectY());
                Vec2i cTX = cell.sum(dV.projectX());
                Vec2i cRX = cell.sum(dV.reflectX());
                Vec2i cTV = cell.sum(dV);
                if (opaqueCells.contains(cell)) {
                    // if the cell is opaque try to create new eRay
                    if ((state & CELLSTATE_SRAY) == 0 && !opaqueCells.contains(cTX) && !opaqueCells.contains(cRX)) {
                        // if the cell is an sRay, then we're done
                        GridUtils.orItem(cells, cTX, CELLSTATE_ERAY);
                        double slope = (cTX.y + (dV.y == -1 ? 1 : 0) - position.y) / (cTX.x + (dV.x == -1 ? 1 : 0) - position.x);
                        GridUtils.setItem(eSlopeGrid, cTX, slope);
                        GridUtils.setItem(eSlopeOverflowGrid, cTX, slope);
                    }
                    break;
                } else {
                    GridUtils.orItem(cells, cell, CELLSTATE_LIT);
                    if ((state & CELLSTATE_SRAY) != 0) {
                        // extend sRays
                        double slope = GridUtils.getItem(sSlopeGrid, cell);
                        double slopeOverflow = GridUtils.getItem(sSlopeOverflowGrid, cell);
                        if (Math.abs(slopeOverflow) > 1.0) {
                            GridUtils.setItem(sSlopeGrid, cTY, slope);
                            GridUtils.setItem(sSlopeOverflowGrid, cTY, slopeOverflow - dV.y * dV.x);
                            GridUtils.orItem(cells, cTY, CELLSTATE_SRAY);
                        } else if (Math.abs(slopeOverflow) < 1.0) {
                            GridUtils.setItem(sSlopeGrid, cTX, slope);
                            GridUtils.setItem(sSlopeOverflowGrid, cTX, slopeOverflow + slope);
                            GridUtils.orItem(cells, cTX, CELLSTATE_SRAY);
                            evaluationQueue.add(cTX);
                        } else if (Math.abs(slopeOverflow) == 1.0) {
                            // genetated by this stucture
                            // .....*00000
                            // ...../00000
                            // ..../000000
                            // ....*000000
                            // ....*000000
                            if (!opaqueCells.contains(cTY) && !opaqueCells.contains(cTV)) {
                                GridUtils.setItem(sSlopeGrid, cTV, slope);
                                GridUtils.setItem(sSlopeOverflowGrid, cTV, slope);
                                GridUtils.orItem(cells, cTV, CELLSTATE_SRAY);
                                evaluationQueue.add(cTV);
                            }
                        }
                    }
                    if ((state & CELLSTATE_ERAY) != 0) {
                        // extend eRays
                        double slope = GridUtils.getItem(eSlopeGrid, cell);
                        double slopeOverflow = GridUtils.getItem(eSlopeOverflowGrid, cell);
                        if (Math.abs(slopeOverflow) > 1.0) {
                            GridUtils.setItem(eSlopeGrid, cTY, slope);
                            GridUtils.setItem(eSlopeOverflowGrid, cTY, slopeOverflow - dV.y * dV.x);
                            GridUtils.orItem(cells, cTY, CELLSTATE_ERAY);
                        } else if (Math.abs(slopeOverflow) < 1.0) {
                            GridUtils.setItem(eSlopeGrid, cTX, slope);
                            GridUtils.setItem(eSlopeOverflowGrid, cTX, slopeOverflow + slope);
                            GridUtils.orItem(cells, cTX, CELLSTATE_ERAY);
                            break;
                        } else if (Math.abs(slopeOverflow) == 1.0) {
                            // genetated by this stucture
                            // ...*0000000
                            // ...*0/*0000
                            // ...*/..0000
                            // .......0000
                            // .......0000
                            if (!opaqueCells.contains(cTX) && !opaqueCells.contains(cTV)) {
                                GridUtils.setItem(eSlopeGrid, cTV, slope);
                                GridUtils.setItem(eSlopeOverflowGrid, cTV, slope);
                                GridUtils.orItem(cells, cTV, CELLSTATE_ERAY);
                                break;
                            }
                        }
                    }
                    if ((state & CELLSTATE_SRAY) == 0) {
                        // if this isnt in a sRay, try to create new sRays
                        if (!opaqueCells.contains(cTX) && opaqueCells.contains(cRX)) {
                            GridUtils.orItem(cells, cTX, CELLSTATE_SRAY);
                            evaluationQueue.add(cTX);
                            double slope = (cTX.y + (dV.y == -1 ? 1 : 0) - position.y) / (cTX.x + (dV.x == -1 ? 1 : 0) - position.x);
                            GridUtils.setItem(sSlopeGrid, cTX, slope);
                            GridUtils.setItem(sSlopeOverflowGrid, cTX, slope);
                        }
                    }
                }
                cell.y += dV.y;
            }
        }

    }
    
    public int getCellState(Vec2i p) {
        return GridUtils.getItem(cells, p);
    }

}
