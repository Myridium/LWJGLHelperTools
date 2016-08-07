/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

import java.nio.FloatBuffer;
import java.util.HashMap;
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
    
    HashMap<Joystick, Axis[]> JoystickAxes;
    HashMap<Joystick, Float> JoystickDeadzones;
    HashMap<Trigger, Axis> TriggerAxes;
    
    public ControllerReader() {
        JoystickAxes = new HashMap<Joystick, Axis[]>();
        JoystickDeadzones = new HashMap<Joystick, Float>();
        TriggerAxes = new HashMap<Trigger, Axis>();
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
    
    public enum AxisID {
        ZERO(0),
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5);
        
        private int axisID;
        
        private AxisID(int id) {
            this.axisID = id;
        }
        
        public int value() {
            return this.axisID;
        }
        
    }
    
    public static class Axis {
        private AxisID id;
        private ControllerID controller;
        private float min, max;
        public Axis(ControllerID myControllerID, AxisID myAxisID, float minValue, float maxValue) {
            id = myAxisID;
            controller = myControllerID;
            min = minValue;
            max = maxValue;
        }
        protected float value(float outMin, float outMax) throws NoControllerException {
            float scale = (outMax - outMin) / (max - min);
            float rawVal;
            rawVal = rawAxisValue(controller, this.id);
            rawVal = Math.max(min, rawVal);
            rawVal = Math.min(max, rawVal);
            return (rawVal - min)*scale + outMin;
        }
    }
    
    private static float rawAxisValue(ControllerID cont, AxisID axis) throws NoControllerException {
        FloatBuffer fb = glfwGetJoystickAxes(cont.value());
        if (fb == null) {
            System.err.println("Could not find any joystick axes of the first controller.");
            System.out.println("Aborting.");
            throw new NoControllerException();
        }
        return fb.get(axis.value());
    }
    
    public void setJoystickAxes(Joystick js, Axis xAxis, Axis yAxis) {
        Axis[] leftRightAxes = new Axis[]{xAxis, yAxis};
        JoystickAxes.put(js, leftRightAxes);
    }
    public void setTriggerAxis(Trigger trig, Axis axis) {
        TriggerAxes.put(trig, axis);
    }
    public void setJoystickDeadzone(Joystick js, float deadRadius) {
        JoystickDeadzones.put(js, deadRadius);
    }
    
    public JoystickFilteredState getJoystickState(Joystick js) throws NotConfiguredException, NoControllerException {
        
        float xstick, ystick, mag, angle;
        
        Axis xAxis, yAxis;
        try {
            xAxis = JoystickAxes.get(js)[0];
            yAxis = JoystickAxes.get(js)[1];
        } catch (NullPointerException e) {
            throw new NotConfiguredException("Requested joystick is not properly configured.");
        }
        
        xstick = xAxis.value(-1, 1);
        ystick = yAxis.value(-1, 1);
        
        mag = (float)Math.sqrt((xstick*xstick) + (ystick*ystick));
        // Deadzone
        float deadRad, dilate;
        try {
            deadRad = JoystickDeadzones.get(js);
        } catch (NullPointerException e) {
            throw new NotConfiguredException("No deadzone set for given joystick.");
        }
        dilate = 1 / (1 - deadRad);
        mag = (float)Math.max(mag-deadRad, 0)*dilate;
        // The actual range of the joystick is a SQUARE whose circumscribed circle has a radius of root 2.
        // To deal with this, I'll simply cap "mag" at 1.
        mag = (float)Math.min(mag, 1);
        angle = (float)Math.atan2(ystick, xstick);
        
        return new JoystickFilteredState(mag,angle);
    }
    public TriggerFilteredState getTriggerState(Trigger t) throws NoControllerException, NotConfiguredException {
        
        Axis trigAxis;
        
        try {
            trigAxis = TriggerAxes.get(t);
        } catch (NullPointerException e) {
            throw new NotConfiguredException("Requested joystick is not properly configured.");
        }
        
        return new TriggerFilteredState(trigAxis.value(0, 1));
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
    
    public static class NoControllerException extends Exception {
        
        public NoControllerException(String msg) {
            super(msg);
        }
        public NoControllerException() {
            super();
        }
    }
    public static class NotConfiguredException extends Exception {
        
        public NotConfiguredException(String msg) {
            super(msg);
        }
        public NotConfiguredException() {
            super();
        }
    }
    public static class NoSuchAxisException extends Exception {
        
        public NoSuchAxisException(String msg) {
            super(msg);
        }
        public NoSuchAxisException() {
            super();
        }
    }
}
