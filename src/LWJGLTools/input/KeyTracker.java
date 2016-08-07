/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

/**
 *
 * @author murdock
 */
public class KeyTracker {
    
    long myWindow;
    int myKey;
    boolean state;
    
    public KeyTracker(long window, int key) {
        myKey = key;
        myWindow = window;
        state = KeyboardReader.isPressed(myWindow,myKey);
    }
    
    public boolean isFreshlyPressed() {
        if (state) {
            updateState();
            return false;
        }
        
        if (updateState())
            return true;
        
        return false;
    }
    
    private boolean updateState() {
        return (state = KeyboardReader.isPressed(myWindow, myKey));
    }
}
