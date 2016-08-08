/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

/**
 * A class used to monitor a particular key on a particular window.
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
public class KeyTracker {
    
    long myWindow;
    int myKey;
    boolean state;
    
    /**
     * Instantiates a key monitor that is tied to a given window and key.
     * 
     * @param window    The window handle.
     * @param key       The key.
     */
    public KeyTracker(long window, int key) {
        myKey = key;
        myWindow = window;
        state = KeyboardReader.isPressed(myWindow,myKey);
    }
    
    /**
     * Returns a boolean of whether the specified key was freshly pressed (with the given window currently in focus)
     * since the last time this method was called.
     * <p>
     * In this case, a `fresh press' is considered to be the process of a key transitioning
     * from a non-depressed state to the depressed state.
     * @return      Whether the key was pressed, with the window in focus, since the last time this method was called.
     */
    public boolean isFreshlyPressed() {
        if (state) {
            updateState();
            return false;
        }
        
        return updateState();
    }
    
    private boolean updateState() {
        return (state = KeyboardReader.isPressed(myWindow, myKey));
    }
}
