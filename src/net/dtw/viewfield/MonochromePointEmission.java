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

    private static int usedIDsS37 = 37;

    // 1's bit represents quadrant in the positive y realative to the light
    // 2's bit represents quadrant in the positive x realative to the light
    public static final int Q1 = 3;
    public static final int Q2 = 2;
    public static final int Q3 = 0;
    public static final int Q4 = 1;

    private static final Vec2i[] QDV = {new Vec2i(-1, -1), new Vec2i(-1, 1), new Vec2i(1, -1), new Vec2i(1, 1)};
        // quadrant vector
    //  \     |     /
    //    3   |   0
    //      \ | /
    // ----------------
    //      / | \
    //    2   |   1
    //  /     |     \

    private static final int CELLSTATE_EMPTY = 0;
    private static final int CELLSTATE_LIT = 1;
    private static final int CELLSTATE_ERAY = 2;
    private static final int CELLSTATE_SRAY = 4;

    private int id;

    private Vec2d nextPosition;
    private Vec2d position;
    private double nextIntensity;
    private double intensity;
    
    private boolean changeQueued;

    private AttenuationFunction attenuationFunction;

    private AABBi bounds;

    private int[][] cells;
    private Double[][] intensityCache;

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
        id = usedIDsS37;
        usedIDsS37++;
        this.position = position;
        this.intensity = intensity;
        nextIntensity = Double.NaN;
        qRendered = new boolean[4];
        evaluationQueue = new ArrayDeque<>();
        attenuationFunction = (d, v) -> d < 1.0 ? v: v / d;
        changeQueued = false;
    }

    public MonochromePointEmission() {
        this(new Vec2d(), 0.0);
    }

    public void setRenderBounds(AABBi bounds) {
        this.bounds = bounds;

        renderAreaWidth = bounds.getWidth() + 2;
        renderAreaHeight = bounds.getHeight() + 2;

        cells = new int[renderAreaWidth][renderAreaHeight];
        intensityCache = new Double[renderAreaWidth][renderAreaHeight];

        sSlopeGrid = new Double[renderAreaWidth][renderAreaHeight];
        sSlopeOverflowGrid = new Double[renderAreaWidth][renderAreaHeight];
        eSlopeGrid = new Double[renderAreaWidth][renderAreaHeight];
        eSlopeOverflowGrid = new Double[renderAreaWidth][renderAreaHeight];

        blankDoubleLine = new Double[renderAreaHeight];
        blankIntLine = new int[renderAreaHeight];

    }

    public void setAttenuationFunction(AttenuationFunction attenuationFunction) {
        this.attenuationFunction = attenuationFunction;
    }

    public void setIntensity(double intensity) {
        nextIntensity = intensity;
        changeQueued = true;
    }

    public void setPosition(double x, double y) {
        if (nextPosition == null) nextPosition = new Vec2d();
        nextPosition.x = x;
        nextPosition.y = y;
        changeQueued = true;
    }
    
    public void setPosition(Vec2d position) {
        nextPosition = position.copy();
        changeQueued = true;
    }
    
    public boolean isChangeQueued() {
        return changeQueued;
    }

    public void renderAll(HashSet<Vec2i> opaqueCells) {

        if (!Double.isNaN(nextIntensity)) {
            intensity = nextIntensity;
            nextIntensity = Double.NaN;
        }
        if (nextPosition != null) {
            position = nextPosition;
            nextPosition = null;
        }
        changeQueued = false;

        // clear all of the cells information holders
        for (int x = 0; x < renderAreaWidth; x++) {
            System.arraycopy(blankIntLine, 0, cells[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, sSlopeGrid[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, sSlopeOverflowGrid[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, eSlopeGrid[x], 0, renderAreaHeight);
            System.arraycopy(blankDoubleLine, 0, eSlopeOverflowGrid[x], 0, renderAreaHeight);
        }

        for (int q = 0; q < 4; q++) {
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
        boolean pX = (q & 0b10) != 0;
        boolean pY = (q & 0b01) != 0;
        int rAX = pX ? sC.x : 0;
        int rAY = pY ? sC.y : 0;
        int rAW = pX ? renderAreaWidth - rAX : sC.x + 1;
        int rAWX = rAW + rAX;
        int rAH = pY ? renderAreaHeight - rAY : sC.y + 1;
        int rAHY = rAH + rAY;
        if (qRendered[q]) {
            // clear that quadrant if it has been rendered
            // iterate along the x axis
            for (int i = rAX; i < rAWX; i++) {
                System.arraycopy(blankIntLine, 0, cells[i], rAY, rAH);
                System.arraycopy(blankDoubleLine, 0, sSlopeGrid[i], rAY, rAH);
                System.arraycopy(blankDoubleLine, 0, sSlopeOverflowGrid[i], rAY, rAH);
                System.arraycopy(blankDoubleLine, 0, eSlopeGrid[i], rAY, rAH);
                System.arraycopy(blankDoubleLine, 0, eSlopeOverflowGrid[i], rAY, rAH);
            }
        }
        qRendered[q] = true;

        //AABBi bound = bounds.intersect(bounds.translate(sC.sum(new Vec2i(-1, -1))));
        Vec2i cI = sC.copy();
        GridUtils.orItem(cells, cI, CELLSTATE_SRAY);
        evaluationQueue.clear();
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
        for (int x = rAX; x < rAWX; x++) {
            for (int y = rAY; y < rAHY; y++) {
                intensityCache[x][y] = attenuationFunction.execute(position.distance(x, y), intensity);
            }
        }
    }

    public void transferAll(double[][] grid, HashSet<MonochromePointEmission>[][] gridOfConsequence) {
        for (int x = 0; x < renderAreaWidth; x++) {
            for (int y = 0; y < renderAreaHeight; y++) {
                if ((cells[x][y] & CELLSTATE_LIT) != 0 && bounds.inBounds(new Vec2i(x, y)) && !gridOfConsequence[x][y].contains(this)) {
                    grid[x][y] += intensityCache[x][y];
                    gridOfConsequence[x][y].add(this);
                }
            }
        }
    }

    private void transferQuadrant(int q, double[][] grid, HashSet<MonochromePointEmission>[][] gridOfConsequence) {

        if (!qRendered[q]) {
            throw new RuntimeException("That quadrant isn't rendered.");
        }

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
        boolean pX = (q & 0b10) != 0;
        boolean pY = (q & 0b01) != 0;
        int rAX = pX ? sC.x : 0;
        int rAY = pY ? sC.y : 0;
        int rAW = pX ? renderAreaWidth - rAX : sC.x + 1;
        int rAWX = rAW + rAX;
        int rAH = pY ? renderAreaHeight - rAY : sC.y + 1;
        int rAHY = rAH + rAY;
        for (int x = rAX; x < rAWX; x++) {
            for (int y = rAY; y < rAHY; y++) {
                if ((cells[x][y] & CELLSTATE_LIT) != 0 && !gridOfConsequence[x][y].contains(this)) {
                    grid[x][y] += intensityCache[x][y];
                    gridOfConsequence[x][y].add(this);
                }
            }
        }
    }

    public void clearAll(double[][] grid, HashSet<MonochromePointEmission>[][] gridOfConsequence) {
        for (int x = 0; x < renderAreaWidth; x++) {
            for (int y = 0; y < renderAreaHeight; y++) {
                if (gridOfConsequence[x][y].contains(this)) {
                    grid[x][y] -= intensityCache[x][y];
                    gridOfConsequence[x][y].remove(this);
                }
            }
        }
    }

    private void clearQuadrant(int q, double[][] grid, HashSet<MonochromePointEmission>[][] gridOfConsequence) {

        if (!qRendered[q]) {
            throw new RuntimeException("That quadrant isn't rendered.");
        }

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
        boolean pX = (q & 0b10) != 0;
        boolean pY = (q & 0b01) != 0;
        int rAX = pX ? sC.x : 0;
        int rAY = pY ? sC.y : 0;
        int rAW = pX ? renderAreaWidth - rAX : sC.x + 1;
        int rAWX = rAW + rAX;
        int rAH = pY ? renderAreaHeight - rAY : sC.y + 1;
        int rAHY = rAH + rAY;
        for (int x = rAX; x < rAWX; x++) {
            for (int y = rAY; y < rAHY; y++) {
                if (gridOfConsequence[x][y].contains(this)) {
                    grid[x][y] -= intensityCache[x][y];
                    gridOfConsequence[x][y].remove(this);
                }
            }
        }
    }

    public void updateCells(Vec2i[] cellsToUpdate, double[][] grid, HashSet<MonochromePointEmission>[][] gridOfConsequence, HashSet<Vec2i> opaqueCells){
        boolean[] rQ = new boolean[4];
        
        Vec2i pR = position.toVec2i();
        
        for (Vec2i cell:cellsToUpdate){
            rQ[((cell.x >= pR.x) ? 0b10 : 0) + ((cell.y >= pR.y) ? 0b01 : 0)] = true;
        }
        
        for(int i = 0; i < 4; i++) {
            if (rQ[i]) {
                clearQuadrant(i, grid, gridOfConsequence);
                renderQuadrant(i, opaqueCells);
                transferQuadrant(i, grid, gridOfConsequence);
            }
        }
    }

    @Override
    public int hashCode() {
        return 71 + this.id * 3;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MonochromePointEmission other = (MonochromePointEmission) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}
