/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.launcher.BundleInfo;
import org.osgi.framework.Constants;

public abstract class FrameworkConfiguration implements Cloneable {
	private BundleInfo frameworkBundle;

	// only used for initializing - chosen a set to remove possible duplicates in config
	private List<BundleInfo> bundles =	new ArrayList<BundleInfo>();
	protected Map<String, String> properties = new HashMap<String, String>();
	private List<String> deleteList = new ArrayList<String>();
    
    {
        // pre-load framework properties with system properties
        for (Map.Entry<Object, Object> e: System.getProperties().entrySet()) {
            properties.put(e.getKey().toString(), e.getValue().toString());
        }
    }

	public BundleInfo getFrameworkBundle() {
		return frameworkBundle;
	}

	public void setFrameworkBundle(BundleInfo frameworkBundle) {
		this.frameworkBundle = frameworkBundle;
	}

	public void addToBundles(BundleInfo info) {
		if(info != null) {
			bundles.add(info);
		}
	}

	public void addToBundles(Collection<BundleInfo> bundles) {
		this.bundles.addAll(bundles);
	}

	public void removeFromBundles(BundleInfo info) {
		bundles.remove(info);
	}
	
	public boolean containsBundle(String groupId, String artifactId) {
		for(BundleInfo bi : bundles) {
			String mavenCoords = bi.getMavenCoords();
			if(mavenCoords != null && mavenCoords.split(":").length > 1) {
				String[] mvnCoordsSplit = mavenCoords.split(":");
				if(mvnCoordsSplit[0].equals(groupId) && mvnCoordsSplit[1].equals(artifactId)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Get all bundles that were read from the configuration file. */
	public List<BundleInfo> getBundles() {
		return bundles;
	}

	public void addFrameworkProperty(String key, String val) {
		properties.put(key, val);
	}

	public void addFrameworkProperties(Map<String, String> props) {
		this.properties.putAll(props);
	}

	/**
	 * Get all properties that were read from the configuration file,
	 * except the osgi.bundles property (use {@link #getBundles() instead}.
	 */
	public Map<String, String> getFrameworkProperties() {
		return properties;
	}

	public void setCleanStart(boolean clean) {
		// default... but unfortunately not working for all OSGi 
		// frameworks in all versions -> override this function if necessary
        if (clean){
            properties.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        }
	}

	public File getFrameworkStorage() {
		Object fwkStorage = properties.get(Constants.FRAMEWORK_STORAGE);
		if(fwkStorage != null) {
			return new File(fwkStorage.toString());
		} else {
			return null;
		}
	}

	public void setFrameworkStorage(File path) {
		properties.put(Constants.FRAMEWORK_STORAGE, path.getAbsolutePath());
	}

	public List<String> getDeleteList() {
		return deleteList;
	}

	public void setDeleteList(List<String> deleteList) {
		this.deleteList.clear();
		this.deleteList.addAll(deleteList);
	}

	/**
	 * Differs from every OSGi implementation. Set the appropriate property
	 * to activate the build in console.
	 * 
	 * @param port - may be null
	 */
	public abstract void activateOsgiBuiltInConsole(String port);
	
	@Override
	protected Object clone() {
		final FrameworkConfiguration cfg;
		try {
			cfg = (FrameworkConfiguration) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error("Clone not supported",e);
		}
		cfg.bundles = new ArrayList<>(this.bundles);
		cfg.deleteList = new ArrayList<>(this.deleteList);
		cfg.properties = new HashMap<>(this.properties);
		return cfg;
	}
}
