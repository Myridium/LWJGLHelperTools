/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

/**
 *
 * @author murdock
 */
public class KeyboardReader {
    
    
    public static void awaitKeyPress(long window, int key) {
        int keyState = glfwGetKey(window, key);
        while ((keyState = glfwGetKey(window,key)) != 0) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(KeyboardReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        while ((keyState = glfwGetKey(window,key)) != 1) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(KeyboardReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    public static boolean isPressed(long window, int key) {
        return (glfwGetKey(window,key) == 1);
    }
}
