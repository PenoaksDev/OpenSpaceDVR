/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.registry;

import com.chiorichan.dvr.DVRLoader;
import com.chiorichan.dvr.MP4Writer;
import com.github.sarxos.webcam.Webcam;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;

public class VideoInput
{
    protected Webcam device;
    protected BufferedImage img = null;
    protected long lastTimeStamp = -1;
    protected int currentFPS = 0;
    protected String title = "Camera 0";
    protected MP4Writer writer;
    
    public VideoInput( Webcam w )
    {
        device = w;
        
        writer = new MP4Writer( this );
    }
    
    public String getChannelName()
    {
        return device.getName().replace( "/dev/", "" );
    }
    
    public Webcam getDevice()
    {
        return device;
    }
    
    public boolean open()
    {
        return device.open();
    }
    
    public boolean close()
    {
        return device.close();
    }
    
    public void capture()
    {
        DVRLoader.getExecutor().execute( new ThreadedExecution( this ) );
    }
    
    public void setTitle( String text )
    {
        title = text;
    }
    
    public BufferedImage getLastImage()
    {
        return img;
    }
    
    public int getHeight()
    {
        return 1080;
    }
    
    public int getWidth()
    {
        return 1920;
    }
    
    class ThreadedExecution implements Runnable
    {
        boolean busy = false;
        VideoInput vi;
        
        public ThreadedExecution( VideoInput _vi )
        {
            vi = _vi;
        }
        
        @Override
        public void run()
        {
            if ( busy )
            {
                return;
            }
            
            busy = true;
            
            img = device.getImage();
            
            if ( img != null )
            {
                currentFPS = Math.round( 1000 / ((float) (System.currentTimeMillis() - lastTimeStamp)) );
                
                lastTimeStamp = System.currentTimeMillis();
                
                Graphics2D gd = img.createGraphics();
                // VideoUtils.adjustGraphics( gd );
                Font font = new Font( "Sans", Font.BOLD, 26 );
                String text = getChannelName() + " - " + currentFPS + " FPS";
                TextLayout textLayout = new TextLayout( text, font, gd.getFontRenderContext() );
                gd.setPaint( Color.WHITE );
                gd.setFont( font );
                FontMetrics fm = gd.getFontMetrics();
                int x = (img.getWidth() / 2) - (fm.stringWidth( text ) / 2);
                int y = img.getHeight() - 20;
                textLayout.draw( gd, x, y );
                gd.dispose();
                /*
                 * float ninth = 1.0f / 9.0f;
                 * float[] kernel = new float[9];
                 * for ( int z = 0; z < 9; z++ )
                 * {
                 * kernel[z] = ninth;
                 * }
                 * ConvolveOp op = new ConvolveOp( new Kernel( 3, 3, kernel ), ConvolveOp.EDGE_NO_OP, null );
                 * BufferedImage image2 = op.filter( bi, null );
                 * Graphics2D g2 = image2.createGraphics();
                 * //VideoUtils.adjustGraphics( g2 );
                 * g2.setPaint( Color.BLACK );
                 * textLayout.draw( g2, x, y );
                 */
                
                DVRLoader.getExecutor().execute( writer.frameHandler( img ) );
            }
            
            busy = false;
        }
    }
}
