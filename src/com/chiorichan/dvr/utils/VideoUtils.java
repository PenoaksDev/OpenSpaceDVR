package com.chiorichan.dvr.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class VideoUtils
{
	static void adjustGraphics( Graphics2D g )
	{
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		g.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
	}
}
