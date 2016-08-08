/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LWJGLTools.GLDrawing;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBEasyFont.stb_easy_font_print;

/**
 * A static class providing more user-friendly access to LWJGL3 drawing tools.
 * <p>
 * This class makes use of OpenGL 1.1
 * 
 * @author Murdock Grewar
 */
public class GLDrawHelper {
    
    private static final double TAU = 2d * Math.PI;
    
    //Number of segments per pixel size of the major axis
    private static final float ELLIPSE_ACCURACY = 1.7f;
    
    /**
     * An enum of text alignment options.
     * 
     * @see #drawString(float, float, java.lang.String, float, LWJGLTools.GLDrawing.GLDrawHelper.TextAlignment)
     */
    public enum TextAlignment {
        LEFT_TOP,
        MIDDLE_TOP;
    }
    
    /**
     * Draws the given string at the specified position, with the specified size.
     * 
     * @param x         x coordinate of the top-left corner of the text.
     * @param y         y coordinate of the top-left corner of the text.
     * @param text      The String representing the text to be drawn.
     * @param scale     The size of the text to be drawn. Each character has a width approximately 5 times this quantity.
     */
    public static void drawString(float x, float y, String text, float scale) {
        drawString(x,y,text,scale,TextAlignment.LEFT_TOP);
    }
    
    /**
     * Draws the given string at the specified position, with the specified size, and with the specified {@link GLDrawHelper.TextAlignment} option.
     * <p>
     * The `x' and `y' coordinates specify the position of the anchor of the text,
     * where the anchor is determined by the {@link GLDrawHelper.TextAlignment} option.
     * 
     * @param x         x coordinate of the text.
     * @param y         y coordinate of the text.
     * @param text      The String representing the text to be drawn.
     * @param scale     The size of the text to be drawn. Each character has a width approximately 5 times this quantity.
     * @param alignment The text alignment option.
     */
    public static void drawString(float x, float y, String text, float scale, TextAlignment alignment) {	
        
        
        int xoffset;
        switch (alignment) {
            case LEFT_TOP:
                xoffset = 0;
                break;
            case MIDDLE_TOP:
                xoffset = Math.round(text.length()*scale*5/2);
                break;
            default:
                xoffset = 0;
        }
        
        // For some reason, 'EasyFont' will draw the text reflected in the y axis.
        
        glPushMatrix();
            glTranslatef(x-xoffset,y,0);
            FloatBuffer fb = BufferUtils.createFloatBuffer(16);
            BufferUtils.zeroBuffer(fb);
            fb.put(0,scale);
            fb.put(5,-scale);
            fb.put(10,1);
            fb.put(15,1);
            glMultMatrixf(fb);

            //270 bytes per character is the recommended amount. It is not enough.
            //ByteBuffer charBuffer = BufferUtils.createByteBuffer(text.length() * 270);
            ByteBuffer charBuffer = BufferUtils.createByteBuffer(text.length() * 540);
            int quads = stb_easy_font_print(0, 0, text, null, charBuffer);

            glEnableClientState(GL_VERTEX_ARRAY);
            glVertexPointer(2, GL_FLOAT, 16, charBuffer);
            glDrawArrays(GL_QUADS, 0, quads*4);
            glDisableClientState(GL_VERTEX_ARRAY);
        glPopMatrix();
    }
    /**
     * Sets the drawing colour by the RGB components. Assumes 100% alpha.
     * 
     * @param red       Red component, from 0 to 1.
     * @param green     Green component, from 0 to 1.
     * @param blue      Blue component, from 0 to 1.
     */
    public static void setColor(float red, float green, float blue) {
        glColor3f(red,green,blue);
    }
    
    /**
     * Sets the drawing colour by the RGBA components.
     * 
     * @param red       Red component, from 0 to 1.
     * @param green     Green component, from 0 to 1.
     * @param blue      Blue component, from 0 to 1.
     * @param alpha      Alpha value, from 0 to 1.
     */
    public static void setColor(float red, float green, float blue, float alpha) {
        glColor4f(red,green,blue,alpha);
    }
    
