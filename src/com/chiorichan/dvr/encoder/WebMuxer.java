/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.encoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mkv.CuesIndexer;
import static org.jcodec.containers.mkv.CuesIndexer.CuePointMock.make;
import org.jcodec.containers.mkv.SeekHeadIndexer;
import org.jcodec.containers.mkv.Type;
import static org.jcodec.containers.mkv.Type.Audio;
import static org.jcodec.containers.mkv.Type.BitDepth;
import static org.jcodec.containers.mkv.Type.Channels;
import static org.jcodec.containers.mkv.Type.CodecID;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.Name;
import static org.jcodec.containers.mkv.Type.OutputSamplingFrequency;
import static org.jcodec.containers.mkv.Type.PixelHeight;
import static org.jcodec.containers.mkv.Type.PixelWidth;
import static org.jcodec.containers.mkv.Type.SamplingFrequency;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.TrackEntry;
import static org.jcodec.containers.mkv.Type.TrackNumber;
import static org.jcodec.containers.mkv.Type.TrackType;
import static org.jcodec.containers.mkv.Type.TrackUID;
import static org.jcodec.containers.mkv.Type.Tracks;
import org.jcodec.containers.mkv.ebml.BinaryElement;
import org.jcodec.containers.mkv.ebml.DateElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.FloatElement;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;

/**
 *
 * @author Chiori Greene
 */
public class WebMuxer
{
    List<WebMuxerTrack> tracks = new ArrayList<WebMuxerTrack>();
    private SeekableByteChannel out;
    private MasterElement mkvInfo;
    private MasterElement mkvTracks;
    private MasterElement mkvCues;
    private MasterElement mkvSeekHead;
    private MasterElement segmentElem;
    private LinkedList<Cluster> mkvClusters = new LinkedList<Cluster>();
    private String writingApp = "JCodec";
    private String muxingApp = "JCodec WebMuxer";

    public WebMuxer( SeekableByteChannel out )
    {
        this.out = out;
    }

    public WebMuxerTrack addVideoTrack( Size dimentions, String encoder )
    {
        WebMuxerTrack video = new WebMuxerTrack( tracks.size() + 1 );
        video.dimentions = dimentions;
        video.encoder = encoder;
        video.ttype = TType.VIDEO;
        tracks.add( video );
        return video;
    }

    public WebMuxerTrack addVideoTrack( Size dimentions, String encoder, int timescale )
    {
        WebMuxerTrack video = new WebMuxerTrack( tracks.size(), timescale );
        video.dimentions = dimentions;
        video.encoder = encoder;
        video.ttype = TType.VIDEO;
        tracks.add( video );
        return video;
    }

    public WebMuxerTrack addAudioTrack( Size dimentions, String encoder, int timescale, int sampleDuration, int sampleSize )
    {
        WebMuxerTrack audio = new WebMuxerTrack( tracks.size(), timescale );
        audio.encoder = encoder;
        audio.sampleDuration = sampleDuration;
        audio.sampleSize = sampleSize;
        audio.ttype = TType.AUDIO;
        tracks.add( audio );
        return audio;
    }

    void writeHeader() throws IOException
    {
        muxEbmlHeader();

        muxSegmentHeader();
    }

    public void mux() throws IOException
    {
        /**
         * In order to write Cues, one has to know the sized of Clusters fist.
         * thus blocks are organized into clusters before writing header.
         *
         */
        getVideoTrack().clusterBlocks();

        // EBML
        // SeekHead
        // Info
        // Tracks
        // Cues
        writeHeader();
        // Clusters
        muxClusters();

        segmentElem.mux( out );

        // TODO: Chapters
        // TODO: Attachments
        // TODO: Tags
    }

    private void muxSegmentHeader()
    {
        // # Segment
        segmentElem = (MasterElement) Type.createElementByType( Segment );

        // # Meta Seek
        // muxSeeks(segmentElem);
        muxInfo();
        muxTracks();
        muxSeekHead();
        //muxCues();

        // Tracks Info
        segmentElem.addChildElement( mkvSeekHead );
        segmentElem.addChildElement( mkvInfo );
        segmentElem.addChildElement( mkvTracks );
        //segmentElem.addChildElement( mkvCues );
    }

    private void muxCues()
    {
        CuesIndexer ci = new CuesIndexer( cuesOffsset(), 1 );
        for ( Cluster aCluster : mkvClusters )
        {
            ci.add( make( aCluster ) );
        }

        MasterElement indexedCues = ci.createCues();
        for ( Element aCuePoint : indexedCues.children )
        {
            mkvCues.addChildElement( aCuePoint );
        }

        System.out.println( "cues size: " + mkvCues.getSize() );
    }

