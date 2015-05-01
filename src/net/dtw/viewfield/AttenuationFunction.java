/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dtw.viewfield;

/**
 *
 * @author Daniel
 */
public interface AttenuationFunction {
    
    public double execute(double distance, double value);
    
}
