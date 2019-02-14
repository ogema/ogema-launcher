/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

import static org.ogema.launcher.config.ConfigurationConstants.FELIX_CONSOLE_PROP;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_CMD_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_CMD_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_CMD_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_RUNTIME_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_RUNTIME_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_RUNTIME_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_SHELL_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_SHELL_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_SHELL_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.SHELL_REMOTE_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.SHELL_REMOTE_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.SHELL_REMOTE_BUNDLE_GROUP_ID;

import org.ogema.launcher.BundleInfo;

public class FelixConfiguration extends FrameworkConfiguration {

	@Override
	public void activateOsgiBuiltInConsole(String port) {
		if(!containsBundle(GOGO_SHELL_BUNDLE_GROUP_ID, GOGO_SHELL_BUNDLE_ARTIFACT_ID)) {
			BundleInfo gogoShell = new BundleInfo();
			gogoShell.setMavenCoords(GOGO_SHELL_BUNDLE);
			addToBundles(gogoShell);
		}

		if(!containsBundle(GOGO_RUNTIME_BUNDLE_GROUP_ID, GOGO_RUNTIME_BUNDLE_ARTIFACT_ID)) {
			BundleInfo gogoRuntime = new BundleInfo();
			gogoRuntime.setMavenCoords(GOGO_RUNTIME_BUNDLE);
			addToBundles(gogoRuntime);
		}

		if(!containsBundle(GOGO_CMD_BUNDLE_GROUP_ID, GOGO_CMD_BUNDLE_ARTIFACT_ID)) {
			BundleInfo gogoCommand = new BundleInfo();
			gogoCommand.setMavenCoords(GOGO_CMD_BUNDLE);
			addToBundles(gogoCommand);
		}
		
		if(port != null && !port.isEmpty()) {
			// activate remote shell
			if(!containsBundle(SHELL_REMOTE_BUNDLE_GROUP_ID, SHELL_REMOTE_BUNDLE_ARTIFACT_ID)) {
				BundleInfo remoteShell = new BundleInfo();
				remoteShell.setMavenCoords(SHELL_REMOTE_BUNDLE);
				addToBundles(remoteShell);
			}
			
			addFrameworkProperty(FELIX_CONSOLE_PROP, port);
			// ip defaults to 127.0.0.1
			// max connections defaults to 2
			// socket timeout defaults to 0
		}
	}

}