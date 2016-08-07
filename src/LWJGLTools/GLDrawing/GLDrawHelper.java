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
 *
 * @author murdock
 */
public class GLDrawHelper {
    
    private static final double TAU = 2d * Math.PI;
    
    //Number of segments per pixel size of the major axis
    private static final float ELLIPSE_ACCURACY = 1.7f;
    
    
    public enum TextAlignment {
        LEFT_TOP,
        MIDDLE_TOP;
    }
    
    public static void drawString(float x, float y, String text, float scale) {
        drawString(x,y,text,scale,TextAlignment.LEFT_TOP);
    }
    //Very inefficient!
    //x,y specifies the top-right corner of the text.
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
    public static void setColor(float red, float green, float blue) {
        glColor3f(red,green,blue);
    }
    public static void setColor(float red, float green, float blue, float alpha) {
        glColor4f(red,green,blue,alpha);
    }
    public static void setColor(Color c) {
        glColor4f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f);
    }
    public static void setStrokeWidth(float width) {
        glLineWidth(width);
    }
    
    public static void line(float startX, float startY, float endX, float endY) {
        glBegin(GL_LINES);
            glVertex2f(startX,startY);
            glVertex2f(endX,endY);
        glEnd();
    }
    public static void line(float startX, float startY, float endX, float endY, float stretch) {
        float aendX = (endX - startX)*stretch + startX;
        float aendY = (endY - startY)*stretch + startY;
        line(startX,startY,aendX,aendY);
    }
    public static void lineByAngle(float startX, float startY, float angle, float length) {
        float endX = length*(float)Math.cos(angle) + startX;
        float endY = length*(float)Math.sin(angle) + startY;
        line(startX,startY,endX,endY);
    }
    public static void disk(float x, float y, float radius) {
        diskSector(x,y,radius,0,(float)TAU);
    }
    public static void diskSector(float x, float y, float radius, float sectorStartAngle, float sectorAngle) {
        ellipseFillSector(x,y,radius,radius,0,sectorStartAngle,sectorAngle);
    }
    public static void circle(float x, float y, float radius) {
        ellipse(x,y,radius,radius,0);
    }
    public static void circleSector(float x, float y, float radius, float sectorStartAngle, float sectorAngle) {
        ellipseSector(x,y,radius,radius,0,sectorStartAngle,sectorAngle);
    }
    public static void ellipseFill(float x, float y, float mrad, float Mrad, float angle) {
        ellipseFillSector(x,y,mrad,Mrad,angle,0,(float)TAU);
    }
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
