/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr;

import com.chiorichan.dvr.registry.InputRegistry;

public class CapturingTask implements Runnable
{
	@Override
	public void run()
	{
		InputRegistry.heartBeat();
	}
}
