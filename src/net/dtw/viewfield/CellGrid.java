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
import java.util.HashSet;
import net.dtw.util.AABBi;
import net.dtw.util.GridUtils;
import net.dtw.util.Vec2d;
import net.dtw.util.Vec2i;

/**
 *
 * @author Daniel
 */
public class CellGrid extends Component {

    private HashSet<Vec2i> opaqueCells;
    private double[][] activeGrid;

    public double scale;

    private AABBi bounds;

    private int width;
    private int height;

    private HashSet<MonochromePointEmission> lights;
    private HashSet<MonochromePointEmission> unrenderedLights;

    public CellGrid(int width, int height, double scale) {
        this.scale = scale;
        // one cell buffer on every side
        this.width = width + 2;
        this.height = height + 2;
        bounds = new AABBi(height + 1, 1, width + 1, 1);
        opaqueCells = new HashSet<>();
        activeGrid = new double[width + 2][height + 2];
        lights = new HashSet<>();
        unrenderedLights = new HashSet<>();

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
        } else {
            opaqueCells.add(p.add(1, 1));
        }
    }

    public void addLight(MonochromePointEmission light) {
        lights.add(light);
        unrenderedLights.add(light);
    }

    public void reRenderScene() {
        // reset the grid
        // reset the rendered lights
        activeGrid = new double[width][height];
        unrenderedLights = new HashSet<>();
        unrenderedLights.addAll(lights);
        renderRemainingScene();
    }

    public void renderRemainingScene() {
        for (MonochromePointEmission light : unrenderedLights) {
            renderLight(light);
        }
    }

    private static final int CELLSTATE_EMPTY = 0;
    private static final int CELLSTATE_LIT = 1;
    private static final int CELLSTATE_ERAY = 2;
    private static final int CELLSTATE_SRAY = 4;

    private void renderLight(MonochromePointEmission light) {

        int[][] cells = new int[width][height];
        Double[][] sSlopeGrid = new Double[width][height];
        Double[][] sSlopeOverflowGrid = new Double[width][height];
        Double[][] eSlopeGrid = new Double[width][height];
        Double[][] eSlopeOverflowGrid = new Double[width][height];

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
            dV = dV.rotate(Math.PI / 2);

            Vec2i sC = new Vec2i((int) light.position.x, (int) light.position.y); // quadrant corner (start cell)
            if ((double) sC.x == light.position.x && dV.x == -1) {
                sC.x--;
            }
            if ((double) sC.y == light.position.y && dV.y == -1) {
                sC.y--;
            }

            if (!bounds.inBounds(sC)) {
                break;
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
                    Vec2i cTY = cell.sum(dV.projectY());
                    Vec2i cTX = cell.sum(dV.projectX());
                    Vec2i cRX = cell.sum(dV.reflectX());
                    Vec2i cTV = cell.sum(dV);
                    if (opaqueCells.contains(cell)) {
                        // if the cell is opaque try to create new eRay
                        if ((state & CELLSTATE_SRAY) == 0 && !opaqueCells.contains(cTX) && !opaqueCells.contains(cRX)) {
                            // if the cell is an sRay, then we're done
                            GridUtils.orItem(cells, cTX, CELLSTATE_ERAY);
                            double slope = (cTX.y + (dV.y == -1 ? 1 : 0) - light.position.y) / (cTX.x + (dV.x == -1 ? 1 : 0) - light.position.x);
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
                                double slope = (cTX.y + (dV.y == -1 ? 1 : 0) - light.position.y) / (cTX.x + (dV.x == -1 ? 1 : 0) - light.position.x);
                                GridUtils.setItem(sSlopeGrid, cTX, slope);
                                GridUtils.setItem(sSlopeOverflowGrid, cTX, slope);
                            }
                        }
                    }
                    cell.y += dV.y;
                }
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if ((cells[x][y] & CELLSTATE_LIT) != 0) {
                    activeGrid[x][y] += light.intensity/light.position.diff(new Vec2d(x, y)).magnitude();
                }
                /*if ((cells[x][y] & CELLSTATE_ERAY) != 0) {
                    activeGrid[x][y] += 0.2;
                }
                if ((cells[x][y] & CELLSTATE_SRAY) != 0) {
                    activeGrid[x][y] += 0.6;
                }
                if ((cells[x][y] & CELLSTATE_LIT) != 0) {
                    activeGrid[x][y] += 0.1;
                }*/
                
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int y = height - 1; y > 1; y--) {
            for (int x = 1; x < width - 1; x++) {
                output.append(activeGrid[x][y - 1] > 0.0 ? "1 " : "0 ");
            }
            output.append('\n');
        }
        return output.toString();
    }

    @Override
    public void paint(Graphics g) {
        for (int y = 0; y < height - 2; y++) {
            for (int x = 0; x < width - 2; x++) {
                try {
                    //g.setColor(new Color(0.0f, (activeGrid[x + 1][y + 1] * 10) % 2 > 0.5 ? 0.5f : 0.0f, (float) activeGrid[x + 1][y + 1]));
                    float intensity = (float) Math.sqrt(activeGrid[x + 1][y + 1]);
                    g.setColor(new Color(opaqueCells.contains(new Vec2i(x + 1, y + 1)) ? 0.5f : intensity, intensity, intensity));
                } catch (RuntimeException e) {
                    g.setColor(Color.white);
                }
                g.fillRect((int) (x * scale), (int) (y * scale), (int) scale, (int) scale);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) ((width - 2) * scale), (int) ((height - 2) * scale));
    }

}
