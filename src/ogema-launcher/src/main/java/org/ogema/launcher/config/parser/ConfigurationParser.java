/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config.parser;

import java.io.File;

import org.ogema.launcher.config.FrameworkConfiguration;
import org.ogema.launcher.exceptions.FrameworkConfigurationException;
import org.osgi.framework.Version;

/**
 * Handles configuration file parsing. Structured as chain of responsibility.
 */
public abstract class ConfigurationParser {
	/**
	 * Used to determine which properties (e.g. for built in console) are used 
	 * for a specific framework
	 */
	protected static Version frameworkVersion = null;
	
	/** Next element in chain of responsibility */
	protected ConfigurationParser next;
	protected Class<? extends FrameworkConfiguration> clazz;

	/**
	 * Checks whether this {@link ConfigurationParser} can handle this file or
	 * not.
	 *
	 * @param file The file to be handled. This argument must not be {@link null}.
	 *
	 * @return {@link true} if the parser can handle this file else {@link false}.
	 */
	protected abstract boolean canHandle(File file);

	/** Check if there is another parser in the chain. */
	public boolean hasNext() {
		return (next != null);
	}

	/**
	 * @param next - next parser in chain of responsibility
	 * @return - Returns the setted ConfigFileHandler. 
	 */
	public ConfigurationParser setNext(ConfigurationParser next) {
		this.next = next;
		return next;
	}

	public FrameworkConfiguration parse(File configFile)
			throws FrameworkConfigurationException {
		FrameworkConfiguration result = null;
		if (configFile == null || !configFile.canRead()) {
			throw new FrameworkConfigurationException("Config file "
					+ configFile != null ? "can't be read." : "is null.");
		}

		if(!canHandle(configFile)) {
			if(hasNext()) {
				result = next.parse(configFile);
			}
		} else {
			result = parseFile(configFile);
		}

		return result;
	}

	protected abstract FrameworkConfiguration parseFile(File configFile) 
			throws FrameworkConfigurationException ;
}
