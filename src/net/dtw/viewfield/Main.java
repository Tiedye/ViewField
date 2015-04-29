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
        CellGrid grid = new CellGrid(20, 20, 30.0);
        MonochromePointEmission light = new MonochromePointEmission(new Vec2d(5, 5), 1);
        grid.addLight(light);
        grid.addLight(new MonochromePointEmission(new Vec2d(3, 3), 1));
        /*grid.setSolid(new Vec2i(30, 30));
        grid.setSolid(new Vec2i(30, 31));
        grid.setSolid(new Vec2i(30, 32));*/
        grid.setSolid(new Vec2i(9, 9));
        grid.setSolid(new Vec2i(9, 10));
        grid.setSolid(new Vec2i(9, 11));
        grid.reRenderScene();
        JFrame f = new JFrame("Jumbled Image");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        f.add("Center", grid);
        f.pack();
        f.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                light.position = new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).sum(new Vec2d(0.5, 0.0));
                grid.reRenderScene();
                f.repaint();
            }
            
        });
        f.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                grid.toggleCell(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).sum(new Vec2d(-0.5, -1.0)).toVec2i());
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
