/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr;

import com.chiorichan.Loader;
import com.chiorichan.dvr.encoder.WebMuxer;
import static com.chiorichan.dvr.encoder.WebMuxer.createAndAddElement;
import com.chiorichan.dvr.registry.VideoInput;
import com.google.common.collect.Maps;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.poi.util.IOUtils;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;

/**
 *
 * @author Chiori Greene
 */
public class VP8Writer implements Runnable
{
    private VideoInput vi;
    private File destFile;
    private FileOutputStream fos;
    private FileChannelWrapper ch;
    private ByteBuffer _buffer;
    private int frameNo;
    private boolean frameEncoding = false;
    private ByteBuffer _out;
    private VP8Encoder encoder;
    private Transform transform;
    private Picture toEncode;

    private WebMuxer muxer;
    private WebMuxer.WebMuxerTrack outTrack;

    private TreeMap<LocalTime, BufferedImage> timeCodedFrames = Maps.newTreeMap();
    private int currentTimecode = -1;
    public boolean detachedFromInput = false;

    public VP8Writer( VideoInput _vi )
    {
        vi = _vi;
        DVRLoader.getExecutor().execute( this );
    }

    @Override
    public void run()
    {
        Loader.getLogger().info( "Starting MP4 Writer Thread - " + Thread.currentThread().getName() );

        do
        {
            try
            {
                if ( ch == null )
                    setupWriter();

                Entry<LocalTime, BufferedImage> firstFrame = getFrame();

                if ( firstFrame != null )
                {
                    frameHandler( firstFrame.getKey(), firstFrame.getValue() );
                    timeCodedFrames.remove( firstFrame.getKey() );
                }

                sleep( 250 );
            }
            catch ( InterruptedException ex )
            {

            }
        }
        while ( DVRLoader.isRunning && !detachedFromInput );

        Loader.getLogger().info( "Stopping MP4 Writer Thread - " + Thread.currentThread().getName() );

        finishUp();
    }

    public synchronized void addFrame( BufferedImage bi )
    {
        LocalTime time = new LocalTime();
        timeCodedFrames.put( time, bi );
    }

    private synchronized Entry<LocalTime, BufferedImage> getFrame()
    {
        if ( timeCodedFrames.isEmpty() )
            return null;

        return timeCodedFrames.firstEntry();
    }

    public synchronized void setupWriter()
    {
        calculateDest();

        destFile.mkdirs();

        if ( destFile.exists() )
            destFile.delete();

        finishUp();

        try
        {
            fos = new FileOutputStream( destFile );
            ch = new FileChannelWrapper( fos.getChannel() );

            //_buffer = ByteBuffer.allocate( 1920 * 1080 * 6 );
            encoder = new VP8Encoder();

            muxer = new WebMuxer( ch );

            outTrack = muxer.addVideoTrack( new Size( 640, 480 ), "V_VP8" );

            transform = ColorUtil.getTransform( ColorSpace.RGB, encoder.getSupportedColorSpaces()[0] );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            ch = null;
        }
        finally
        {
            toEncode = null;
        }
    }

    private String currentClusterId = "";
    private Cluster currentCluster = null;
    private Cluster lastCluster = null;

    private synchronized void writeBlock( String clusterId, BlockElement be ) throws IOException
    {
        Loader.getLogger().info( "ClusterId: " + clusterId + ", lastClusterId: " + currentClusterId );

        if ( !clusterId.equals( currentClusterId ) )
        {
            if ( currentCluster != null )
            {
                // FINISH PREVIOUS CLUSTER

                ByteBuffer buff = currentCluster.mux();

                Loader.getLogger().debug( "Wrote Cluster (" + currentClusterId + "): " + buff.array().length );

                ch.write( buff );
            }

            // START NEW CLUSTER
            lastCluster = currentCluster;
            currentCluster = Type.createElementByType( Type.Cluster );

            createAndAddElement( currentCluster, Type.Timecode, frameNo );

            currentCluster.timecode = frameNo;

            if ( lastCluster != null )
            {
                long prevSize = lastCluster.getSize();
                createAndAddElement( currentCluster, Type.PrevSize, prevSize );
                currentCluster.prevsize = prevSize;
            }

            currentClusterId = clusterId;
        }

        be.timecode = (int) (frameNo - currentCluster.timecode);
        currentCluster.addChildElement( be );
        frameNo++;
    }

    private void frameHandler( LocalTime lt, BufferedImage img )
    {
        Loader.getLogger().info( "Writing Frame: " + lt.toString() + " - " + Thread.currentThread().getName() );

        frameEncoding = true;

        if ( _out != null )
            _out.clear();

        Picture pic = AWTUtil.fromBufferedImage( img, ColorSpace.YUV420 );
        /*
         * if ( toEncode == null )
         * toEncode = Picture.create( pic.getWidth(), pic.getHeight(), encoder.getSupportedColorSpaces()[0] );
         *
         * transform.transform( pic, toEncode );
         */
        _buffer = ByteBuffer.allocate( 1080 * 1920 * 6 );
        _out = encoder.encodeFrame( pic, _buffer );

        byte[] bytes = new byte[_out.limit()];

        for ( int o = 0; o < _out.limit(); o++ )
        {
            bytes[o] = _out.get( o );
        }

        BlockElement se = BlockElement.keyFrame( 0, frameNo, bytes );

        // Generate some sort of idenifier so we know when video advances to next frame.
        String clusterId = lt.getHourOfDay() + "" + lt.getMinuteOfHour() + "" + lt.getSecondOfMinute();

        try
        {
            writeBlock( clusterId, se );

            Loader.getLogger().debug( "Current File Size: " + ch.size() + ", Frame Size: " + bytes.length + ", Frame No: " + frameNo );
        }
        catch ( IOException ex )
        {
            ex.printStackTrace();
        }

        frameEncoding = false;

    }

    private synchronized void finishUp()
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

        if ( fos != null )
            try
            {
                IOUtils.closeQuietly( fos );
                if ( ch != null )
                    ch.close();
            }
            catch ( IOException ex )
            {
                ex.printStackTrace();
            }
            finally
            {
                ch = null;
                fos = null;
            }
    }

    public File calculateDest()
    {
        DateTime dt = new DateTime();
        String sep = System.getProperty( "file.separator" );
        String suffix = sep + vi.getChannelName() + sep + dt.getYear() + sep + dt.getMonthOfYear() + sep + dt.getDayOfMonth() + sep + dt.getHourOfDay() + sep + ((Math.ceil( dt.getMinuteOfHour() / 5 )) * 5) + ".webm";
        destFile = new File( DVRLoader.getConfiguration().getString( "config.storage", DVRLoader.instance.getDataFolder().getAbsolutePath() ) + suffix );

        Loader.getLogger().info( "Calculated recording location: " + destFile.getAbsolutePath() );

        return destFile;
    }
}
