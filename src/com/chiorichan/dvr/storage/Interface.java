/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, OpenSpace Solutions LLC. All Right Reserved.
 */
package com.chiorichan.dvr.storage;

import com.chiorichan.dvr.DVRLoader;
import java.io.File;
import org.joda.time.DateTime;

/**
 *
 * @author Chiori Greene
 */
public class Interface
{
	public Interface()
	{
	}

	public long getTen( DateTime td )
	{
		// Calculate the rounded ten minutes.
		double lastTen = Math.floor( td.getMinuteOfDay() / 10 );

		return Math.round( lastTen );
	}

	public File calculateContainingFile( DateTime td, String inputName )
	{
		String sep = System.getProperty( "file.separator", "/" );

		// Main storage folder
		File file = new File( DVRLoader.getConfiguration().getString( "config.storage", DVRLoader.instance.getDataFolder().getAbsolutePath() ) );

		// [storage]/2014/126/video1/block_[specialepoch].opv
		file = new File( file, td.getYear() + sep + td.getDayOfYear() + sep + inputName );

		// Create the needed directory structure.
		file.mkdirs();

		return new File( file, "block_" + getTen( td ) + ".opv" );
	}
}
