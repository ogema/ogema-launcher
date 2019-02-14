/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_PORT_PROP;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_PROP;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_SSH;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_SSH_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_SSH_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_CONSOLE_SSH_PORT_PROP;
import static org.ogema.launcher.config.ConfigurationConstants.EQUINOX_DEF_STORAGE_PROP;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_CMD_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_CMD_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_CMD_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_RUNTIME_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_RUNTIME_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_RUNTIME_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_SHELL_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_SHELL_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.GOGO_SHELL_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.JAAS_FRAGMENT_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.JAAS_FRAGMENT_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.JAAS_FRAGMENT_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.MINA_CORE_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.MINA_CORE_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.MINA_CORE_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.SSHD_CORE_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.SSHD_CORE_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.SSHD_CORE_BUNDLE_GROUP_ID;
import static org.ogema.launcher.config.ConfigurationConstants.SUPPORTABILITY_BUNDLE;
import static org.ogema.launcher.config.ConfigurationConstants.SUPPORTABILITY_BUNDLE_ARTIFACT_ID;
import static org.ogema.launcher.config.ConfigurationConstants.SUPPORTABILITY_BUNDLE_GROUP_ID;

import java.io.File;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.util.FrameworkUtil;
import org.osgi.framework.Version;

public class EquinoxConfiguration extends FrameworkConfiguration {

	@Override
	public void activateOsgiBuiltInConsole(String port) {
		// equinox built in console property must be set to false 
		addFrameworkProperty(EQUINOX_CONSOLE_PROP, "false");

		// only add those bundles for console support if not already in config / bundle list 
		if(!containsBundle(GOGO_SHELL_BUNDLE_GROUP_ID, GOGO_SHELL_BUNDLE_ARTIFACT_ID)) {
			BundleInfo gogoShell = new BundleInfo();
			gogoShell.setMavenCoords(GOGO_SHELL_BUNDLE);
			gogoShell.setBinDir("bin/osgi");
			addToBundles(gogoShell);
		}

		if(!containsBundle(GOGO_RUNTIME_BUNDLE_GROUP_ID, GOGO_RUNTIME_BUNDLE_ARTIFACT_ID)) {
			BundleInfo gogoRuntime = new BundleInfo();
			gogoRuntime.setMavenCoords(GOGO_RUNTIME_BUNDLE);
			gogoRuntime.setBinDir("bin/osgi");
			addToBundles(gogoRuntime);
		}

		if(!containsBundle(GOGO_CMD_BUNDLE_GROUP_ID, GOGO_CMD_BUNDLE_ARTIFACT_ID)) {
			BundleInfo gogoCommand = new BundleInfo();
			gogoCommand.setMavenCoords(GOGO_CMD_BUNDLE);
			gogoCommand.setBinDir("bin/osgi");
			addToBundles(gogoCommand);
		}
		
		// since version 3.10 equinox changed the incubator console bundles ...:
		final String v = this.getFrameworkBundle().getMavenCoords().split(":")[2];
		final Version version;
		// this distinction is necessary to cope with both versions of the form
		// 3.9.1.v20130814-1242 and 3.13.0-SNAPSHOT
		if (v.length() - v.replace(".","").length() >= 3)
			version = new Version(v);
		else
			version = new Version(v.replaceFirst("-", "."));
		BundleInfo equinoxConsole = new BundleInfo();
		if(version.compareTo(new Version("3.10")) > 0) {
			if(!containsBundle(EQUINOX_CONSOLE_GROUP_ID, EQUINOX_CONSOLE_ARTIFACT_ID)) {
				equinoxConsole.setMavenCoords(EQUINOX_CONSOLE);
				equinoxConsole.setBinDir("bin/osgi");
				addToBundles(equinoxConsole);
			}
		} else {
			if(!containsBundle(SUPPORTABILITY_BUNDLE_GROUP_ID, SUPPORTABILITY_BUNDLE_ARTIFACT_ID)) {
				equinoxConsole.setMavenCoords(SUPPORTABILITY_BUNDLE);
				equinoxConsole.setBinDir("bin/osgi");
				addToBundles(equinoxConsole);
			}
		}
		
		// we need to add this property with empty port so that the console will be enabled
		// even if no port is given...
		addFrameworkProperty(EQUINOX_CONSOLE_PORT_PROP, "");

		if(port != null && !port.isEmpty()) {
			// enable remote access ...
			if(!containsBundle(JAAS_FRAGMENT_BUNDLE_GROUP_ID, JAAS_FRAGMENT_BUNDLE_ARTIFACT_ID)) {
				BundleInfo jaasBundle = new BundleInfo();
				jaasBundle.setMavenCoords(JAAS_FRAGMENT_BUNDLE);
				jaasBundle.setBinDir("bin/osgi");
				addToBundles(jaasBundle);
			}

			if(version.compareTo(new Version("3.10")) > 0) {
				if(!containsBundle(EQUINOX_CONSOLE_SSH_GROUP_ID, EQUINOX_CONSOLE_SSH_ARTIFACT_ID)) {
					BundleInfo sshBundle = new BundleInfo();
					sshBundle.setMavenCoords(EQUINOX_CONSOLE_SSH);
					sshBundle.setBinDir("bin/osgi");
					addToBundles(sshBundle);
				}
			}
			
			if(!containsBundle(SSHD_CORE_BUNDLE_GROUP_ID, SSHD_CORE_BUNDLE_ARTIFACT_ID)) {
				BundleInfo sshdBundle = new BundleInfo();
				sshdBundle.setMavenCoords(SSHD_CORE_BUNDLE);
				sshdBundle.setBinDir("bin/osgi");
				addToBundles(sshdBundle);
			}

			if(!containsBundle(MINA_CORE_BUNDLE_GROUP_ID, MINA_CORE_BUNDLE_ARTIFACT_ID)) {
				BundleInfo minaBundle = new BundleInfo();
				minaBundle.setMavenCoords(MINA_CORE_BUNDLE);
				minaBundle.setBinDir("bin/osgi");
				addToBundles(minaBundle);
			}

			addFrameworkProperty(EQUINOX_DEF_STORAGE_PROP, "true");
			addFrameworkProperty(EQUINOX_CONSOLE_SSH_PORT_PROP, "" + port);

			if(System.getProperty("java.security.auth.login.config") == null) {
				// set to default:
				System.setProperty("java.security.auth.login.config", "config/org.eclipse.equinox.console.authentication.config");
			}

			File userdataPath = FrameworkUtil.getOgemaUserdataPath();
			System.setProperty("org.eclipse.equinox.console.jaas.file", new File(userdataPath, "store").getAbsolutePath());
			System.setProperty("ssh.server.keystore", new File(userdataPath, "hostkey.ser").getAbsolutePath());
		}
	}

	@Override
	public void setCleanStart(boolean clean) {
		super.setCleanStart(clean);
		addFrameworkProperty("osgi.clean", String.valueOf(clean));
	}

}