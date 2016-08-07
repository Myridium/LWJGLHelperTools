/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

import static org.lwjgl.glfw.GLFW.glfwGetKey;

/**
 *
 * @author murdock
 */
public class KeyTracker {
    
    long myWindow;
    int myKey;
    int state;
    
    public KeyTracker(long window, int key) {
        myKey = key;
        myWindow = window;
        state = glfwGetKey(myWindow,myKey);
    }
    
    public boolean isFreshlyPressed() {
        if (state == 1) {
            updateState();
            return false;
        }
        
        if (updateState() == 1)
            return true;
        
        return false;
    }
    
    private int updateState() {
        return (state = glfwGetKey(myWindow,myKey));
    }
}
