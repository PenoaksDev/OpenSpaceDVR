package com.chiorichan.dvr.registry;

import com.github.sarxos.webcam.Webcam;

public class VideoInput
{
	protected Webcam device;
	
	public VideoInput(Webcam w)
	{
		device = w;
	}
	
	public Webcam getDevice()
	{
		return device;
	}
}
