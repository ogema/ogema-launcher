/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.ogema.launcher.LauncherConstants;
import org.ogema.launcher.LauncherConstants.KnownProgOptions;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.config.parser.ConfigurationParser;
import org.ogema.launcher.config.parser.ParserFactory;
import org.ogema.launcher.exceptions.FrameworkConfigurationException;

public class LauncherConfiguration {
	/**
	 * chain of responsibility to support different types of 
	 * configuration (xml, ini, yaml ...)
	 */
	private final ConfigurationParser configParser = initConfigParsers();
	/** Configuration properties for the framework - differs from every implementation (felix, equinox, etc.) */
	private final FrameworkConfiguration frameworkConfig;
	private final CommandLine options;

	public LauncherConfiguration(CommandLine options) throws FrameworkConfigurationException {
		this(options,null);
	}
	
	public LauncherConfiguration(CommandLine options, FrameworkConfiguration config) throws FrameworkConfigurationException {
		this.options = options;
		if (config == null) {
			Set<Path> cfgFile = resolveCfgFile();
			if (!cfgFile.isEmpty()) {
				final Iterator<Path> it = cfgFile.iterator();
				config = configParser.parse(it.next().toFile());
				while (it.hasNext()) {
					config = ConfigurationFactory.merge(config, configParser.parse(it.next().toFile()));
				}
			}
		}
		frameworkConfig = config;
	}

	private ConfigurationParser initConfigParsers() {
		return ParserFactory.createConfigurationParser();
	}

	private Set<Path> resolveCfgFile() throws FrameworkConfigurationException {
		final Set<Path> result = new LinkedHashSet<>(4);
		boolean cfgParameterExists =
				options.hasOption(KnownProgOptions.CONFIG_FILE.getSwitch());
		if (cfgParameterExists) {
			final String[] vals = options.getOptionValues(KnownProgOptions.CONFIG_FILE.getSwitch());
			for (String val : vals) {
				Path f = Paths.get(val);
				if (!Files.exists(f)) {
					continue;
				} else if (Files.isDirectory(f)) {
					f = f.resolve(LauncherConstants.DEFAULT_CFG_FILE_NAME);
				}
				result.add(f);
			}
		} else {
			final Path f1 = Paths.get(LauncherConstants.DEFAULT_CONFIG_FILE);
			if (Files.isRegularFile(f1))
				result.add(f1);
			else {
				// try old config.ini default:
				final Path f2 = Paths.get(LauncherConstants.DEFAULT_CONFIG_FILE.replace(".xml", ".ini"));
				if (Files.isRegularFile(f2))
					result.add(f2);
			}
		}
		
		if (result.isEmpty()) {
			String msg;
			if (cfgParameterExists) {
				msg = "Given config file(s) not found " + Arrays.toString(options.getOptionValues(KnownProgOptions.CONFIG_FILE.getSwitch()))
						+ ". Use -? or --help parameter for further details.";
			} else {
				msg = "Missing config file parameter and couldn't find any configuration "
						+ "at the default location ("
						+ LauncherConstants.DEFAULT_CONFIG_FILE
						+ " | " + LauncherConstants.DEFAULT_CONFIG_FILE.replace(".xml", ".ini")
						+ ")";
			}
			throw new FrameworkConfigurationException(msg);
		}
		for (Path f : result) {
			if (!Files.isReadable(f)) {
				throw new FrameworkConfigurationException("Can't read from " +
						"config file: " + f.toAbsolutePath() +"! Check permissions.");
			}
		}
		OgemaLauncher.LOGGER.fine("Reading config files: " + result);
		return result;
	}

	public FrameworkConfiguration getFrameworkConfig() {
		return frameworkConfig;
	}

	public CommandLine getOptions() {
		return options;
	}
}
