package com.chiorichan.dvr.registry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamException;

public class InputRegistry implements WebcamDiscoveryListener
{
	protected static List<VideoInput> inputs = new CopyOnWriteArrayList<VideoInput>();
	
	public InputRegistry()
	{
		try
		{
			for ( Webcam w : Webcam.getWebcams( 30, TimeUnit.SECONDS ) )
			{
				inputs.add( new VideoInput( w ) );
			}
		}
		catch ( WebcamException | TimeoutException e )
		{
			e.printStackTrace();
		}
		
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
	
	public static void RegisterNewInput( Webcam w )
	{
		for ( VideoInput i : inputs )
			if ( i.getDevice().equals( w ) )
				return;
		
		inputs.add( new VideoInput( w ) );
	}
}
