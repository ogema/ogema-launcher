/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.resolver;

import java.io.File;
import java.net.URI;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.OgemaLauncher;

/**
 * This resolver tries to resolve the bundle via it's given
 * {@link BundleInfo#getFileLocation()} attribute. 
 * @author mperez
 */
public class BundleFileResolver extends BundleResolver {
	
	protected BundleFileResolver() {}

	@Override
	protected boolean canHandle(BundleInfo bi) {
		return bi != null && bi.getFileLocation() != null;
	}

	// this method explicitly relativizes the file location, in order for permissions
	// to apply
	@Override
	protected boolean resolveBundle(BundleInfo bi) {
		if(bi.getFileLocation().exists()) {
            URI bundleURI = bi.getFileLocation().toURI();
            URI workingDirURI = new File(System.getProperty("user.dir")).toURI();
            bundleURI = workingDirURI.relativize(bundleURI);
            bi.setPreferredLocation(URI.create("file:./"+bundleURI.toString()));
			OgemaLauncher.LOGGER.fine(this.getClass().getSimpleName() + ": " +
					"found " + bi.getPreferredLocation());
			return true;
		}
		return false;
	}
	
}
