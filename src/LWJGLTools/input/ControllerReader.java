/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.input;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import static org.lwjgl.glfw.GLFW.*;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A wrapper class for handling gamepad input.
 * <p>
 * Make sure that {@link org.lwjgl.glfw.GLFW#glfwPollEvents()} is called before reading inputs.
 * This is necessary to update the states of the controllers.
 * <p>
 * After instantiating an instance of this class, the user should configure
 * the axes they intend to use by creating {@link ControllerReader.Axis} objects
 * and assigning them to the appropriate physical controls represented by the
 * {@link ControllerReader.Joystick} and {@link ControllerReader.Trigger} enums.
 * This is accomplished by calling the `setXXXAxes(...)' methods of this class.
 * <p>
 * An attempt to read a control which is not yet fully configured (i.e. attempting
 * to read from a {@link ControllerReader.Joystick} which has not yet been assigned a deadzone)
 * will result in a {@link ControllerReader.NotConfiguredException} to be thrown.
 * 
 * @see ControllerReader.Axis
 * @see ControllerReader.Joystick
 * @see ControllerReader.Trigger
 * @see ControllerReader#setJoystickAxes(LWJGLTools.input.ControllerReader.Joystick, LWJGLTools.input.ControllerReader.Axis, LWJGLTools.input.ControllerReader.Axis) 
 * @see ControllerReader#setJoystickDeadzone(LWJGLTools.input.ControllerReader.Joystick, float) 
 * @see ControllerReader#setTriggerAxis(LWJGLTools.input.ControllerReader.Trigger, LWJGLTools.input.ControllerReader.Axis) 
 */
public final class ControllerReader {
    
    //Through testing I have determined that on my XBOX 360 controller on Linux Mint:
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
    
    /**
     * Returns a new ControllerReader object with a blank configuration.
     * 
     * @see         ControllerReader
     */
    public ControllerReader() {
        JoystickAxes = new HashMap<Joystick, Axis[]>();
        JoystickDeadzones = new HashMap<Joystick, Float>();
        TriggerAxes = new HashMap<Trigger, Axis>();
    }
    
    /**
     * An enum listing some of the controllers made available by the LWJGL3 library.
     * <p>
     * If no controllers are unplugged, then new controllers will fill the slots
     * in ascending order. However, if a controller is unplugged, its place is reserved
     * so that if plugged back in again, it will have the same index.
     * <p>
     * In the event that a controller is requested by its ControllerID but no
     * such controller could be found, a NoControllerException will be thrown.
     * 
     * @see         org.lwjgl.glfw.GLFW#GLFW_JOYSTICK_1
     * @see         NoControllerException
     */
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
    
    /**
     * An enum listing axes by their index. 
     * <p>
     * For each ControllerID, there will be
     * an associated set of axes. There is no way to tell ahead of time how
     * many axes will be available on a controller, so in the case that an axis
     * is requested where it cannot be found, a NoSuchAxisException will be thrown.
     * 
     * @see         ControllerID
     * @see         NoSuchAxisException
     */
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
    
    /**
     * A wrapper class for requesting axis data that would otherwise have to be read raw.
     * Each Axis object is associated with a ControllerID and AxisID on that
     * controller. 
     * 
     * @see         ControllerID
     * @see         AxisID
     * @see         Axis#value(float, float)
     */
    public static class Axis {
        private AxisID id;
        private ControllerID controller;
        private float min, max;
        
        /**
         * Returns a new Axis object with the specified configuration.
         * <p>
         * Note that in general, minValue may be larger than maxValue.
         * This will reverse the orientation of the axis, returning a different
         * value when {@link #value(float, float)} is called.
         * 
         * @param cid       The ID of the controller which this axis reads from.
         * @param aid       The ID of the axis from which this Axis reads.
         * @param minValue  The minimum raw value of the axis as read by {@link org.lwjgl.glfw.GLFW#glfwGetJoystickAxes(int)}.
         * @param maxValue  The maximum raw value of the axis as read by {@link org.lwjgl.glfw.GLFW#glfwGetJoystickAxes(int)}.
         */
        public Axis(ControllerID cid, AxisID aid, float minValue, float maxValue) {
            id = aid;
            controller = cid;
            
            // These are MISNOMERS! "min" refers to the down/left/not pressed position.
            min = minValue;
            max = maxValue;
        }
        
        /**
         * Returns a float specifying the current position of the axis mapped
         * into the range [outMin, outMax].
         * <p>
         * Note that in general, outMin may be larger than outMax. If so, the
         * axis value will be read in the reversed orientation.
         * <p>
         * A {@link ControllerReader.NoControllerException} will be thrown when the controller associated
         * with the axis can not be found. A {@link ControllerReader.NoSuchAxisException} will be thrown
         * when the associated axis, specified by its {@link AxisID}, cannot be
         * found on the associated controller.
         * 
         * @param outMin The output value to be returned if the raw value of the
         * axis is `min'.
         * @param outMax The output value to be returned if the raw value of the
         * axis is `max'.
         * @return The current value of the axis mapped onto the range [outMin, outMax].
         * @throws LWJGLTools.input.ControllerReader.NoControllerException
         * @throws LWJGLTools.input.ControllerReader.NoSuchAxisException 
         * @see Axis
         * @see ControllerReader.Axis#ControllerReader.Axis(LWJGLTools.input.ControllerReader.ControllerID cid, LWJGLTools.input.ControllerReader.AxisID aid, float minValue, float maxValue)
         */
        public float value(float outMin, float outMax) throws NoControllerException, NoSuchAxisException {
            float scale = (outMax - outMin) / (max - min);
            float rawVal;
            rawVal = rawAxisValue(controller, this.id);
            
            // Again, max is not actually the max.
            if (max > min) {
                rawVal = Math.max(min, rawVal);
                rawVal = Math.min(max, rawVal);
            } else {
                rawVal = Math.max(max, rawVal);
                rawVal = Math.min(min, rawVal);
            }
            return (rawVal - min)*scale + outMin;
        }
        protected Element getXMLNode(Document doc) {
            Element el =                doc.createElement("axis");
            
            Element elMin =             doc.createElement("min");
            Element elMax =             doc.createElement("max");
            Element elControllerID =    doc.createElement("controllerID");
            Element elAxisID =          doc.createElement("axisID");
            
            elMin.setTextContent(String.valueOf(min));
            elMax.setTextContent(String.valueOf(max));
            elControllerID.setTextContent(controller.name());
            elAxisID.setTextContent(id.name());
            
            el.appendChild(elMin);
            el.appendChild(elMax);
            el.appendChild(elControllerID);
            el.appendChild(elAxisID);
            
            return el;
        }
        protected static Axis parseXMLNode(Element axisEl) {
            
            ControllerID cid = ControllerID.valueOf(axisEl.getElementsByTagName("controllerID").item(0).getTextContent());
            AxisID aid = AxisID.valueOf(axisEl.getElementsByTagName("axisID").item(0).getTextContent());
            float min = Float.valueOf(axisEl.getElementsByTagName("min").item(0).getTextContent());
            float max = Float.valueOf(axisEl.getElementsByTagName("max").item(0).getTextContent());

            Axis axis = new Axis(cid, aid, min, max);
            return axis;
            
        }
        public AxisID getAxisID() {
            return id;
        }
        public ControllerID getControllerID() {
            return controller;
        }
        public float getRawMin() {
            return min;
        }
        public float getRawMax() {
            return max;
        }
    }
    
    /**
     * Will read a raw axis value without the need for instantiating an instance
     * of {@link ControllerReader} or {@link ControllerReader.Axis}.
     * <p>
     * Internally, this method uses {@link org.lwjgl.glfw.GLFW#glfwGetJoystickAxes(int)}
     * to read all raw joystick axis values, then extracts the desired one.
     * <p>
     * A {@link ControllerReader.NoControllerException} will be thrown if the specified
     * controller could not be found. A {@link ControllerReader.NoSuchAxisException}
     * will be thrown if the controller could be found, but not the specified axis on the controller.
     * @param cont      The controller to read from.
     * @param axis      The axis to read from the given controller.
     * @return          The raw axis value as yielded by {@link org.lwjgl.glfw.GLFW#glfwGetJoystickAxes(int)}.
     * @throws LWJGLTools.input.ControllerReader.NoControllerException
     * @throws LWJGLTools.input.ControllerReader.NoSuchAxisException 
     */
    public static float rawAxisValue(ControllerID cont, AxisID axis) throws NoControllerException, NoSuchAxisException {
        FloatBuffer fb = glfwGetJoystickAxes(cont.value());
        if (fb == null) {
            System.err.println("Could not find any joystick axes of the first controller.");
            System.out.println("Aborting.");
            throw new NoControllerException();
        }
        try {
            return fb.get(axis.value());
        } catch (NullPointerException e) {
            throw new NoSuchAxisException("The specified axis could not be found on the specified controller.");
        }
    }
    
    /**
     * Associates a physical joystick, specified by a {@link Joystick}, with the
     * given axes.
     * 
     * @param js        The physical joystick to associate with the axes.
     * @param xAxis     The `x' axis of the joystick.
     * @param yAxis     The `y' axis of the joystick.
     * @see ControllerReader.Axis
     * @see ControllerReader.Joystick
     */
    public void setJoystickAxes(Joystick js, Axis xAxis, Axis yAxis) {
        Axis[] leftRightAxes = new Axis[]{xAxis, yAxis};
        JoystickAxes.put(js, leftRightAxes);
    }
    
    /**
     * Associates a physical trigger button, specified by a {@link Trigger}, with the
     * given axis.
     * 
     * @param trig      The physical trigger button to associate with the axis.
     * @param axis      The axis of the trigger.
     * @see ControllerReader.Axis
     * @see ControllerReader.Trigger
     */
    public void setTriggerAxis(Trigger trig, Axis axis) {
        TriggerAxes.put(trig, axis);
    }
    
    /**
     * Sets the deadzone of a physical joystick.
     * 
     * @param js            The physical joystick.
     * @param deadRadius    The deadzone radius (relative to the range determined by {@link ControllerReader.JoystickFilteredState}.
     * @see ControllerReader.Joystick
     * @see ControllerReader.JoystickFilteredState
     */
    public void setJoystickDeadzone(Joystick js, float deadRadius) {
        JoystickDeadzones.put(js, deadRadius);
    }
    
    /**
     * Measures the state of the given {@link ControllerReader.Joystick} since
     * the last call to {@link org.lwjgl.glfw.GLFW#glfwGetJoystickAxes(int)}.
     * <p>
     * The exceptions which can be thrown are self-explanatory. 
     * This method takes into account the configured joystick deadzone,
     * and the range of its axes. It assumes that the joystick is circular.
     * <p>
     * See {@link ControllerReader.JoystickFilteredState} for more information
     * about the returned object.
     * 
     * @param js        The physical joystick to measure.
     * @return          The filtered state of the joystick.
     * @throws LWJGLTools.input.ControllerReader.NotConfiguredException
     * @throws LWJGLTools.input.ControllerReader.NoControllerException
     * @throws LWJGLTools.input.ControllerReader.NoSuchAxisException 
     * @see ControllerReader.JoystickFilteredState
     * @see ControllerReader.NotConfiguredException
     * @see ControllerReader.NoControllerException
     * @see ControllerReader.NoSuchAxisException
     */
    public JoystickFilteredState getJoystickState(Joystick js) throws NotConfiguredException, NoControllerException, NoSuchAxisException {
        
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
    
    /**
     * Measures the state of the given {@link ControllerReader.Trigger} since
     * the last call to {@link org.lwjgl.glfw.GLFW#glfwGetJoystickAxes(int)}.
     * <p>
     * The exceptions which can be thrown are self-explanatory. 
     * See {@link ControllerReader.TriggerFilteredState} for more information
     * about the returned object.
     * 
     * @param t         The physical trigger button to measure.
     * @return          The filtered state of the trigger button.
     * @throws LWJGLTools.input.ControllerReader.NotConfiguredException
     * @throws LWJGLTools.input.ControllerReader.NoControllerException
     * @throws LWJGLTools.input.ControllerReader.NoSuchAxisException 
     * @see ControllerReader.TriggerFilteredState
     * @see ControllerReader.NotConfiguredException
     * @see ControllerReader.NoControllerException
     * @see ControllerReader.NoSuchAxisException
     */
    public TriggerFilteredState getTriggerState(Trigger t) throws NoControllerException, NotConfiguredException, NoSuchAxisException {
        
        Axis trigAxis;
        
        try {
            trigAxis = TriggerAxes.get(t);
        } catch (NullPointerException e) {
            throw new NotConfiguredException("Requested joystick is not properly configured.");
        }
        
        return new TriggerFilteredState(trigAxis.value(0, 1));
    }
    
    /**
     * An enum of the physical joysticks present on a gamepad.
     */
    public enum Joystick {
        LEFT,
        RIGHT;
    }
    
    /**
     * An enum of the physical trigger buttons present on a gamepad.
     */
    public enum Trigger {
        LEFT,
        RIGHT;
    }
    
    /**
     * Writes the existing configuration of this {@link ControllerReader} instance
     * to an XML file.
     * 
     * @param dir   The full target file directory. Should use `/' as a path separator.
     * @return      Whether the file was successfully written.
     * @see ControllerReader#readConfig(java.lang.String)
     */
    public boolean writeConfig(String dir) {
        try {

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// Create the root element of the document.
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("Configuration");
		doc.appendChild(rootElement);

		// Adding Joystick child elements.
                Element joyEl;
                Element xAxisEl, yAxisEl, deadzoneEl;
                for (Joystick js : Joystick.values()) {
                    joyEl = doc.createElement("joystick");
                    joyEl.setAttribute("which", js.name());
                    
                    // Adding the axes
                    if (JoystickAxes.containsKey(js)) {
                        
                        Axis xAxis, yAxis;
                        xAxis = JoystickAxes.get(js)[0];
                        yAxis = JoystickAxes.get(js)[1];
                        
                        xAxisEl = xAxis.getXMLNode(doc);
                        xAxisEl.setAttribute("which", "x");
                        yAxisEl = yAxis.getXMLNode(doc);
                        yAxisEl.setAttribute("which", "y");
                        
                        joyEl.appendChild(xAxisEl);
                        joyEl.appendChild(yAxisEl);
                        
                    }
                    
                    // Adding the deadzone
                    if (JoystickDeadzones.containsKey(js)) {
                        float val = JoystickDeadzones.get(js);
                        deadzoneEl = doc.createElement("deadzone");
                        deadzoneEl.setTextContent(String.valueOf(val));
                        
                        joyEl.appendChild(deadzoneEl);
                    }
                    
                    rootElement.appendChild(joyEl);
                    
                }
                
                Element trigEl, axisEl;
                
                for (Trigger trig : Trigger.values()) {
                    
                    trigEl = doc.createElement("trigger");
                    trigEl.setAttribute("which", trig.name());
                        
                    if (TriggerAxes.containsKey(trig)) {
                        axisEl = TriggerAxes.get(trig).getXMLNode(doc);
                        trigEl.appendChild(axisEl);
                    }
                    
                    rootElement.appendChild(trigEl);
                }
                
                
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
                
                // Adds some sensible formatting to the document
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(dir));

		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);

		transformer.transform(source, result);

		System.out.println("Configuration XML saved.");
                
                return true;

	  } catch (ParserConfigurationException pce) {
		pce.printStackTrace();
	  } catch (TransformerException tfe) {
		tfe.printStackTrace();
	  }
            
        return false;
    }
    
    /**
     * Reads and applies the configuration stored in an XML file produced by {@link ControllerReader#writeConfig(java.lang.String)}.
     * <p>
     * This will only apply the configuration parameters that are stored in the XML file.
     * Other parameters will not be modified.
     * 
     * @param dir   The full target file directory. Should use `/' as a path separator.
     * @return      Whether the file was successfully read, and the configuration applied.
     * @see ControllerReader#writeConfig(java.lang.String)
     */
    public boolean readConfig(String dir) {
        try {
            File xmlFile = new File(dir);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            
            //Not necessary for my particular XML files:
            //doc.getDocumentElement().normalize();
            //See
            //http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            
            Element rootNode = doc.getDocumentElement();
            
            // Fetching Joystick configuration
            NodeList joystickNodes = rootNode.getElementsByTagName("joystick");
            for (int i = 0; i < joystickNodes.getLength(); i++) {
                Element jsNode = (Element)joystickNodes.item(i);
                
                Joystick which = Joystick.valueOf(jsNode.getAttribute("which"));
                Axis xAxis = null, yAxis = null;
                float deadzone = Float.valueOf(jsNode.getElementsByTagName("deadzone").item(0).getTextContent());
                
                
                NodeList axesEl = jsNode.getElementsByTagName("axis");
                for (int j=0; j < axesEl.getLength(); j++) {
                    Element axisEl = (Element) axesEl.item(j);
                    
                    Axis axis = Axis.parseXMLNode(axisEl);
                    
                    switch (axisEl.getAttribute("which")) {
                        
                        case "x":
                            xAxis = axis;
                            break;
                        case "y":
                            yAxis = axis;
                            break;
                        default:
                            break;
                    }
                    
                }
                
                this.setJoystickAxes(which, xAxis, yAxis);
                this.setJoystickDeadzone(which, deadzone);
                
            }
            //////////////////////////////////////
            
            // Fetching trigger configuration:
            NodeList triggerNodes = rootNode.getElementsByTagName("trigger");
            for (int i=0; i < triggerNodes.getLength(); i++) {
                Element trigEl = (Element)triggerNodes.item(i);
                
                Element axisEl = (Element)trigEl.getElementsByTagName("axis").item(0);
                Axis axis = Axis.parseXMLNode(axisEl);
                
                Trigger trig = Trigger.valueOf(trigEl.getAttribute("which"));
                
                this.setTriggerAxis(trig, axis);
                
                
            }
            
            return true;
            
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ControllerReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(ControllerReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ControllerReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
        
    }        
    
    /**
     * An object encapsulating an instantaneous state of a physical trigger.
     * 
     * @see ControllerReader.TriggerFilteredState#getValue()
     */
    public final class TriggerFilteredState {
        private float value;
        /**
         * Returns the current value of the trigger.
         * <p>
         * 0 corresponds to the trigger being untouched,
         * and 1 to the trigger being fully depressed.
         * 
         * @return  The value of the trigger.
         */
        public float getValue() {
            return value;
        }
        
        // Should only ever be used with a value from 0 to 1!
        protected TriggerFilteredState(float v) {
            value = v;
        }
    }
    
    /**
     * An object encapsulating an instantaneous state of a physical joystick.
     * <p>
     * The information about the joystick state is stored in the form of a 
     * magnitude of displacement from the origin, and an angle.
     * 
     * @see ControllerReader.JoystickFilteredState#getMag()
     * @see ControllerReader.JoystickFilteredState#getAngle()
     */
    public final class JoystickFilteredState {
        private static final float TAU = (float)Math.PI*2;
        private float mag;
        private float angle;
        /**
         * Returns the magnitude of the joystick displacement from its resting position.
         * 0 corresponds to resting position, and 1 to fully pushed in any direction.
         * <p>
         * Note that in the production of an instance of {@link ControllerReader.JoystickFilteredState},
         * the deadzone of the joystick will have been taken into account.
         * 
         * @return The magnitude of the joystick displacement from its resting position.
         */
        public float getMag() {
            return mag;
        }
        
        /**
         * Returns the angle of the joystick position. 0 corresponds to the positive x axis,
         * and pi/2 to the positive y axis.
         * <p>
         * Note that in the production of an instance of {@link ControllerReader.JoystickFilteredState},
         * it will have been assumed that the joystick is (physically) circular. 
         * Hence this angle will not be correct for an elliptic joystick.
         * 
         * @return The angle of the joystick displacement in the range [0, 2*pi). 
         * 0 = positive x axis. 
         * pi/2 = positive y axis.
         */
        public float getAngle() {
            float returnAngle = angle % TAU;
            if (returnAngle < 0)
                returnAngle += TAU;
            return returnAngle;
        }
        // Should only ever be instantiated with a magnitude from 0 to 1!!
        protected JoystickFilteredState(float m, float a) {
            mag = m;
            angle = a;
        }
    }
    
    /**
     * An Exception associated with attempting to access a controller where none can be found.
     */
    public static class NoControllerException extends Exception {
        
        public NoControllerException(String msg) {
            super(msg);
        }
        public NoControllerException() {
            super();
        }
    }
    /**
     * An Exception associated with attempting to access the state of a physical
     * control that has not yet been assigned the necessary configuration.
     * <p>
     * If you encounter this Exception, then you probably have not sufficiently
     * configured the physical control whose state you are trying to access.
     * A {@link ControllerReader.Joystick} must be assigned both axes, and a deadzone.
     * A {@link ControllerReader.Trigger} must be assigned an axis.
     * 
     * @see ControllerReader#setJoystickAxes(LWJGLTools.input.ControllerReader.Joystick, LWJGLTools.input.ControllerReader.Axis, LWJGLTools.input.ControllerReader.Axis)
     * @see ControllerReader#setJoystickDeadzone(LWJGLTools.input.ControllerReader.Joystick, float)
     * @see ControllerReader#setTriggerAxis(LWJGLTools.input.ControllerReader.Trigger, LWJGLTools.input.ControllerReader.Axis)
     */
    public static class NotConfiguredException extends Exception {
        
        public NotConfiguredException(String msg) {
            super(msg);
        }
        public NotConfiguredException() {
            super();
        }
    }
    
    /**
     * An Exception associated with trying to read the state of an axis
     * which cannot be found on the specified controller.
     */
    public static class NoSuchAxisException extends Exception {
        
        public NoSuchAxisException(String msg) {
            super(msg);
        }
        public NoSuchAxisException() {
            super();
        }
    }
}
