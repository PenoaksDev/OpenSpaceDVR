/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.registry;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.dvr.DVRLoader;
import com.chiorichan.dvr.VideoWriter;
import com.chiorichan.dvr.utils.VideoUtils;
import com.github.sarxos.webcam.Webcam;
import com.google.common.collect.Lists;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.joda.time.DateTime;

public class VideoInput implements Runnable
{
	protected Webcam device;
	protected BufferedImage img = null;
	protected long lastTimeStamp = -1;
	protected int currentFPS = 0;
	protected String title = "Camera 0";
	protected VideoWriter writer;
	protected final static CopyOnWriteArrayList<VideoWriter> writers = Lists.newCopyOnWriteArrayList();
	public Thread currentThread;

	private boolean busy = false;

	public VideoInput( Webcam w )
	{
		device = w;

		// Temporary Testing Means
		if ( w.getName().equals( "/dev/video0" ) )
			DVRLoader.getExecutor().execute( this );
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
		writer = new VideoWriter( this );

		writers.add( writer );

		return device.open( false );
	}

	public boolean close()
	{
		writer.detachedFromInput = true;
		writers.remove( writer );
		writer = null;

		return device.close();
	}

	public static List<VideoWriter> getVideoWriters()
	{
		return writers;
	}

	public VideoWriter getVideoWriter()
	{
		return writer;
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

	@Override
	public void run()
	{
		currentThread = Thread.currentThread();

		Loader.getLogger().info( "Starting Video Input Thread - " + currentThread.getName() );

		do
		{
			try
			{
				if ( device.isOpen() )
				{
					img = device.getImage();

					if ( img != null )
						if ( busy == false )
						{
							busy = true;

							long start = System.currentTimeMillis();
							currentFPS = Math.round( 1000 / ((float) (System.currentTimeMillis() - lastTimeStamp)) );

							lastTimeStamp = System.currentTimeMillis();

							// Determine the current time for the timestamp and frame position
							DateTime dt = new DateTime();

							Graphics2D gd = img.createGraphics();
							VideoUtils.adjustGraphics( gd );
							Font font = new Font( "Sans", Font.PLAIN, 18 );
							String text = dt.toString( "YYYY/MM/dd hh:mm:ss.SSS aa" );
							TextLayout textLayout = new TextLayout( text, font, gd.getFontRenderContext() );
							gd.setPaint( Color.WHITE );
							gd.setFont( font );
							FontMetrics fm = gd.getFontMetrics();
							//int x = (img.getWidth() / 2) - (fm.stringWidth( text ) / 2);
							int x = img.getWidth() - fm.stringWidth( text ) - 20;
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

							Loader.getLogger().info( ChatColor.BLUE + "Frame Saved: Current FPS: " + currentFPS + ", Device: " + this.getChannelName() + ", Time Taken: " + (System.currentTimeMillis() - start) + ", Thread: " + currentThread.getName() );
							writer.addFrame( dt, img );

							busy = false;
						}
						else
							Loader.getLogger().warning( "Received a new Frame from Video Input Device but the processing subroutine was busy, Thread: " + currentThread.getName() + ", Device: " + this.getChannelName() );
				}

				// This should get us about 25 FPS if possible.
				Thread.sleep( 100L );
			}
			catch ( Exception ex )
			{
				Loader.getLogger().severe( "Exception Encountered in the Video Input Thread, Thread: " + currentThread.getName() + ", Device: " + this.getChannelName(), ex );
			}
		}
		while ( DVRLoader.isRunning );

		Loader.getLogger().info( "Stopping Video Input Thread - " + currentThread.getName() );
	}
}
