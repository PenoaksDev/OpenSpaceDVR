package com.chiorichan.dvr;

import com.chiorichan.dvr.registry.InputRegistry;

public class CapturingTask implements Runnable
{
	@Override
	public void run()
	{
		InputRegistry.heartBeat();
	}
}
