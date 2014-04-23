/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr;

import com.chiorichan.Loader;
import com.chiorichan.dvr.registry.VideoInput;
import com.chiorichan.dvr.utils.AWTUtil;
import com.chiorichan.dvr.utils.VideoUtils;
import com.chiorichan.event.Listener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.RgbToYuv420p;
import org.joda.time.DateTime;

/**
 *
 * @author Chiori Greene
 */
public class MP4Writer implements Listener
{
    private VideoInput vi;
    private File destFile;
    private FileChannelWrapper ch;
    private MP4Muxer muxer;
    private FramesMP4MuxerTrack outTrack;
    private ArrayList<ByteBuffer> spsList;
    private ArrayList<ByteBuffer> ppsList;
    private int frameNo;

    public MP4Writer( VideoInput _vi )
    {
        vi = _vi;

        updateDest();
    }

    public void updateDest()
    {
        calculateDest();

        destFile.mkdirs();

        if ( destFile.exists() )
        {
            destFile.delete();
        }

        // Close and save last channel;
        if ( ch != null )
        {
            outTrack.addSampleEntry( H264Utils.createMOVSampleEntry( spsList, ppsList ) );

            try
            {
                muxer.writeHeader();
                NIOUtils.closeQuietly( ch );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }

        try
        {
            ch = NIOUtils.writableFileChannel( destFile );

            muxer = new MP4Muxer( ch, Brand.MP4 );
            outTrack = muxer.addTrack( TrackType.VIDEO, 25 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            ch = null;
        }
    }

    public File calculateDest()
    {
        DateTime dt = new DateTime();
        String sep = System.getProperty( "file.separator" );
        String suffix = sep + vi.getChannelName() + sep + dt.getYear() + sep + dt.getMonthOfYear() + sep + dt.getDayOfMonth() + sep + dt.getHourOfDay() + sep + ((Math.ceil( dt.getMinuteOfHour() / 5 )) * 5) + ".mp4";
        destFile = new File( DVRLoader.getConfiguration().getString( "config.storage", DVRLoader.instance.getDataFolder().getAbsolutePath() ) + suffix );

        Loader.getLogger().info( "Recalculated the recording location to be: " + destFile.getAbsolutePath() );

        return destFile;
    }

    public Runnable frameHandler( BufferedImage img )
    {
        return new FrameExecutor( img );
    }

    class FrameExecutor implements Runnable
    {
        private BufferedImage bi;
        private ByteBuffer _buffer;

        public FrameExecutor( BufferedImage _bi )
        {
            bi = _bi;
        }

        @Override
        public void run()
        {
            try
            {
                if ( _buffer != null )
                {
                    _buffer.clear();
                }

                ByteBuffer _out = ByteBuffer.allocate( vi.getHeight() * vi.getWidth() * 6 );
                RgbToYuv420p transform = new RgbToYuv420p( 0, 0 );

                Picture encoded = Picture.create( vi.getHeight(), vi.getWidth(), ColorSpace.YUV420 );

                transform.transform( AWTUtil.fromBufferedImage( bi ), encoded );

                _buffer = VideoUtils.getMP4Encoder().encodeFrame( encoded, _out );

                spsList.clear();
                ppsList.clear();
                H264Utils.wipePS( _buffer, spsList, ppsList );
                H264Utils.encodeMOVPacket( _buffer );

                outTrack.addFrame( new MP4Packet( _buffer, frameNo, 25, 1, frameNo, true, null, frameNo, 0 ) );

                frameNo++;
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }

        public ByteBuffer getFrame()
        {
            return _buffer;
        }
    }
}