    /**
     * Sets the drawing colour by a {@link java.awt.Color} instance.
     * 
     * @param c     The drawing colour.
     */
    public static void setColor(Color c) {
        glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);
    }
    
    /**
     * Sets the stroke width in pixels.
     * <p>
     * Note that this stroke width does not affect drawing functions which fill regions;
     * it only affects those functions which produce line segments.
     * @param width 
     */
    public static void setStrokeWidth(float width) {
        glLineWidth(width);
    }
    
    /**
     * Draw a line segment.
     * 
     * @param startX    x position of start vertex.
     * @param startY    y position of start vertex.
     * @param endX      x position of end vertex.
     * @param endY      y position of end vertex.
     */
    public static void line(float startX, float startY, float endX, float endY) {
        glBegin(GL_LINES);
            glVertex2f(startX,startY);
            glVertex2f(endX,endY);
        glEnd();
    }
    /**
     * Draw a line segment stretched from its starting vertex by a given factor.
     * 
     * @param startX    x position of start vertex.
     * @param startY    y position of start vertex.
     * @param endX      x position of end vertex before stretching.
     * @param endY      y position of end vertex before stretching.
     * @param stretch   Stretch factor.
     */
    public static void line(float startX, float startY, float endX, float endY, float stretch) {
        float aendX = (endX - startX)*stretch + startX;
        float aendY = (endY - startY)*stretch + startY;
        line(startX,startY,aendX,aendY);
    }
    /**
     * Draw a line segment.
     * 
     * @param startX    x position of start vertex.
     * @param startY    y position of start vertex.
     * @param angle     Angle from start vertex to end vertex (in radians). 0 = rightward, pi/2 = upward.
     * @param length    Length of the line segment.
     */
    public static void lineByAngle(float startX, float startY, float angle, float length) {
        float endX = length*(float)Math.cos(angle) + startX;
        float endY = length*(float)Math.sin(angle) + startY;
        line(startX,startY,endX,endY);
    }
    
    /**
     * Draw a filled disk.
     * 
     * @param x         Origin x coordinate.
     * @param y         Origin y coordinate.
     * @param radius    Disk radius.
     */
    public static void disk(float x, float y, float radius) {
        diskSector(x,y,radius,0,(float)TAU);
    }
    /**
     * Draw a sector of a filled disk.
     * <p>
     * {@code sectorStartAngle} specifies the starting angle of the sector.
     * 0 corresponds to the positive x axis; pi/2 corresponds to the positive y axis.
     * <p>
     * {@code sectorAngle} specifies the angle of the sector. The sector is extended
     * from its starting angle, by this amount, in a counter-clockwise direction.
     * 
     * @param x                 Origin x coordinate.
     * @param y                 Origin y coordinate.
     * @param radius            Disk radius
     * @param sectorStartAngle  Starting angle (in radians).
     * @param sectorAngle       Subtended angle (in radians).
     */
    public static void diskSector(float x, float y, float radius, float sectorStartAngle, float sectorAngle) {
        ellipseFillSector(x,y,radius,radius,0,sectorStartAngle,sectorAngle);
    }
    /**
     * Draw a circle (a hollow disk).
     * 
     * @param x         Origin x coordinate.
     * @param y         Origin y coordinate.
     * @param radius    Disk radius.
     * @see GLDrawHelper#setStrokeWidth(float)
     */
    public static void circle(float x, float y, float radius) {
        ellipse(x,y,radius,radius,0);
    }
    
    /**
     * Draw a sector of a circle (hollow disk).
     * <p>
     * {@code sectorStartAngle} specifies the starting angle of the sector.
     * 0 corresponds to the positive x axis; pi/2 corresponds to the positive y axis.
     * <p>
     * {@code sectorAngle} specifies the angle of the sector. The sector is extended
     * from its starting angle, by this amount, in a counter-clockwise direction.
     * 
     * @param x                 Origin x coordinate.
     * @param y                 Origin y coordinate.
     * @param radius            Disk radius
     * @param sectorStartAngle  Starting angle (in radians).
     * @param sectorAngle       Subtended angle (in radians).
     */
    public static void circleSector(float x, float y, float radius, float sectorStartAngle, float sectorAngle) {
        ellipseSector(x,y,radius,radius,0,sectorStartAngle,sectorAngle);
    }
    
    /**
     * Draw a filled ellipse.
     * 
     * @param x         Origin x coordinate.
     * @param y         Origin y coordinate.
     * @param mrad      Minor radius.
     * @param Mrad      Major radius
     * @param angle     Angle of rotation of the ellispe. This is the angle of the line drawn along the major radius starting from the origin of the ellipse.
     */
    public static void ellipseFill(float x, float y, float mrad, float Mrad, float angle) {
        ellipseFillSector(x,y,mrad,Mrad,angle,0,(float)TAU);
    }
    
    /**
     * Draw a sector of a filled ellipse.
     * <p>
     * {@code sectorStartAngle} specifies the starting angle of the sector
     * relative to the rotation of the ellipse (specified by {@code angle}).
     * <p>
     * {@code sectorAngle} specifies the angle of the sector. The sector is extended
     * from its starting angle, by this amount, in a counter-clockwise direction.
     * 
     * @param x                     Origin x coordinate.
     * @param y                     Origin y coordinate.
     * @param mrad                  Minor radius.
     * @param Mrad                  Major radius
     * @param angle                 Angle of rotation of the ellispe. This is the angle of the line drawn along the major radius starting from the origin of the ellipse.
     * @param sectorStartAngle      Starting angle of sector (relative to ellispe rotation).
     * @param sectorAngle           Subtended angle of sector.
     */
    public static void ellipseFillSector(float x, float y, float mrad, float Mrad, float angle, float sectorStartAngle, float sectorAngle) {
       
        int sliceCount = (int)Math.ceil(ELLIPSE_ACCURACY*Mrad*Math.abs(sectorAngle)/TAU);
        float cache,relX,relY;

        glBegin(GL_TRIANGLE_FAN);
            glVertex2f(x, y); // center of circle
            for(int i = 0; i <= sliceCount ; i++) { 
                cache = (float)(Mrad*Math.cos(i * sectorAngle / sliceCount + sectorStartAngle));
                relY = (float)(mrad*Math.sin(i * sectorAngle / sliceCount + sectorStartAngle));
                relX = (float)((Math.cos(-angle)*cache) + (Math.sin(-angle)*relY));
                relY = (float)((-Math.sin(-angle)*cache) + (Math.cos(-angle)*relY));
                
                glVertex2f(
                    x + relX,
                    y + relY
                );
            }
        glEnd();
    }
    public static void ellipse(float x, float y, float mrad, float Mrad, float angle) {
       
        int sliceCount = (int)Math.ceil(ELLIPSE_ACCURACY*Mrad);
        float cache,relX,relY;

        glBegin(GL_LINE_LOOP);
            for(int i = 0; i <= sliceCount ; i++) { 
                cache = (float)(Mrad*Math.cos(i * TAU / sliceCount));
                relY = (float)(mrad*Math.sin(i * TAU / sliceCount));
                relX = (float)((Math.cos(-angle)*cache) + (Math.sin(-angle)*relY));
                relY = (float)((-Math.sin(-angle)*cache) + (Math.cos(-angle)*relY));
                
                glVertex2f(
                    x + relX,
                    y + relY
                );
            }
        glEnd();
    }
    public static void ellipseSector(float x, float y, float mrad, float Mrad, float angle, float sectorStartAngle, float sectorAngle) {
        int sliceCount = (int)Math.ceil(ELLIPSE_ACCURACY*Mrad*Math.abs(sectorAngle)/TAU);
        float cache,relX,relY;

        glBegin(GL_LINE_STRIP);
            for(int i = 0; i <= sliceCount ; i++) { 
                cache = (float)(Mrad*Math.cos(i * sectorAngle / sliceCount + sectorStartAngle));
                relY = (float)(mrad*Math.sin(i * sectorAngle / sliceCount + sectorStartAngle));
                relX = (float)((Math.cos(-angle)*cache) + (Math.sin(-angle)*relY));
                relY = (float)((-Math.sin(-angle)*cache) + (Math.cos(-angle)*relY));
                
                glVertex2f(
                    x + relX,
                    y + relY
                );
            }
        glEnd();
    }
    public static void urchin(float x, float y, float sRad, float bRad, int spines, float angle) {
        
        int sliceCount = spines*2;
        float cache,relX,relY;

        glBegin(GL_LINE_LOOP);
            for(int i = 0; i <= sliceCount ; i++) {
                float rad = ((bRad - sRad)*(i % 2)) + sRad;
                cache = rad *(float)Math.cos(i * TAU / sliceCount);
                relY =  rad *(float)Math.sin(i * TAU / sliceCount);
                relX =  (float)((Math.cos(-angle)*cache) + (Math.sin(-angle)*relY));
                relY =  (float)((-Math.sin(-angle)*cache) + (Math.cos(-angle)*relY));
                
                glVertex2f(
                    x + relX,
                    y + relY
                );
            }
        glEnd();
    }
    
}
