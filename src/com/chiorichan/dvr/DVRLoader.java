package com.chiorichan.dvr;

import com.chiorichan.Loader;
import com.chiorichan.dvr.registry.InputRegistry;
import com.chiorichan.event.Listener;
import com.chiorichan.plugin.java.JavaPlugin;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import com.github.sarxos.webcam.log.WebcamLogConfigurator;

public class DVRLoader extends JavaPlugin implements Listener
{
	static DVRLoader instance;
	
	static
	{
		// If we are running on a Linux system, we want to use the Video4Linux4Java driver.
		if ( System.getProperty( "os.name" ).toLowerCase().contains( "linux" ) )
			Webcam.setDriver( new V4l4jDriver() );
	}
	
	public DVRLoader()
	{
		instance = this;
	}
	
	public void onEnable()
	{
		new InputRegistry();
		
		WebcamLogConfigurator.configure( DVRLoader.class.getClassLoader().getResourceAsStream( "com/chiorichan/dvr/logback.xml" ) );
		
		Loader.getLogger().info( "You are running OS: " + System.getProperty( "os.name" ) );
		
		Webcam.setHandleTermSignal( true );
		
		/*
		 * List<Webcam> webcams = Webcam.getWebcams( 10, TimeUnit.SECONDS );
		 * for ( Webcam w : webcams )
		 * {
		 * //System.out.println( "Found Webcamera: " + w.getName() );
		 * }
		 * Webcam webcam = webcams.get( 0 );
		 * webcam.open();
		 * BufferedImage image = webcam.getImage();
		 * Graphics2D gd = image.createGraphics();
		 * adjustGraphics( gd );
		 * Font font = new Font( "Sans", Font.BOLD, 26 );
		 * String text = "Camera 08 - Testing";
		 * TextLayout textLayout = new TextLayout( text, font, gd.getFontRenderContext() );
		 * gd.setPaint( Color.BLACK );
		 * gd.setFont( font );
		 * FontMetrics fm = gd.getFontMetrics();
		 * int x = ( image.getWidth() / 2 ) - ( fm.stringWidth( text ) / 2 );
		 * int y = image.getHeight() - 20;
		 * textLayout.draw( gd, x, y );
		 * gd.dispose();
		 * float ninth = 1.0f / 9.0f;
		 * float[] kernel = new float[9];
		 * for ( int z = 0; z<9; z++ )
		 * {
		 * kernel[z] = ninth;
		 * }
		 * ConvolveOp op = new ConvolveOp( new Kernel( 3, 3, kernel ), ConvolveOp.EDGE_NO_OP, null );
		 * BufferedImage image2 = op.filter( image, null );
		 * Graphics2D g2 = image2.createGraphics();
		 * adjustGraphics( g2 );
		 * g2.setPaint( Color.WHITE );
		 * textLayout.draw( g2, x, y );
		 * ImageIO.write( image2, "PNG", new File( "test.png" ) );
		 */
		
		Loader.getPluginManager().registerEvents( this, this );
		
		// Loader.getScheduler().scheduleSyncRepeatingTask( this, new SMSTask(), 1480L, 1480L );
	}
	
	public void onDisable()
	{
		
	}
}
