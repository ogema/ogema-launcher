/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Encapsulating localization into this class. Singleton for easy access.
 * 
 * @author Marco Postigo Perez
 */
public class LocalizationHelper {
	private static final String DEFAULT_LOCALE = "en";
	private static final String HELP_LOCALE_BASE_NAME = "org/ogema/launcher/locale/help";

	private static LocalizationHelper instance;
	
	private ResourceBundle helpResBundle;
	
	private LocalizationHelper() {
		try {
			helpResBundle = ResourceBundle.getBundle(HELP_LOCALE_BASE_NAME, Locale.getDefault());
		} catch(MissingResourceException e) {
			// fall back to default:
			helpResBundle = ResourceBundle.getBundle(HELP_LOCALE_BASE_NAME, new Locale(DEFAULT_LOCALE));
		}
	}
	
	public static synchronized LocalizationHelper getInstance() {
		if(instance == null) {
			instance = new LocalizationHelper();
		}

		return instance;
	}
	
	public ResourceBundle getResorceBundleForHelpOutput() {
		return helpResBundle;
	}
	
	public String getStringForHelpOutput(String key) {
        try{
            return helpResBundle.getString(key);
        } catch (MissingResourceException mre){
            return "///" + key + "///";
        }
	}
}
