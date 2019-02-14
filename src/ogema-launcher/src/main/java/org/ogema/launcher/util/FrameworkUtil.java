/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.util;

import static org.ogema.launcher.LauncherConstants.USERDATA_PROPERTY;
import static org.ogema.launcher.config.ConfigurationConstants.DEF_OGEMA_USERDATA_PATH;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.codehaus.plexus.util.FileUtils;
import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.config.FrameworkConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleRevision;

public class FrameworkUtil {
	
//	@SuppressWarnings({"unchecked", "rawtypes"})
	public static URLClassLoader addFwkBundleToClasspath(final BundleInfo frameworkBundle, final ClassLoader baseClassLoader) {
		File fwkBundle = new File(frameworkBundle.getPreferredLocation().getSchemeSpecificPart());
		if (fwkBundle.exists()) {
			try {
				final URL url = frameworkBundle.getPreferredLocation().toURL();
				// we create a new classloader for the framework bundle everytime the framework is restartet
				// this is required for instance to ensure updated framework extensions are properly taken into
				// account in the class path
				final AccessibleURLClassloader urlClassLoader = new AccessibleURLClassloader(new URL[] {url}, baseClassLoader);
				urlClassLoader.addURL(url);
				/*
				Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
				method.setAccessible(true);
				method.invoke(urlClassLoader, new Object[]{url});
				*/
				OgemaLauncher.LOGGER.fine("added framwork bundle to system classpath");
				return urlClassLoader;
			} catch (Exception ex) {
//				System.err.println("Couldn't add framwork bundle to classpath: " + ex);
				throw new RuntimeException(ex);
			}
		} else {
			throw new RuntimeException("Could not find framework jar at " + fwkBundle);
		}
	}
	
	public static void doCleanStart(boolean clean, FrameworkConfiguration frameworkConfig, List<String> deleteList) {
		frameworkConfig.setCleanStart(clean);
		
		if(clean) {
			for(String s : deleteList) {
				File fileToDelete = new File(s);
				if(fileToDelete.exists()) {
					if(fileToDelete.isDirectory()) {
						deleteDirectory(fileToDelete);
					} else {
						if(!fileToDelete.delete()) {
							OgemaLauncher.LOGGER.warning("Unable to delete " + s);
						}
					}
				}
			}
			
			File frameworkStorage = getFrameworkStorage(frameworkConfig);
			if(frameworkStorage.exists()) {
				deleteDirectory(frameworkStorage);
			}
		}
	}

	private static void deleteDirectory(File fileToDelete) {
		try {
			FileUtils.deleteDirectory(fileToDelete);
		} catch (IOException e) {
			OgemaLauncher.LOGGER.warning("Unable to delete " + fileToDelete.getAbsolutePath() +
					" - Error message: " + e.getMessage());
		}
	}
	
	// FIXME is this really required?
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void fixJavaFxClasspath(final URLClassLoader urlClassLoader) {
		/* automatically retrofit classpath with JavaFX jar, necessary for Java 1.7 */
		File javaHome = new File(System.getProperty("java.home"));
		File javaFxRt = new File(new File(javaHome, "lib"), "jfxrt.jar");
		if (javaFxRt.exists()) {
			try {
//				URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
				Class urlClass = URLClassLoader.class;
				Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
				method.setAccessible(true);
				method.invoke(urlClassLoader, new Object[]{javaFxRt.toURI().toURL()});
				OgemaLauncher.LOGGER.fine("added JavaFX jar to system classpath");
			} catch (Exception ex) {
				OgemaLauncher.LOGGER.warning("fail: " + ex);
			}
		}
	}
	
	public static BundleInfo getClosestBundle(List<BundleInfo> tmpToInstall, Bundle installedBundle) {
		BundleInfo result = null;
		int min = Integer.MAX_VALUE;
		for (BundleInfo tmp : tmpToInstall) {
			int versionDif = getVersionDif(installedBundle.getVersion(),
					tmp.getVersion());
			if (result == null) {
				result = tmp;
				min = versionDif;
			} else if (versionDif < min) {
				result = tmp;
				min = versionDif;
			}
		}
		return result;
	}
	
	/**
	 * Get the framework storage and create the directory if it doesn't exist yet.
	 * @param frameworkConfig - The framework configuration
	 * @return The framework storage directory.
	 */
	public static File getFrameworkStorage(FrameworkConfiguration frameworkConfig) {
		File result = frameworkConfig.getFrameworkStorage();
		if(result == null) {
			result = new File(getOgemaUserdataPath(), "osgi-storage");
			frameworkConfig.setFrameworkStorage(result);
		}
		
		return result;
	}
	
