/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.LauncherConstants;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.config.ConfigurationFactory;
import org.ogema.launcher.config.FrameworkConfiguration;
import org.ogema.launcher.exceptions.FrameworkConfigurationException;
import org.ogema.launcher.exceptions.InitBundleInfoException;

/**
 * Parser for OSGi config.ini files. This parser is also able to parse the
 * extended configuration format:<br />
 * {@code <simple bundle location>[@ [<start-level>] [":start"]] [(mvn=groupId:artifactId:version)] }
 * 
 * @author mperez
 */
public class ConfigIniParser extends ConfigurationParser {

	private static final String FILE_SUFFIX = ".ini";

    @Override
	protected FrameworkConfiguration parseFile(File configFile)
			throws FrameworkConfigurationException {
		Properties p = new Properties();
		try {
			p.load(new BufferedReader(new FileReader(configFile)));
		} catch (FileNotFoundException e) {
			throw new FrameworkConfigurationException(e);
		} catch (IOException e) {
			throw new FrameworkConfigurationException(e);
		}

		BundleInfo fwkBundle = null;
		if(p.contains("osgi.framework")) {
			String fwk = p.get("osgi.framework").toString();
			try {
				fwkBundle = getBundleInfo(fwk);
			} catch (InitBundleInfoException e) {
				OgemaLauncher.LOGGER.warning("Error: " +
						e.getLocalizedMessage());
				OgemaLauncher.LOGGER.warning("Using default framwork bundle: "
						+ LauncherConstants.DEF_FRAMEWORK_BUNDLE.getMavenCoords());
				// use default fwk ...
				fwkBundle = LauncherConstants.DEF_FRAMEWORK_BUNDLE;
			}
			
			
		} 
		if(fwkBundle == null) {
			// use default fwk:
			fwkBundle = LauncherConstants.DEF_FRAMEWORK_BUNDLE;
		}
		
		FrameworkConfiguration result = null;
		String mvnCoords = fwkBundle.getMavenCoords();
		result = ConfigurationFactory.createFrameworkConfiguration(
				mvnCoords != null ? mvnCoords : fwkBundle.getFileName());
		result.setFrameworkBundle(fwkBundle);
		
		for (String s : p.stringPropertyNames()) {
			if (s.equalsIgnoreCase(LauncherConstants.OSGI_BUNDLES_PROP)) {
				// osgi bundle property found: fill up our map with the bundles
				// that should be installed / updated
				String props = p.getProperty(s);
				if (props != null) {
					String[] bundlesFromProp = props.split(",");

					for (int i = 0; i < bundlesFromProp.length; ++i) {
						String entry = bundlesFromProp[i];
						BundleInfo bundleInfo;
						try {
							bundleInfo = getBundleInfo(entry);
						} catch (InitBundleInfoException e) {
							OgemaLauncher.LOGGER.warning("Error: " +
									e.getLocalizedMessage());
							continue;
						}
						result.addToBundles(bundleInfo);
					}
				} else {
					OgemaLauncher.LOGGER.severe("Config file contains an empty "
							+ "osgi.bundles property!");
				}
			} else { // property is an ordinary framework config prop
				result.addFrameworkProperty(s, p.getProperty(s));
			}
		}
		
		return result;
	}

	@Override
	protected boolean canHandle(File file) {
		return file.getName().toLowerCase().endsWith(FILE_SUFFIX);
	}

	private BundleInfo getBundleInfo(String cfgFileEntry)
			throws InitBundleInfoException {
		if(cfgFileEntry.startsWith("http") || cfgFileEntry.startsWith("ftp")
				|| cfgFileEntry.startsWith("sftp")) {
			throw new InitBundleInfoException("URLs aren't supported in " +
					"config.ini files yet: " + cfgFileEntry);
		}

		BundleInfo result = new BundleInfo();
		result.setBinDir(getBundleBinDir(cfgFileEntry));
		result.setFileName(getBundleFileName(cfgFileEntry));

		String mavenCoords = getMavenCoords(cfgFileEntry);
		result.setMavenCoords(mavenCoords);

		int startLevel = getStartLevel(cfgFileEntry);
		boolean start = getStart(cfgFileEntry);
		result.setStartLevel(startLevel);
		result.setStart(start);

		return result;
	}

	private boolean getStart(String cfgFileEntry) {
		Pattern startPattern = Pattern.compile("\\:start");
		Matcher matcher = startPattern.matcher(cfgFileEntry);

        return matcher.find();
	}

	private int getStartLevel(String cfgFileEntry) {
		int startLevel = LauncherConstants.DEF_START_LVL;
		Pattern startLevelPattern = Pattern.compile("\\@{1}[0-9]{1,}");
		Matcher matcher = startLevelPattern.matcher(cfgFileEntry);

		if(matcher.find()) {
			startLevel = Integer.parseInt(matcher.group().substring(1));
		}
		return startLevel;
	}
	
	private String getBundleBinDir(String cfgFileEntry) {
		// bundle location has to be at the beginning of each config.ini file entry:
		if(cfgFileEntry.contains("/")) {
			return cfgFileEntry.substring(0, cfgFileEntry.lastIndexOf("/"));
		} else if(cfgFileEntry.contains("\\")){
			// windows file separator used ?
			return cfgFileEntry.substring(0, cfgFileEntry.lastIndexOf("\\"));
		} else {
			// no bin dir given
			return "";
		}
	}

	private String getBundleFileName(String cfgFileEntry)
			throws InitBundleInfoException {
		// bundle location has to be at the beginning of each config.ini file entry:
		int idxOfDotJar = cfgFileEntry.indexOf(".jar");
		
		String result = cfgFileEntry.substring(0, idxOfDotJar+4);
		if(result.contains("/")) {
			result = result.substring(result.lastIndexOf("/") + 1);
		} else if(result.contains("\\")){
			// windows file seperator used ?
			result = result.substring(result.lastIndexOf("\\") + 1);
		}
		
		if(idxOfDotJar < 0) {
			throw new InitBundleInfoException("Invalid config file -> cannot extract file location" +
					" from: " + cfgFileEntry + " -> do you've maybe forgot " +
					"the \".jar\" at the end of the location?");
		}

		return result;
	}

	private String getMavenCoords(String cfgFileEntry) {
		int idxMavenCoords = cfgFileEntry.indexOf("(mvn=");
		if (idxMavenCoords > -1){
			return cfgFileEntry.substring(
					idxMavenCoords + 5,
					cfgFileEntry.lastIndexOf(")"));
		}

		return null;
	}

}