    private void muxSeekHead()
    {
        SeekHeadIndexer shi = new SeekHeadIndexer();
        mkvCues = (MasterElement) Type.createElementByType( Cues );
        shi.add( mkvInfo );
        shi.add( mkvTracks );
        shi.add( mkvCues );
        mkvSeekHead = shi.indexSeekHead();
    }

    private long cuesOffsset()
    {
        return mkvSeekHead.getSize() + mkvInfo.getSize() + mkvTracks.getSize();
    }

    private void muxEbmlHeader() throws IOException
    {
        MasterElement ebmlHeaderElem = (MasterElement) Type.createElementByType( Type.EBML );

        StringElement docTypeElem = (StringElement) Type.createElementByType( Type.DocType );
        docTypeElem.set( "webm" );

        UnsignedIntegerElement docTypeVersionElem = (UnsignedIntegerElement) Type.createElementByType( Type.DocTypeVersion );
        docTypeVersionElem.set( 2 );

        UnsignedIntegerElement docTypeReadVersionElem = (UnsignedIntegerElement) Type.createElementByType( Type.DocTypeReadVersion );
        docTypeReadVersionElem.set( 2 );

        ebmlHeaderElem.addChildElement( docTypeElem );
        ebmlHeaderElem.addChildElement( docTypeVersionElem );
        ebmlHeaderElem.addChildElement( docTypeReadVersionElem );
        ebmlHeaderElem.mux( out );
    }

    private void muxClusters()
    {
        for ( Cluster cluster : mkvClusters )
        {
            segmentElem.addChildElement( cluster );
        }
    }

    private void muxTracks()
    {
        mkvTracks = (MasterElement) Type.createElementByType( Tracks );
        for ( WebMuxerTrack track : tracks )
        {
            MasterElement trackEntryElem = (MasterElement) Type.createElementByType( TrackEntry );

            createAndAddElement( trackEntryElem, TrackNumber, track.trackId );

            createAndAddElement( trackEntryElem, TrackUID, track.trackId );

            createAndAddElement( trackEntryElem, TrackType, track.getMkvType() );

            if ( track.getName() != null && !track.getName().isEmpty() )
                createAndAddElement( trackEntryElem, Name, track.getName() );

//                trackEntryElem.addChildElement(findFirst(track, TrackEntry, Language));
            createAndAddElement( trackEntryElem, CodecID, track.encoder );

//                trackEntryElem.addChildElement(findFirst(track, TrackEntry, CodecPrivate));
//                trackEntryElem.addChildElement(findFirst(track, TrackEntry, DefaultDuration));
            // Now we add the audio/video dependant sub-elements
            if ( track.isVideo() )
            {
                MasterElement trackVideoElem = (MasterElement) Type.createElementByType( Type.Video );

                createAndAddElement( trackVideoElem, PixelWidth, track.dimentions.getWidth() );
                createAndAddElement( trackVideoElem, PixelHeight, track.dimentions.getHeight() );

                trackEntryElem.addChildElement( trackVideoElem );
            }
            else if ( track.isAudio() )
            {
                MasterElement trackAudioElem = (MasterElement) Type.createElementByType( Audio );

                createAndAddElement( trackAudioElem, Channels, track.channels );
                createAndAddElement( trackAudioElem, BitDepth, track.bitdepth );
                createAndAddElement( trackAudioElem, SamplingFrequency, track.samplingFrequency );
                createAndAddElement( trackAudioElem, OutputSamplingFrequency, track.outputSamplingFrequency );

                trackEntryElem.addChildElement( trackAudioElem );
            }

            mkvTracks.addChildElement( trackEntryElem );
        }
    }

    public void setWritingApp( String name )
    {
        writingApp = name;
    }
    
    public void setMuxingApp( String name )
    {
        muxingApp = name;
    }
    
    private void muxInfo()
    {
        // # Segment Info
        mkvInfo = (MasterElement) Type.createElementByType( Type.Info );

        // Add timecode scale
        UnsignedIntegerElement timecodescaleElem = (UnsignedIntegerElement) Type.createElementByType( Type.TimecodeScale );
        timecodescaleElem.set( getVideoTrack().getTimescale() );
        mkvInfo.addChildElement( timecodescaleElem );

        FloatElement durationElem = (FloatElement) Type.createElementByType( Type.Duration );
        durationElem.set( getVideoTrack().getTrackTotalDuration() );
        mkvInfo.addChildElement( durationElem );

        DateElement dateElem = (DateElement) Type.createElementByType( Type.DateUTC );
        dateElem.setDate( new Date() );
        mkvInfo.addChildElement( dateElem );

        StringElement writingAppElem = (StringElement) Type.createElementByType( Type.WritingApp );
        writingAppElem.set( writingApp );
        mkvInfo.addChildElement( writingAppElem );

        StringElement muxingAppElem = (StringElement) Type.createElementByType( Type.MuxingApp );
        muxingAppElem.set( muxingApp );
        mkvInfo.addChildElement( muxingAppElem );
    }