	public static boolean createFrameworkStorage(FrameworkConfiguration frameworkConfig) {
		File frameworkStorage = getFrameworkStorage(frameworkConfig);
		if(!frameworkStorage.exists()) {
			return frameworkStorage.mkdirs();
		}
		
		return true;
	}
	
	public static boolean frameworkStorageExists(FrameworkConfiguration frameworkConfig) {
		return getFrameworkStorage(frameworkConfig).exists();
	}

	/**
	 * Gets the ogema userdata path and creates the directory if it doesn't exist yet.
	 * @return
	 */
	public static File getOgemaUserdataPath() {
		String userdataPath = System.getProperty(USERDATA_PROPERTY);
		if(userdataPath == null) {
			// backwards compatibility with ogema 1:
			userdataPath = System.getProperty("ogema.userdata.path");
			if(userdataPath == null) {
				// set to default:
				userdataPath = DEF_OGEMA_USERDATA_PATH;
			}
			
			// set old property for backwards compatibility:
			System.setProperty("ogema.userdata.path", userdataPath);
			System.setProperty(USERDATA_PROPERTY, userdataPath);
		}
		
		return new File(userdataPath);
	}
	
	public static boolean createOgemaUserdataPath() {
		File result = getOgemaUserdataPath();
		
		if(!result.exists()) {
			return result.mkdirs();
		}
		
		return true;
	}

	public static int getVersionDif(Version v1, Version v2) {
		return Math.abs((v1.getMajor() * 100 + v1.getMinor() * 10 + v1.getMicro())
				- (v2.getMajor() * 100 + v2.getMinor() * 10 + v2.getMicro()));
	}

	public static boolean isFragment(BundleRevision revision) {
		return ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) > 0);
	}
	
	private final static String[] FRAMEWORK_FACTORY_IMPLS = {
			"org.apache.felix.framework.FrameworkFactory",
			"org.eclipse.osgi.launch.EquinoxFactory",
			"org.knopflerfish.framework.FrameworkFactoryImpl",
			"org.eclipse.concierge.Factory"
	};
	
	// Java 9 quick fix
	public static Framework getFrameworkImpl(Map<String, String> frameworkProps, BundleInfo frameworkBundle, final ClassLoader frameworkClassloader) {
		for (String factory: FRAMEWORK_FACTORY_IMPLS) {
			try {
				final Class<?> cl = Class.forName(factory, true, frameworkClassloader);
				OgemaLauncher.LOGGER.info("Factory class loaded " + factory);
				try {
					return (Framework) cl.getMethod("newFramework", Map.class).invoke(cl.newInstance(), frameworkProps);
				} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					OgemaLauncher.LOGGER.warning("Unexpected exception " + e);
					continue;
				}
				
			} catch (ClassNotFoundException ignore) {
			}
		}
		return null;
	}
	
	public static Framework getFramework(Map<String, String> frameworkProps, BundleInfo frameworkBundle, URLClassLoader classLoader) {
		ServiceLoader<FrameworkFactory> facLoader = ServiceLoader.load(FrameworkFactory.class, classLoader);
		Iterator<FrameworkFactory> it = facLoader.iterator();
		if (!it.hasNext()) {
			facLoader = ServiceLoader.load(FrameworkFactory.class);
			it = facLoader.iterator();
		}
		if (!it.hasNext()) {
			return null;
		}
		final FrameworkFactory frameworkFactory = it.next();
		if (it.hasNext()) {
			OgemaLauncher.LOGGER.warning(String.format(
					"multiple OSGi FrameworkFactories on classpath, using first (%s)",
					frameworkFactory.getClass().getName()));
		}
		OgemaLauncher.LOGGER.log(Level.FINE, "Framework factory: {0}", frameworkFactory.getClass());
		return frameworkFactory.newFramework(frameworkProps);
	}
	
	public final static String getBundleState(final int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.STARTING:
			return "STARTING";
		default:
			return null;
		}
	}
	
	/**
	 * Required only to make the method addURL accessible. 
	 * Java >= 9 complains about illegal access otherwise when using reflections...
	 */
	private static class AccessibleURLClassloader extends URLClassLoader {

		public AccessibleURLClassloader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}
		
		@Override
		protected void addURL(URL url) {
			super.addURL(url);
		}
		
	}
	
}
