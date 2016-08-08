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
 * A static class providing convenient methods to perform common actions associated
 * with keyboard input.
 * <p>
 * Note that key states will not be updated until a call to {@link org.lwjgl.glfw.GLFW#glfwPollEvents()} is made.
 * <p>
 * Wherever a `window handle' is referred to as a long, it should be one returned by 
 * {@link org.lwjgl.glfw.GLFW#glfwCreateWindow(int, int, java.nio.ByteBuffer, long, long)} 
 * or 
 * {@link org.lwjgl.glfw.GLFW#glfwCreateWindow(int, int, java.lang.CharSequence, long, long)}.
 * Wherever a `key' is referred to as an int, it should be one of the key constants found in 
 * {@link org.lwjgl.glfw.GLFW}.
 * 
 * @author Murdock Grewar
 * @see org.lwjgl.glfw.GLFW
 * @see org.lwjgl.glfw.GLFW#glfwPollEvents()
 */
public class KeyboardReader {
    
    /**
     * Blocks the thread until the specified key is pressed while focussed
     * on the specified window.
     * <p>
     * In this case, a `press' is considered to be the process of a key transitioning
     * from a non-depressed state to the depressed state.
     * <p>
     * @param window        Handle to the window which must have focus during the key press.
     * @param key           Key to await.
     */
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
    
    /**
     * Returns a boolean indicating whether (the specified key is currently depressed) && (the given window has focus).
     * @param window        Handle to the window which must have focus.
     * @param key           Key to check for depression.
     * @return              True if and only if the window has focus, and the key is currently depressed.
     */
    public static boolean isPressed(long window, int key) {
        return (glfwGetKey(window,key) == 1);
    }
}
