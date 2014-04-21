package com.chiorichan.dvr.registry;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;

public class VideoInput
{
	protected Webcam device;
	protected BufferedImage img = null;
	protected long lastTimeStamp = -1;
	protected int currentFPS = 0;
	protected String title = "Camera 0";
	
	public VideoInput(Webcam w)
	{
		device = w;
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
		img = device.getImage();
		
		if ( img != null )
		{
			currentFPS = Math.round( 1000 / ((float) (System.currentTimeMillis() - lastTimeStamp)) );
			
			lastTimeStamp = System.currentTimeMillis(); 
			
			Graphics2D gd = img.createGraphics();
			//VideoUtils.adjustGraphics( gd );
			Font font = new Font( "Sans", Font.BOLD, 26 );
			String text = title + " - " + currentFPS + " FPS";
			TextLayout textLayout = new TextLayout( text, font, gd.getFontRenderContext() );
			gd.setPaint( Color.WHITE );
			gd.setFont( font );
			FontMetrics fm = gd.getFontMetrics();
			int x = ( img.getWidth() / 2 ) - ( fm.stringWidth( text ) / 2 );
			int y = img.getHeight() - 20;
			textLayout.draw( gd, x, y );
			gd.dispose();
			/*float ninth = 1.0f / 9.0f;
			float[] kernel = new float[9];
			for ( int z = 0; z < 9; z++ )
			{
				kernel[z] = ninth;
			}
			ConvolveOp op = new ConvolveOp( new Kernel( 3, 3, kernel ), ConvolveOp.EDGE_NO_OP, null );
			BufferedImage image2 = op.filter( bi, null );
			Graphics2D g2 = image2.createGraphics();
			//VideoUtils.adjustGraphics( g2 );
			g2.setPaint( Color.BLACK );
			textLayout.draw( g2, x, y );*/
		}
	}
	
	public void setTitle( String text )
	{
		title = text;
	}
	
	public BufferedImage getLastImage()
	{
		return img;
	}
}
