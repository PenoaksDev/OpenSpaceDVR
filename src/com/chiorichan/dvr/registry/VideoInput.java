/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.registry;

import com.chiorichan.Loader;
import com.chiorichan.dvr.DVRLoader;
import com.chiorichan.dvr.VP8Writer;
import com.chiorichan.dvr.utils.VideoUtils;
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
    protected VP8Writer writer;

    private boolean busy = false;

    public VideoInput( Webcam w )
    {
        device = w;
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
        writer = new VP8Writer( this );

        return device.open( true );
    }

    public boolean close()
    {
        writer.detachedFromInput = true;
        writer = null;

        return device.close();
    }

    public void capture()
    {
        if ( busy )
        {
            //   Loader.getLogger().info( "Starting Executor - BUSY!" );
        }
        else
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
        return 480;
    }

    public int getWidth()
    {
        return 640;
    }

    class ThreadedExecution implements Runnable
    {
        private VideoInput vi;

        public ThreadedExecution( VideoInput _vi )
        {
            vi = _vi;
        }

        @Override
        public void run()
        {
            if ( busy )
                return;

            busy = true;

            if ( device.isImageNew() )
            {
                img = device.getImage();

                if ( img != null )
                {
                    currentFPS = Math.round( 1000 / ((float) (System.currentTimeMillis() - lastTimeStamp)) );

                    lastTimeStamp = System.currentTimeMillis();

                    Graphics2D gd = img.createGraphics();
                    VideoUtils.adjustGraphics( gd );
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

                    Loader.getLogger().info( "Frame Saved for Input: " + device.getName() + " - " + currentFPS );
                    writer.addFrame( img );
                }

                busy = false;
            }
        }
    }
}
