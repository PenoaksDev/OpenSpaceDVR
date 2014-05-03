/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.registry;

import com.chiorichan.Loader;
import com.chiorichan.dvr.VideoWriter;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InputRegistry implements WebcamDiscoveryListener
{
	protected static List<VideoInput> inputs = new CopyOnWriteArrayList<VideoInput>();
	protected static InputRegistry instance;

	public static void destroyAllDevices()
	{
		List<VideoWriter> writers = VideoInput.getVideoWriters();

		if ( writers.size() > 0 )
		{
			for ( VideoWriter w : writers )
			{
				// VideoWriters might shutdown early if they manually check the isRunning boolean in DVRLoader.
				// But since it's important that VideoWriters are closed before we close the VideoInput, we notify it and wait.
				w.detachedFromInput = true;

				Loader.getLogger().info( "Notified thread `" + w.currentThread.getName() + "` of the shutdown! WAITING." );
			}

			boolean allFinished = false;
			long looperCount = 0;

			Boolean g = false;

			// Wait for all VideoWriters to finish their operation.
			// TODO: Monitor if any VideoWriters don't shutdown like they should, TERMINATE THEM!
			// Copied this way of doing things from the main Server Thread loop.
			for ( long j = 0L; !allFinished; g = true )
			{
				try
				{
					int state = 0;

					for ( VideoWriter w : writers )
					{
						if ( !w.isRunning )
							if ( state != 2 )
								state = 1;
							else
								state = 2;
					}

					if ( state == 1 )
						allFinished = true;

					looperCount++;

					// 10 Seconds
					if ( looperCount > 15000 )
						for ( VideoWriter w : writers )
						{
							// Is there a better way to do this?
							if ( w.currentThread != null && w.isRunning )
								w.currentThread.interrupt();
						}

					Thread.sleep( 100L );
				}
				catch ( Exception ex )
				{
					Loader.getLogger().severe( "Exception Thrown in VideoInput Shutdown: " + ex.getMessage() );
					allFinished = true;
					break;
				}
			}
		}

		for ( VideoInput i : inputs )
		{
			i.close();
		}

		writers = null;
		inputs.clear();

		Loader.getLogger().info( "All VideoWriters and VideoInputs closed!" );
	}

	public static void findAllDevices()
	{
		destroyAllDevices();

		try
		{
			for ( Webcam w : Webcam.getWebcams( 30, TimeUnit.SECONDS ) )
			{
				RegisterNewInput( w );
			}
		}
		catch ( WebcamException | TimeoutException e )
		{
			e.printStackTrace();
		}
	}

	public InputRegistry()
	{
		instance = this;
		Webcam.addDiscoveryListener( this );
	}

	@Override
	public void webcamFound( WebcamDiscoveryEvent arg0 )
	{
		RegisterNewInput( arg0.getWebcam() );
	}

	@Override
	public void webcamGone( WebcamDiscoveryEvent arg0 )
	{
		for ( VideoInput i : inputs )
		{
			if ( i.getDevice().equals( arg0.getWebcam() ) )
				inputs.remove( i );
		}
	}

	public static void openAllDevices()
	{
		for ( VideoInput i : inputs )
		{
			i.open();
		}
	}

	public static void closeAllDevices()
	{
		for ( VideoInput i : inputs )
		{
			i.close();
		}
	}

	public static VideoInput get( int index )
	{
		try
		{
			return inputs.get( index );
		}
		catch ( ArrayIndexOutOfBoundsException e )
		{
			return null;
		}
	}

	public static void RegisterNewInput( Webcam w )
	{
		for ( VideoInput i : inputs )
		{
			if ( i.getDevice().getDevice().getName().equals( w.getDevice().getName() ) )
				return;
		}

		Loader.getLogger().info( "Adding VideoInput: " + w.getName() );

		inputs.add( new VideoInput( w ) );
	}

	public static int getInputCount()
	{
		return inputs.size();
	}
}
