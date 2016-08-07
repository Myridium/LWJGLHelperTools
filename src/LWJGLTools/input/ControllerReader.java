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
import java.util.Map.Entry;
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
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import static org.lwjgl.glfw.GLFW.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
            
            // These are MISNOMERS! "min" refers to the down/left/not pressed position.
            min = minValue;
            max = maxValue;
        }
        protected float value(float outMin, float outMax) throws NoControllerException, NoSuchAxisException {
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
        public float getMin() {
            return min;
        }
        public float getMax() {
            return max;
        }
    }
    
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
    public TriggerFilteredState getTriggerState(Trigger t) throws NoControllerException, NotConfiguredException, NoSuchAxisException {
        
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
