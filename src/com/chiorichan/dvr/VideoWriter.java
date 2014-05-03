/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.dvr.registry.VideoInput;
import com.chiorichan.dvr.storage.Interface;
import com.google.common.collect.Maps;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.joda.time.DateTime;

/**
 *
 * @author Chiori Greene
 */
public class VideoWriter implements Runnable
{
	public boolean isRunning;
	public Thread currentThread;
	private Interface storageInterface = new Interface();
	private long lastTen = 0L;
	private ZipOutputStream containerStream = null;
	private boolean frameEncoding = false;
	private final TreeMap<DateTime, BufferedImage> timeCodedFrames = Maps.newTreeMap();
	public boolean detachedFromInput = false;
	private final VideoInput vi;
	private File destFile;
	private ZipParameters parameters;

	public VideoWriter( VideoInput _vi )
	{
		vi = _vi;

		DVRLoader.getExecutor().execute( this );

		parameters = new ZipParameters();
		parameters.setCompressionMethod( Zip4jConstants.COMP_DEFLATE );
		parameters.setCompressionLevel( Zip4jConstants.DEFLATE_LEVEL_NORMAL );
		parameters.setEncryptFiles( true );
		parameters.setEncryptionMethod( Zip4jConstants.ENC_METHOD_AES );
		parameters.setAesKeyStrength( Zip4jConstants.AES_STRENGTH_256 );
		parameters.setPassword( "OpenSpaceDVR2014" ); // Generate a private key for each DVR install. Installation ID maybe?
	}

	@Override
	public void run()
	{
		currentThread = Thread.currentThread();

		Loader.getLogger().info( "Starting Video Writer Thread - " + currentThread.getName() );

		isRunning = true;

		do
		{
			try
			{
				if ( containerStream == null )
					changeDestFile();

				Entry<DateTime, BufferedImage> firstFrame = getFrame();

				if ( firstFrame != null )
				{
					frameHandler( firstFrame.getKey(), firstFrame.getValue() );
					timeCodedFrames.remove( firstFrame.getKey() );
				}

				Thread.sleep( 10L );
			}
			catch ( Exception ex )
			{
				Loader.getLogger().severe( "Exception Encountered in the Video Writer Thread (" + currentThread.getName() + ")", ex );
			}
		}
		while ( DVRLoader.isRunning && !detachedFromInput );

		Loader.getLogger().info( "Stopping Video Writer Thread - " + Thread.currentThread().getName() );

		try
		{
			finishUp();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		isRunning = false;
		currentThread = null;

		Loader.getLogger().info( "Video Writer Thread STOPPED! - " + Thread.currentThread().getName() );
	}

	public synchronized void addFrame( DateTime dt, BufferedImage bi )
	{
		timeCodedFrames.put( dt, bi );
	}

	private synchronized Entry<DateTime, BufferedImage> getFrame()
	{
		if ( timeCodedFrames.isEmpty() )
			return null;

		return timeCodedFrames.firstEntry();
	}

	private void changeDestFile() throws IOException
	{
		File lastDestFile = destFile;
		destFile = storageInterface.calculateContainingFile( new DateTime(), vi.getChannelName() );

		Loader.getLogger().info( "New destination selected for input " + vi.getChannelName() + ": " + destFile.getAbsolutePath() );

		// Close previous zip stream
		if ( containerStream != null )
			containerStream.finish();

		// Open new zip stream
		containerStream = new ZipOutputStream( new FileOutputStream( destFile ) );
		containerStream.setComment( "OpenSpaceDVR video storage container!" );

	}

	private void frameHandler( DateTime dt, BufferedImage img )
	{
		try
		{
			if ( img == null )
				return;

			frameEncoding = true;

			long start = System.currentTimeMillis();

			if ( lastTen != storageInterface.getTen( dt ) )
			{
				lastTen = storageInterface.getTen( dt );
				changeDestFile();
			}

			containerStream.putNextEntry( new ZipEntry( dt.getMillis() + ".jpg" ) );

			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			ImageIO.write( img, "JPG", bs );

			containerStream.write( bs.toByteArray() );

			containerStream.closeEntry();

			Loader.getLogger().info( ChatColor.YELLOW + "Writing Frame: Capture Time: " + dt.toString() + ", File Size: " + bs.size() + " bytes, Frames Buffered: " + timeCodedFrames.size() + ", Time Taken: " + (System.currentTimeMillis() - start) + ", Thread: " + Thread.currentThread().getName() );

			frameEncoding = false;
		}
		catch ( IOException ex )
		{
			Loader.getLogger().severe( "Exception encountered within the frameHandler method:", ex );
		}
	}

	private synchronized void finishUp() throws IOException
	{
		while ( frameEncoding )
		{
			try
			{
				sleep( 10 );
			}
			catch ( InterruptedException ex )
			{
			}
		}

		containerStream.close();
	}
}
