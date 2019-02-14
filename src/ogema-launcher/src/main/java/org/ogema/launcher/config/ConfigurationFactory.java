/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

public class ConfigurationFactory {
	/**
	 * Create a specific {@link FrameworkConfiguration} object for the given framework bundle
	 * file name or maven coordinates.
	 * @param mvnCoordsOrFileName - maven coordinates or file name.
	 * @return A specific {@link FrameworkConfiguration} object. This method will return a 
	 * {@link DefaultFrameworkConfiguration} object if the framework bundle cannot be specified by
	 * the given name.
	 */
	public static FrameworkConfiguration createFrameworkConfiguration(String mvnCoordsOrFileName) {
		// TODO open jar and read symbolic name from manifest ?! 
		String mvnCoordsLowerCase = mvnCoordsOrFileName.toLowerCase();
		if(mvnCoordsLowerCase.contains("equinox") || mvnCoordsLowerCase.contains("eclipse")) {
			return new EquinoxConfiguration();
		} else if (mvnCoordsLowerCase.contains("felix")) {
			return new FelixConfiguration();
		} else if (mvnCoordsLowerCase.contains("knopflerfish")) {
			return new KnopflerfishConfiguration();
		}
		
		return new DefaultFrameworkConfiguration();
	}
	
	public static FrameworkConfiguration merge(final FrameworkConfiguration cfg0, final FrameworkConfiguration cfg1) {
		final FrameworkConfiguration cfg = (FrameworkConfiguration) cfg0.clone();
		cfg.addFrameworkProperties(cfg1.getFrameworkProperties());
		cfg.addToBundles(cfg1.getBundles());
		cfg.getDeleteList().addAll(cfg1.getDeleteList());
		return cfg;
	}
	
}