    WebMuxerTrack getVideoTrack()
    {
        for ( WebMuxerTrack track : tracks )
        {
            if ( track.isVideo() )
                return track;
        }
        return null;
    }

    WebMuxerTrack getTimecodeTrack()
    {
        for ( WebMuxerTrack track : tracks )
        {
            if ( track.isTimecode() )
                return track;
        }
        return null;
    }

    List<WebMuxerTrack> getAudioTracks()
    {
        List<WebMuxerTrack> audio = new ArrayList<WebMuxerTrack>();
        for ( WebMuxerTrack t : tracks )
        {
            if ( t.isAudio() )
                audio.add( t );
        }

        return audio;
    }

    public static void createAndAddElement( MasterElement parent, Type type, byte[] value )
    {
        BinaryElement se = (BinaryElement) Type.createElementByType( type );
        se.setData( value );
        parent.addChildElement( se );
    }

    public static void createAndAddElement( MasterElement parent, Type type, double value )
    {
        FloatElement se = (FloatElement) Type.createElementByType( type );
        se.set( value );
        parent.addChildElement( se );
    }

    public static void createAndAddElement( MasterElement parent, Type type, long value )
    {
        UnsignedIntegerElement se = (UnsignedIntegerElement) Type.createElementByType( type );
        se.set( value );
        parent.addChildElement( se );
    }

    public static void createAndAddElement( MasterElement parent, Type type, String value )
    {
        StringElement se = (StringElement) Type.createElementByType( type );
        se.set( value );
        parent.addChildElement( se );
    }

    public enum TType
    {
        VIDEO, AUDIO, TIMECODE;
    }

    // MuxerTrack
    // UncompressedTrack
    // TimecodeTrack
    // CompressedTrack
    public class WebMuxerTrack
    {
        private static final int NANOSECONDS_IN_A_SECOND = 1000000000;
        public int bitdepth;
        public int channels;
        public double outputSamplingFrequency;
        public double samplingFrequency;
        public int sampleSize;
        public int sampleDuration;
        public long chunkDuration;
        public String encoder;
        public Size dimentions;
        public int trackId;
        private int timescale = 1000000;
        public int currentBlock = 0;
        public List<BlockElement> blocks = new ArrayList<BlockElement>();
        public String trackName = null;
        WebMuxer.TType ttype = TType.VIDEO;

        WebMuxerTrack( int trackId )
        {
            this.trackId = trackId;
        }

        WebMuxerTrack( int trackId, int timescale )
        {
            this.trackId = trackId;
            this.timescale = timescale;
        }

        public void setName( String name )
        {
            trackName = name;
        }

        public String getName()
        {
            return trackName;
        }

        public byte getMkvType()
        {
            //A set of track types coded on 8 bits (1: video, 2: audio, 3: complex, 0x10: logo, 0x11: subtitle, 0x12: buttons, 0x20: control).
            if ( isVideo() )
                return 0x01;
            if ( isAudio() )
                return 0x02;
            return 0x03;
        }

        public void setTgtChunkDuration( Rational duration, Unit unit )
        {

        }

        long getTrackTotalDuration()
        {
            return 0;
        }

        int getTimescale()
        {
            return timescale;
        }

        boolean isVideo()
        {
            return TType.VIDEO.equals( this.ttype );
        }

        boolean isTimecode()
        {
            return false;
        }

        boolean isAudio()
        {
            return TType.AUDIO.equals( this.ttype );
        }

        Size getDisplayDimensions()
        {
            return null;
        }

        public void addSampleEntry( BlockElement se )
        {
            blocks.add( se );
        }

        public void clusterBlocks()
        {
            int framesPerCluster = NANOSECONDS_IN_A_SECOND / timescale;
            long i = 0;
            for ( BlockElement be : blocks )
            {
                if ( i % framesPerCluster == 0 )
                {
                    Cluster c = Type.createElementByType( Type.Cluster );
                    createAndAddElement( c, Type.Timecode, i );
                    c.timecode = i;

                    if ( !mkvClusters.isEmpty() )
                    {
                        long prevSize = mkvClusters.getLast().getSize();
                        createAndAddElement( c, Type.PrevSize, prevSize );
                        c.prevsize = prevSize;
                    }

                    mkvClusters.add( c );
                }
                Cluster c = mkvClusters.getLast();
                be.timecode = (int) (i - c.timecode);
                c.addChildElement( be );
                i++;
            }
        }
    }
}
