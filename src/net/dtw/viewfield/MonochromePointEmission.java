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

    public Vec2d position;
    public double intensity;
    
    AABBi bounds;

    private int[][] cells;

    private Double[][] sSlopeGrid;
    private Double[][] sSlopeOverflowGrid;
    private Double[][] eSlopeGrid;
    private Double[][] eSlopeOverflowGrid;

    private static final int CELLSTATE_EMPTY = 0;
    private static final int CELLSTATE_LIT = 1;
    private static final int CELLSTATE_ERAY = 2;
    private static final int CELLSTATE_SRAY = 4;

    public MonochromePointEmission(Vec2d position, double intensity) {
        this.position = position;
        this.intensity = intensity;
    }

    public MonochromePointEmission() {
        this(new Vec2d(), 0.0);
    }
    
    public void setRenderBounds(AABBi bounds) {
        this.bounds = bounds;
        
        cells = new int[bounds.getWidth()+2][bounds.getHeight()+2];
        
        sSlopeGrid = new Double[bounds.getWidth()+2][bounds.getHeight()+2];
        sSlopeOverflowGrid = new Double[bounds.getWidth()+2][bounds.getHeight()+2];
        eSlopeGrid = new Double[bounds.getWidth()+2][bounds.getHeight()+2];
        eSlopeOverflowGrid = new Double[bounds.getWidth()+2][bounds.getHeight()+2];
    }

    public void render(HashSet<Vec2i> opaqueCells, AABBi bounds) {
        Vec2i dV = new Vec2i(-1, 1); // quadrant vector
        //  \     |     /
        //    3   |   0
        //      \ | /
        // ----------------
        //      / | \
        //    2   |   1
        //  /     |     \

        AABBi fullField = bounds.expand(1, 1, 1, 1);
        
        for (int i = 0; i < 4; i++) {
            // dV is the quadrant vector
            dV = dV.rotate(Math.PI / 2);
            renderQuadrant(dV, opaqueCells, bounds);
        }
    }

    public void renderQuadrant(Vec2i q, HashSet<Vec2i> opaqueCells, AABBi bounds) {
        Vec2i sC = new Vec2i((int) position.x, (int) position.y); // quadrant corner (start cell)
        if ((double) sC.x == position.x && q.x == -1) {
            sC.x--;
        }
        if ((double) sC.y == position.y && q.y == -1) {
            sC.y--;
        }

        //AABBi bound = bounds.intersect(bounds.translate(sC.sum(new Vec2i(-1, -1))));
        ArrayDeque<Vec2i> evaluationQueue = new ArrayDeque<>();
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
                Vec2i cTY = cell.sum(q.projectY());
                Vec2i cTX = cell.sum(q.projectX());
                Vec2i cRX = cell.sum(q.reflectX());
                Vec2i cTV = cell.sum(q);
                if (opaqueCells.contains(cell)) {
                    // if the cell is opaque try to create new eRay
                    if ((state & CELLSTATE_SRAY) == 0 && !opaqueCells.contains(cTX) && !opaqueCells.contains(cRX)) {
                        // if the cell is an sRay, then we're done
                        GridUtils.orItem(cells, cTX, CELLSTATE_ERAY);
                        double slope = (cTX.y + (q.y == -1 ? 1 : 0) - position.y) / (cTX.x + (q.x == -1 ? 1 : 0) - position.x);
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
                            GridUtils.setItem(sSlopeOverflowGrid, cTY, slopeOverflow - q.y * q.x);
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
                            GridUtils.setItem(eSlopeOverflowGrid, cTY, slopeOverflow - q.y * q.x);
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
                            double slope = (cTX.y + (q.y == -1 ? 1 : 0) - position.y) / (cTX.x + (q.x == -1 ? 1 : 0) - position.x);
                            GridUtils.setItem(sSlopeGrid, cTX, slope);
                            GridUtils.setItem(sSlopeOverflowGrid, cTX, slope);
                        }
                    }
                }
                cell.y += q.y;
            }
        }

    }
    
    public int getCellState(Vec2i p) {
        return GridUtils.getItem(cells, p);
    }

}
