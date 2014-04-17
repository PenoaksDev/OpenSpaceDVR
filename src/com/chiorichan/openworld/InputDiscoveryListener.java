package com.chiorichan.openworld;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

public class InputDiscoveryListener implements WebcamDiscoveryListener
{
	public InputDiscoveryListener()
	{
		for ( Webcam webcam : Webcam.getWebcams() )
		{
			System.out.println( "Webcam detected: " + webcam.getName() );
		}
		Webcam.addDiscoveryListener( this );
		System.out.println( "\n\nPlease connect additional webcams, or disconnect already connected ones. Listening for events..." );
	}
	
	@Override
	public void webcamFound( WebcamDiscoveryEvent event )
	{
		System.out.format( "Webcam connected: %s \n", event.getWebcam().getName() );
	}
	
	@Override
	public void webcamGone( WebcamDiscoveryEvent event )
	{
		System.out.format( "Webcam disconnected: %s \n", event.getWebcam().getName() );
	}
}
