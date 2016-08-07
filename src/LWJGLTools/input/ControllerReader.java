/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

import java.nio.FloatBuffer;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Make sure that glfwPollEvents() is called before using the methods in here.
 * @author murdock
 */
public final class ControllerReader {
    
    //Through testing I have determined that on my XBOX 360 controller:
        //Axis 0 is the left stick x axis. Left = -1, Right  = 1
        //Axis 1 is the left stick y axis. Top  = -1, Bottom = 1
        //Axis 2 is the left trigger.      Untouched = -1, Fully pressed = 1
        //Axis 3 is the right stick x axis. Left = -1, Right  = 1
        //Axis 4 is the right stick y axis. Top = -1, Bottom = 1
        //Axis 5 is the right trigger.     Untouched = -1, Fully pressed = 1
        //Axis 6 - unused
        //Axis 7 - unused
    
    int controllerID;
    
    public ControllerReader(ControllerID cid) {
        controllerID = cid.value();
    }
    
    public enum ControllerID {
        ONE(GLFW_JOYSTICK_1),
        TWO(GLFW_JOYSTICK_2),
        THREE(GLFW_JOYSTICK_3),
        FOUR(GLFW_JOYSTICK_4);
        
        private int GLFW_ID;
        
        private ControllerID(int id) {
            this.GLFW_ID = id;
        }
        public int value() {
            return this.GLFW_ID;
        }
    }
    
    public JoystickFilteredState getJoystickState(Joystick js) throws NoControllerException, NoSuchAxisException {
        FloatBuffer fb = glfwGetJoystickAxes(controllerID);
        if (fb == null) {
            // Could not find the gamepad joystick
            System.err.println("Could not find any joystick axes of the first controller.");
            System.out.println("Aborting.");
            throw new NoControllerException();
        }
        
        float xstick, ystick, mag, angle;
        
        switch (js) {
            case LEFT:
                xstick = fb.get(0);
                ystick = fb.get(1);
                break;
            case RIGHT:
                xstick = fb.get(3);
                ystick = fb.get(4);
                break;
            default:
                throw new NoSuchAxisException();
        }
        
        mag = (float)Math.sqrt((xstick*xstick) + (ystick*ystick));
        //Need a deadzone, etc
        mag = (float)Math.min(Math.max(mag-0.3, 0),0.65)/0.65f;
        angle = (float)Math.atan2(ystick, xstick);
        
        return new JoystickFilteredState(mag,angle);
    }
    public TriggerFilteredState getTriggerState(Trigger t) throws NoSuchAxisException, NoControllerException {
        FloatBuffer fb = glfwGetJoystickAxes(controllerID);
        if (fb == null) {
            // Could not find the gamepad joystick
            System.err.println("Could not find any joystick axes of the first controller.");
            System.out.println("Aborting.");
            throw new NoControllerException();
        }
        
        float value;
        
        switch (t) {
            case LEFT:
                value = fb.get(2);
                break;
            case RIGHT:
                value = fb.get(5);
                break;
            default:
                throw new NoSuchAxisException();
        }
        value = (value*1.2f+1f)/2f;
        value = Math.max(0, value);
        value = Math.min(1, value);
        return new TriggerFilteredState(value);
    }
    
    public enum Joystick {
        LEFT,
        RIGHT;
    }
    public enum Trigger {
        LEFT,
        RIGHT;
    }
            
    // Varies from 0 to 1
    public final class TriggerFilteredState {
        private float value;
        public float getValue() {
            return value;
        }
        protected TriggerFilteredState(float v) {
            value = v;
        }
    }
    public final class JoystickFilteredState {
        private float mag;
        private float angle;
        public float getMag() {
            return mag;
        }
        public float getAngle() {
            return angle;
        }
        protected JoystickFilteredState(float m, float a) {
            mag = m;
            angle = a;
        }
    }
    
    public class NoControllerException extends Exception {
        
        public NoControllerException(String msg) {
            super(msg);
        }
        public NoControllerException() {
            super();
        }
    }
    public class NoSuchAxisException extends Exception {
        
        public NoSuchAxisException(String msg) {
            super(msg);
        }
        public NoSuchAxisException() {
            super();
        }
    }
}
