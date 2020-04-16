/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher;

import java.io.File;

import org.apache.commons.cli.Option;
import org.ogema.launcher.resolver.MavenResolver;
import org.ogema.launcher.util.LocalizationHelper;

/**
 * Interface that contains all necessary constants.
 * 
 * @author Marco Postigo Perez
 */
public interface LauncherConstants {
	
	public static final String LOCK_FILE = "ogema-launcher.lock";
	static final String DEFAULT_CFG_FILE_NAME = "config.xml";
	/** If no cfg file is given check if the default cfg file exists */
	static final String DEFAULT_CONFIG_FILE = "config" +
			File.separator + DEFAULT_CFG_FILE_NAME;
	static final String OSGI_BUNDLES_PROP = "osgi.bundles";
	static final String DEF_PROP_FILE = "org/ogema/launcher/props/default.properties";
    static final String VERSION_FILE = "org/ogema/launcher/props/version.properties";
	static final String DEFAULT_PROPERTIES_FILE = "config/ogema.properties";
	static final int DEF_START_LVL = 4;
	static final String USERDATA_PROPERTY = "org.ogema.userdata.path";
	static final long DEF_REFRESH_TIMEOUT = 5000;

	static final LocalizationHelper lh = LocalizationHelper.getInstance();
	
	/** Default framework bundle if not given in config file */
	static final BundleInfo DEF_FRAMEWORK_BUNDLE //= new BundleInfo("org.eclipse.tycho", "org.eclipse.osgi", "3.13.0.v20180226-1711", "bin");
		= new BundleInfo("org.apache.felix", "felix-framework", "6.0.1", "bin/system");
	
	/**
	 * Contains all known/allowed arguments for the Ogema Launcher.
	 */
	public enum KnownProgOptions {
		HELP("?", "help", lh.getStringForHelpOutput("help")),
		CONFIG_FILE("cfg", "config", lh.getStringForHelpOutput("config"), Option.UNLIMITED_VALUES, "file"),
		CONFIG_AREA("ca", "configarea", lh.getStringForHelpOutput("configarea"), 1, "directory"),
		PROPS("p", "properties", lh.getStringForHelpOutput("properties"),
				Option.UNLIMITED_VALUES, "file..."),
//				Option.UNINITIALIZED, "file..."),
		CLEAN("clean", "clean", lh.getStringForHelpOutput("clean")),
		OFFLINE("o", "offline", lh.getStringForHelpOutput("offline")),
		USE_RUNDIR_ONLY("uro", "use-rundir-only", lh.getStringForHelpOutput("userundironly")),
		/**
		 * Copies bundles to their target bin-location and creates an archive
		 */
		BUILD("b", "build", lh.getStringForHelpOutput("build")),
		/**
		 * Set to 'NONE' (case-insensitive) in order to disable archive creation
		 */
        OUTFILE("O", "output-archive", lh.getStringForHelpOutput("outfile"), 1, "file"),
		//BUILDOFFLINE("bo", "buildoffline", lh.getStringForHelpOutput("buildoffline")),
		VERBOSE("v", "verbose", lh.getStringForHelpOutput("verbose")),
		WORKSPACE_LOC("w", "workspaceloc", lh.getStringForHelpOutput("workspaceloc"), 1, "directory"),
		CONSOLE("c", "console", lh.getStringForHelpOutput("console"), 1, "port" , true),
        SECURITY("security", "security", lh.getStringForHelpOutput("security"), 1, "file" , true),
        //POLICY("policy", "policy", lh.getStringForHelpOutput("policy"), 1, "Java security policy" , true),
		//REFRESH_BUNDLES("rb", "refresh-bundles", lh.getStringForHelpOutput("refresh_bundles")),
        REFERENCE(null, "reference", lh.getStringForHelpOutput("reference")),
        REPOSITORIES(null, "repositories", String.format(lh.getStringForHelpOutput("repositories"), MavenResolver.REPOSITORY_CONFIG_DEFAULT), 1, "file", false),
		REFRESH_TIMEOUT("ut", "refresh-timeout", lh.getStringForHelpOutput("refresh_timeout"), 1, "timeout" ),
		RESTART("restart", "restart", lh.getStringForHelpOutput("restart")),
		UPDATE_BUNDLES("ub", "update-bundles", lh.getStringForHelpOutput("update_bundles")),
		DEPLOYMENT_PACKAGE("dp", "deployment-package", lh.getStringForHelpOutput("deployment-package")),
		TAG_SNAPSHOTS("ts", "tag-snapshots", lh.getStringForHelpOutput("tag-snapshots"),1,"isDiff",true),
		// development mode -> do not use in production
		STARTLEVEL("sl", "startlevel", lh.getStringForHelpOutput("startlevel"),1, "startlevel"),
		STRICT_MODE("s", "strict", lh.getStringForHelpOutput("strict"));

		private String cmdSwitch;
		private String longCmdSwitch = null;
		private String description;

        //additional argument
        private int nmbOfArgs = 0;
		private String argName = null;
		private boolean argOptional = false;
		
		private KnownProgOptions(String cmdswitch, String description) {
			this.cmdSwitch = cmdswitch;
			this.description = description;
		}
		
		private KnownProgOptions(String cmdswitch, String longCmdSwitch,
				String description) {
			this(cmdswitch, description);
			this.longCmdSwitch = longCmdSwitch;
		}
		
		private KnownProgOptions(String cmdswitch, String longCmdSwitch,
				String description, int numOfArgs, String argName) {
			this(cmdswitch, longCmdSwitch, description);
			this.nmbOfArgs = numOfArgs;
			this.argName = argName;
		}
		
		private KnownProgOptions(String cmdswitch, String longCmdSwitch,
				String description, int numOfArgs, String argName, boolean optional) {
			this(cmdswitch, longCmdSwitch, description);
			this.nmbOfArgs = numOfArgs;
			this.argName = argName;
			this.argOptional = optional;
		}

		public boolean isArgOptional() {
			return argOptional;
		}

		public String getSwitch() {
			return cmdSwitch;
		}

		public String getLongSwitch() {
			return longCmdSwitch;
		}

		public String getDescription() {
			return description;
		}
		
		public int getNmbOfArgs() {
			return nmbOfArgs;
		}

		public String getArgName() {
			return argName;
		}

	}

}