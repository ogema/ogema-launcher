/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

public interface ConfigurationConstants {
	String EQUINOX_CONSOLE_PORT_PROP = "osgi.console";
	String EQUINOX_CONSOLE_SSH_PORT_PROP = "osgi.console.ssh";
	String EQUINOX_CONSOLE_PROP = "osgi.console.enable.builtin";
	String EQUINOX_DEF_STORAGE_PROP = "osgi.console.ssh.useDefaultSecureStorage";
	
	String FELIX_CONSOLE_PROP = "osgi.shell.telnet.port";
	
	// gogo shell bundles
	String GOGO_SHELL_BUNDLE_GROUP_ID = "org.apache.felix";
	String GOGO_SHELL_BUNDLE_ARTIFACT_ID = "org.apache.felix.gogo.shell";
	String GOGO_SHELL_BUNDLE_VERSION = "0.10.0";
	String GOGO_SHELL_BUNDLE = GOGO_SHELL_BUNDLE_GROUP_ID + ":" + GOGO_SHELL_BUNDLE_ARTIFACT_ID + ":" + GOGO_SHELL_BUNDLE_VERSION;
	
	String GOGO_RUNTIME_BUNDLE_GROUP_ID = "org.apache.felix";
	String GOGO_RUNTIME_BUNDLE_ARTIFACT_ID = "org.apache.felix.gogo.runtime";
	String GOGO_RUNTIME_BUNDLE_VERSION = "0.12.1";
	String GOGO_RUNTIME_BUNDLE = GOGO_RUNTIME_BUNDLE_GROUP_ID + ":" + GOGO_RUNTIME_BUNDLE_ARTIFACT_ID + ":" + GOGO_RUNTIME_BUNDLE_VERSION;
	
	String GOGO_CMD_BUNDLE_GROUP_ID = "org.apache.felix";
	String GOGO_CMD_BUNDLE_ARTIFACT_ID = "org.apache.felix.gogo.command";
	String GOGO_CMD_BUNDLE_VERSION = "0.14.0";
	String GOGO_CMD_BUNDLE = GOGO_CMD_BUNDLE_GROUP_ID + ":" + GOGO_CMD_BUNDLE_ARTIFACT_ID + ":" + GOGO_CMD_BUNDLE_VERSION;
	
	String SHELL_REMOTE_BUNDLE_GROUP_ID = "org.apache.felix";
	String SHELL_REMOTE_BUNDLE_ARTIFACT_ID = "org.apache.felix.shell.remote";
	String SHELL_REMOTE_BUNDLE_VERSION = "1.1.2";
	String SHELL_REMOTE_BUNDLE = SHELL_REMOTE_BUNDLE_GROUP_ID + ":" + SHELL_REMOTE_BUNDLE_ARTIFACT_ID + ":" + SHELL_REMOTE_BUNDLE_VERSION;
	
	// equinox incubator console bundles
	String JAAS_FRAGMENT_BUNDLE_GROUP_ID = "org.eclipse.equinox.console";
	String JAAS_FRAGMENT_BUNDLE_ARTIFACT_ID = "jaas-fragment";
	String JAAS_FRAGMENT_BUNDLE_VERSION = "1.0.0.v20130327-1442";
	String JAAS_FRAGMENT_BUNDLE = JAAS_FRAGMENT_BUNDLE_GROUP_ID + ":" + JAAS_FRAGMENT_BUNDLE_ARTIFACT_ID + ":" + JAAS_FRAGMENT_BUNDLE_VERSION;
	
	String SUPPORTABILITY_BUNDLE_GROUP_ID = "org.eclipse.equinox.console";
	String SUPPORTABILITY_BUNDLE_ARTIFACT_ID = "supportability";
	String SUPPORTABILITY_BUNDLE_VERSION = "1.0.0.201108021516";
	String SUPPORTABILITY_BUNDLE = SUPPORTABILITY_BUNDLE_GROUP_ID + ":" + SUPPORTABILITY_BUNDLE_ARTIFACT_ID + ":" + SUPPORTABILITY_BUNDLE_VERSION;
	
	String EQUINOX_CONSOLE_SSH_GROUP_ID = "org.eclipse.equinox";
	String EQUINOX_CONSOLE_SSH_ARTIFACT_ID = "console-ssh";
	String EQUINOX_CONSOLE_SSH_VERSION = "1.0.100.v20131208";
	String EQUINOX_CONSOLE_SSH = EQUINOX_CONSOLE_SSH_GROUP_ID + ":" + EQUINOX_CONSOLE_SSH_ARTIFACT_ID + ":" + EQUINOX_CONSOLE_SSH_VERSION;
	
	String EQUINOX_CONSOLE_GROUP_ID = "org.eclipse.equinox";
	String EQUINOX_CONSOLE_ARTIFACT_ID = "console";
	String EQUINOX_CONSOLE_VERSION = "1.1.0.v20140131";
	String EQUINOX_CONSOLE = EQUINOX_CONSOLE_GROUP_ID + ":" + EQUINOX_CONSOLE_ARTIFACT_ID + ":" + EQUINOX_CONSOLE_VERSION;
	
	String MINA_CORE_BUNDLE_GROUP_ID = "org.apache.mina";
	String MINA_CORE_BUNDLE_ARTIFACT_ID = "mina-core";
	String MINA_CORE_BUNDLE_VERSION = "2.0.7";
	String MINA_CORE_BUNDLE = MINA_CORE_BUNDLE_GROUP_ID + ":" + MINA_CORE_BUNDLE_ARTIFACT_ID + ":" + MINA_CORE_BUNDLE_VERSION;
	
	String SSHD_CORE_BUNDLE_GROUP_ID = "org.apache.sshd";
	String SSHD_CORE_BUNDLE_ARTIFACT_ID = "sshd-core";
	String SSHD_CORE_BUNDLE_VERSION = "0.12.0";
	String SSHD_CORE_BUNDLE = SSHD_CORE_BUNDLE_GROUP_ID + ":" + SSHD_CORE_BUNDLE_ARTIFACT_ID + ":" + SSHD_CORE_BUNDLE_VERSION;
	
	String DEF_OGEMA_USERDATA_PATH = "data";
	String OGEMA_SECURITY = "org.ogema.security";
}
