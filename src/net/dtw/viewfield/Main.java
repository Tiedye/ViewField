/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.viewfield;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;
import java.util.HashSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.dtw.util.Vec2d;
import net.dtw.util.Vec2i;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/**
 *
 * @author Daniel
 */
public class Main extends JPanel {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CellGrid grid = new CellGrid(120, 120, 5.0);
        //grid.setSolid(new Vec2i(6, 5));
        //MonochromePointEmission light = new MonochromePointEmission(new Vec2d(5.5, 10.5), 0.2);
        //light.setAttenuationFunction((d, v) -> Math.sin(d/2)*v);
        //grid.addLight(light);
        //grid.addLight(new MonochromePointEmission(new Vec2d(5.5, 17.5), 1));
        /*grid.setSolid(new Vec2i(30, 30));
        grid.setSolid(new Vec2i(30, 31));
        grid.setSolid(new Vec2i(30, 32));*/
        /*for (int x = 2; x < 38; x++) {
            grid.setSolid(new Vec2i(x, 2));
            grid.setSolid(new Vec2i(x, 37));
        }
        for (int y = 3; y < 37; y++) {
            grid.setSolid(new Vec2i(2, y));
            grid.setSolid(new Vec2i(37, y));
        }*/
        JFrame f = new JFrame("Jumbled Image");
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        f.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_R:
        //                light.renderQuadrant(MonochromePointEmission.Q1, new HashSet<>());
                        grid.updateGrid();
                        break;
                    default:
                        
                }
            }

        });
        grid.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                //light.setPosition(e.getX()/grid.scale + 1, e.getY()/grid.scale + 1);
                //grid.updateGrid();
                //f.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (e.isControlDown()) {
                    grid.setEmpty(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).toVec2i());
                }else {
                    grid.setSolid(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).toVec2i());
                }
                grid.updateGrid();
                f.repaint();
            }
            
        });
        grid.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    MonochromePointEmission light = new MonochromePointEmission(new Vec2d(e.getX()/grid.scale + 1, e.getY()/grid.scale + 1), 0.2);
                    //light.setAttenuationFunction((d, v) -> Math.sin(d)*v);
                    grid.addLight(light);
                    f.repaint();
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.isControlDown()) {
                        grid.setEmpty(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).toVec2i());
                    }else {
                        grid.setSolid(new Vec2d(e.getX()/grid.scale, e.getY()/grid.scale).toVec2i());
                    }
                    grid.updateGrid();
                    f.repaint();
                }
            }
        
        });
        
        JButton button = new JButton("Remove Light");
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                grid.removeLight();
                f.repaint();
            }
            
        });
        Insets insets = f.getInsets();
        Dimension size = button.getPreferredSize();
        button.setBounds(5 + insets.left, 5 + insets.top, size.width, size.height);
        f.add(button);
        f.add(grid);
        f.pack();
        
        f.setVisible(true);
        // Create grid object
        // set wall locations
        // set view location(s)
        
        // initialize glfw
        // render loop
        //  - draw grid w/ shader
    }
    
}
