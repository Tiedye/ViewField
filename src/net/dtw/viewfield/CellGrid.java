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
import java.util.Iterator;
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
    private double[][] intensityGrid;
    private HashSet<MonochromePointEmission>[][] gridOfConsequence;

    public double scale;

    private AABBi bounds;

    private int width;
    private int height;

    private HashSet<MonochromePointEmission> lights;

    public CellGrid(int width, int height, double scale) {
        this.scale = scale;
        // one cell buffer on every side
        this.width = width + 2;
        this.height = height + 2;
        bounds = new AABBi(height + 1, 1, width + 1, 1);
        opaqueCells = new HashSet<>();
        intensityGrid = new double[width + 2][height + 2];
        lights = new HashSet<>();
        gridOfConsequence = new HashSet[width + 2][height + 2];
        for (int x = 0; x < width + 2; x++) {
            for (int y = 0; y < height + 2; y++) {
                gridOfConsequence[x][y] = new HashSet<>();
            }
        }
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
    
    public AABBi getGridBounds(){
        return bounds.copy();
    }

    public void setSolid(Vec2i p) {
        opaqueCells.add(p.add(1, 1));
        Vec2i[] cells = new Vec2i[1];
        cells[0] = p;
        lights.addAll(GridUtils.getItem(gridOfConsequence, p));
        for(MonochromePointEmission light : lights) {
            light.updateCells(cells, intensityGrid, gridOfConsequence, opaqueCells);
        }
    }

    public void setEmpty(Vec2i p) {
        opaqueCells.remove(p.add(1, 1));
        Vec2i[] cells = new Vec2i[8];
        cells[0] = p.sum(0, 1);
        cells[1] = p.sum(1, 1);
        cells[2] = p.sum(1, 0);
        cells[3] = p.sum(1, -1);
        cells[4] = p.sum(0, -1);
        cells[5] = p.sum(-1, -1);
        cells[6] = p.sum(-1, 0);
        cells[7] = p.sum(-1, 1);
        HashSet<MonochromePointEmission> lightsToBeUpdated = new HashSet<>();
        for (Vec2i cell:cells) {
            lightsToBeUpdated.addAll(GridUtils.getItem(gridOfConsequence, cell));
        }
        for(MonochromePointEmission light : lightsToBeUpdated) {
            light.updateCells(cells, intensityGrid, gridOfConsequence, opaqueCells);
        }
    }

    public void toggleCell(Vec2i p) {
        if (opaqueCells.contains(p.add(1, 1))) {
            opaqueCells.remove(p.add(1, 1));
        } else {
            opaqueCells.add(p.add(1, 1));
        }
    }

    public void addLight(MonochromePointEmission light) {
        light.setRenderBounds(bounds);
        light.renderAll(opaqueCells);
        light.transferAll(intensityGrid, gridOfConsequence);
        lights.add(light);
    }

    public void updateGrid(){
        for(MonochromePointEmission light: lights){
            if (light.isChangeQueued()) {
                light.clearAll(intensityGrid, gridOfConsequence);
                light.renderAll(opaqueCells);
                light.transferAll(intensityGrid, gridOfConsequence);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int y = height - 1; y > 1; y--) {
            for (int x = 1; x < width - 1; x++) {
                output.append(intensityGrid[x][y - 1] > 0.0 ? "1 " : "0 ");
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
                    float intensity = (float) Math.sqrt(intensityGrid[x + 1][y + 1]);
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
