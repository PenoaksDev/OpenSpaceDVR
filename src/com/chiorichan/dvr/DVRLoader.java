/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.chiorichan.Loader;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.dvr.registry.InputRegistry;
import com.chiorichan.http.HttpResponse;
import com.chiorichan.http.HttpResponseStage;
import com.chiorichan.plugin.java.JavaPlugin;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DVRLoader extends JavaPlugin
{
    private int captureId = -1;
    private EventListener listener = new EventListener();

    private static Executor executor = null;

    static DVRLoader instance;

    static
    {
        // If we are running on a Linux system, we want to use the Video4Linux4Java driver.
        if ( System.getProperty( "os.name" ).toLowerCase().contains( "linux" ) )
        {
            Webcam.setDriver( new V4l4jDriver() );
        }
    }

    public DVRLoader()
    {
        //WebcamLogConfigurator.configure( DVRLoader.class.getClassLoader().getResourceAsStream( "com/chiorichan/dvr/logback.xml" ) );

        Loader.getLogger().info( "You are running OS: " + System.getProperty( "os.name" ) );

        instance = this;
    }

    public void startMultipart( final HttpResponse rep, int channel ) throws IOException
    {
        if ( channel < 0 || channel > InputRegistry.getInputCount() - 1 )
        {
            channel = 0;
        }

        final int channelNum = channel;

        rep.setContentType( "image/jpeg" );

        rep.sendMultipart( null );

        int _taskId = -1;

        class PushTask implements Runnable
        {
            int id = -1;

            @Override
            public void run()
            {
                if ( InputRegistry.get( channelNum ).getLastImage() != null )
                {
                    try
                    {
                        ByteArrayOutputStream bs = new ByteArrayOutputStream();
                        ImageIO.write( InputRegistry.get( channelNum ).getLastImage(), "JPG", bs );
                        bs.flush();
                        byte[] bss = bs.toByteArray();
                        bs.close();

                        rep.sendMultipart( bss );
                    }
                    catch ( IOException e )
                    {
                        if ( e.getMessage().toLowerCase().equals( "stream is closed" ) || e.getMessage().toLowerCase().equals( "broken pipe" ) )
                        {
                            Loader.getScheduler().cancelTask( id );
                            try
                            {
                                rep.closeMultipart();
                            }
                            catch ( Exception e1 )
                            {
                            }
                        }

                        Loader.getLogger().warning( e.getMessage(), e );
                    }
                }

                if ( rep.getStage() != HttpResponseStage.MULTIPART )
                {
                    Loader.getScheduler().cancelTask( id );
                    try
                    {
                        rep.closeMultipart();
                    }
                    catch ( Exception e1 )
                    {
                    }
                }
            }

            public void setId( int _taskId )
            {
                id = _taskId;
            }
        }
        ;

        PushTask task = new PushTask();

        _taskId = Loader.getScheduler().scheduleSyncRepeatingTask( this, task, 1L, 1L );
        task.setId( _taskId );
    }

    public byte[] grabSnapshot( int channel ) throws IOException
    {
        if ( channel < 0 || channel > InputRegistry.getInputCount() - 1 )
        {
            channel = 0;
        }

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ImageIO.write( InputRegistry.get( channel ).getLastImage(), "PNG", bs );
        bs.flush();
        byte[] bss = bs.toByteArray();
        bs.close();

        return bss;
    }

    @Override
    public void onEnable()
    {
        getConfig().addDefault( "config.storage", getDataFolder() );
        saveConfig();

        //Loader.getPluginManager().registerEvents( listener, this );
        InputRegistry.findAllDevices();

        InputRegistry.openAllDevices();

        captureId = Loader.getScheduler().scheduleSyncRepeatingTask( this, new CapturingTask(), 2L, 2L );
    }

    @Override
    public void onDisable()
    {
        Loader.getScheduler().cancelTask( captureId );
        InputRegistry.destroyAllDevices();
    }

    public static FileConfiguration getConfiguration()
    {
        return instance.getConfig();
    }

    public static Executor getExecutor()
    {
        if ( executor == null )
        {
            executor = Executors.newCachedThreadPool();
        }

        return executor;
    }
}
