/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.registry;

import com.chiorichan.Loader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamException;

public class InputRegistry implements WebcamDiscoveryListener
{
    protected static List<VideoInput> inputs = new CopyOnWriteArrayList<VideoInput>();
    protected static InputRegistry instance;
    
    public static void destroyAllDevices()
    {
        for ( VideoInput i : inputs )
        {
            i.close();
        }
        
        inputs.clear();
    }
    
    public static void findAllDevices()
    {
        destroyAllDevices();
        
        try
        {
            for ( Webcam w : Webcam.getWebcams( 30, TimeUnit.SECONDS ) )
            {
                RegisterNewInput( w );
            }
        }
        catch ( WebcamException | TimeoutException e )
        {
            e.printStackTrace();
        }
    }
    
    public InputRegistry()
    {
        instance = this;
        Webcam.addDiscoveryListener( this );
    }
    
    @Override
    public void webcamFound( WebcamDiscoveryEvent arg0 )
    {
        RegisterNewInput( arg0.getWebcam() );
    }
    
    @Override
    public void webcamGone( WebcamDiscoveryEvent arg0 )
    {
        for ( VideoInput i : inputs )
        {
            if ( i.getDevice().equals( arg0.getWebcam() ) )
            {
                inputs.remove( i );
            }
        }
    }
    
    public static void openAllDevices()
    {
        for ( VideoInput i : inputs )
        {
            i.open();
        }
    }
    
    public static void closeAllDevices()
    {
        for ( VideoInput i : inputs )
        {
            i.close();
        }
    }
    
    public static VideoInput get( int index )
    {
        try
        {
            return inputs.get( index );
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            return null;
        }
    }
    
    public static void RegisterNewInput( Webcam w )
    {
        for ( VideoInput i : inputs )
        {
            if ( i.getDevice().getDevice().getName().equals( w.getDevice().getName() ) )
            {
                return;
            }
        }
        
        Loader.getLogger().info( "Adding VideoInput: " + w.getName() );
        
        inputs.add( new VideoInput( w ) );
    }
    
    public static int getInputCount()
    {
        return inputs.size();
    }
    
    public static void heartBeat()
    {
        inputs.get( 0 ).capture();

        /*
         * for ( VideoInput i : inputs )
         * {
         * i.capture();
         * }
         */
    }
}
