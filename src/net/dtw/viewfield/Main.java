/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.viewfield;

import java.awt.GridBagConstraints;
import java.awt.event.*;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.dtw.util.Vec2d;
import net.dtw.util.Vec2i;

/**
 *
 * @author Daniel
 */
public class Main extends JPanel {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CellGrid grid = new CellGrid(60, 40, 15.0);
        MonochromePointEmission light = new MonochromePointEmission(new Vec2d(5, 5), 0.2);
        grid.addLight(light);
        //grid.addLight(new MonochromePointEmission(new Vec2d(5.5, 17.5), 1));
        /*grid.setSolid(new Vec2i(30, 30));
        grid.setSolid(new Vec2i(30, 31));
        grid.setSolid(new Vec2i(30, 32));*/
        for (int x = 2; x < 38; x++) {
            grid.setSolid(new Vec2i(x, 2));
            grid.setSolid(new Vec2i(x, 37));
        }
        for (int y = 3; y < 37; y++) {
            grid.setSolid(new Vec2i(2, y));
            grid.setSolid(new Vec2i(37, y));
        }
        grid.reRenderScene();
        JFrame f = new JFrame("Jumbled Image");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        f.add("Center", grid);
        f.pack();
        grid.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                light.position = new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).sum(new Vec2d(1, 1));
                grid.reRenderScene();
                f.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (e.isControlDown()) {
                    grid.setEmpty(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).toVec2i());
                }else {
                    grid.setSolid(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).toVec2i());
                }
                grid.reRenderScene();
                f.repaint();
            }
            
            
            
        });
        grid.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                grid.addLight(new MonochromePointEmission(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).sum(new Vec2d(1, 1)), 0.2));
                grid.reRenderScene();
                f.repaint();
            }
        
        });
        f.setVisible(true);
        // Create grid object
        // set wall locations
        // set view location(s)
        
        // initialize glfw
        // render loop
        //  - draw grid w/ shader
    }
    
}
