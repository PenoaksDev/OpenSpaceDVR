/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr;

import com.chiorichan.event.Listener;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;

/**
 *
 * @author Chiori Greene
 */
class EventListener implements Listener, WebcamMotionListener
{
    public EventListener()
    {
    }

    @Override
    public void motionDetected( WebcamMotionEvent arg0 )
    {

    }
}
