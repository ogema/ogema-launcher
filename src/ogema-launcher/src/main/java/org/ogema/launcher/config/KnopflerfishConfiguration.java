/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

import org.ogema.launcher.OgemaLauncher;

public class KnopflerfishConfiguration extends FrameworkConfiguration {

	@Override
	public void activateOsgiBuiltInConsole(String port) {
		// knopflerfish don't have a osgi built in console and needs several
		// bundles to be installed... the main problem is that they all 
		// depend on specific Knopflerfish framework and bundle versions
		// rather than only the OSGi Interfaces... for further details on how
		// to activate the Knopflerfish console:
		// <http://www.knopflerfish.org/releases/current/docs/bundledoc/console/index.html>
		// <http://www.knopflerfish.org/releases/current/docs/bundledoc/consoletelnet/index.html>
		OgemaLauncher.LOGGER.warning("Unfortunately Knopflerfish doesn't " +
				"have a osgi built in console -> for further details on " +
				"how to activate the console please refer to: " +
				"http://www.knopflerfish.org/releases/current/docs/bundledoc/console/index.html");
//		BundleInfo console = new BundleInfo();
//		console.setMavenCoords("org.knopflerfish.bundle:console:3.0.4");
//		addToBundles(console);
//		
//		BundleInfo consoleApi = new BundleInfo();
//		consoleApi.setMavenCoords("org.knopflerfish.bundle:console-API:3.0.4");
//		addToBundles(consoleApi);
//		
//		BundleInfo logApi = new BundleInfo();
//		logApi.setMavenCoords("org.knopflerfish:log-API:3.1.3");
//		addToBundles(logApi);
//		
//		BundleInfo cmApi = new BundleInfo();
//		cmApi.setMavenCoords("org.knopflerfish.bundle:cm-API:3.0.4");
//		addToBundles(cmApi);
//		
//		BundleInfo consoletty = new BundleInfo();
//		consoletty.setMavenCoords("org.knopflerfish.bundle:consoletty-IMPL:3.0.1");
//		addToBundles(consoletty);
//		
//		BundleInfo fwkCommands = new BundleInfo();
//		fwkCommands.setMavenCoords("org.knopflerfish.bundle:frameworkcommands-IMPL:3.2.0");
//		addToBundles(fwkCommands);
//		
//		BundleInfo logCommands = new BundleInfo();
//		logCommands.setMavenCoords("org.knopflerfish.bundle:logcommands-IMPL:3.1.1");
//		addToBundles(logCommands);
	}

}
