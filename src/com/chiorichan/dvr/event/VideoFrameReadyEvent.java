/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.event;

import com.chiorichan.dvr.registry.VideoInput;
import com.chiorichan.dvr.utils.AWTUtil;
import com.chiorichan.dvr.utils.VideoUtils;
import com.chiorichan.event.Event;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.RgbToYuv420p;

/**
 *
 * @author Chiori Greene
 */
public class VideoFrameReadyEvent extends Event
{
    private ByteBuffer _buffer;
    private ArrayList<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
    private ArrayList<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
    private VideoInput vi;

    public VideoFrameReadyEvent( VideoInput _vi, BufferedImage bi )
    {
        vi = _vi;
        _buffer.clear();

        ByteBuffer _out = ByteBuffer.allocate( vi.getHeight() * vi.getWidth() * 6 );
        RgbToYuv420p transform = new RgbToYuv420p( 0, 0 );

        Picture encoded = Picture.create( vi.getHeight(), vi.getWidth(), ColorSpace.YUV420 );

        transform.transform( AWTUtil.fromBufferedImage( bi ), encoded );

        _buffer = VideoUtils.getMP4Encoder().encodeFrame( encoded, _out );

        spsList.clear();
        ppsList.clear();
        H264Utils.wipePS( _buffer, spsList, ppsList );
        H264Utils.encodeMOVPacket( _buffer );
    }
    
    public ArrayList<ByteBuffer> getSPSList()
    {
        return spsList;
    }
    
    public ArrayList<ByteBuffer> getPPSList()
    {
        return ppsList;
    }
    
    public ByteBuffer getFrame()
    {
        return _buffer;
    }

    public Object getVideoInput()
    {
        return vi;
    }
}
