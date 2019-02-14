/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.exceptions.InitBundleInfoException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleInfoUtil {

	public static void addVersionToBundleInfo(BundleInfo bi)
			throws InitBundleInfoException {
		bi.setVersion(BundleInfoUtil.getBundleVersionFromJar(
				new File(bi.getPreferredLocation().getSchemeSpecificPart())));
	}

	public static void addSymbolicNameToBundleInfo(BundleInfo bi)
			throws InitBundleInfoException {
		bi.setSymbolicName(BundleInfoUtil.getSymbolicNameFromJar(
				new File(bi.getPreferredLocation().getSchemeSpecificPart())));
	}

	public static String getSymbolicNameFromJar(File file)
			throws InitBundleInfoException {
		if(file == null) {
			throw new InitBundleInfoException("file must not be null!");
		}

		String result = null;
		JarFile jf = null;
		try {
			jf = new JarFile(file);
			Manifest mf = jf.getManifest();
			result = getSymbolicNameFromManifest(mf);
		} catch(InitBundleInfoException e) {
			throw new InitBundleInfoException(String.format("%s seems" +
					" to be no bundle!", file.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			throw new InitBundleInfoException(e);
		} catch (IOException e) {
			throw new InitBundleInfoException(e);
		} finally {
			if(jf != null) {
				try {
					jf.close();
				} catch (IOException ign) {}
			}
		}

		return result;
	}

	public static Version getBundleVersionFromJar(File file)
			throws InitBundleInfoException {
		if(file == null) {
			throw new InitBundleInfoException("file must not be null!");
		}

		Version result = null;
		JarFile jf = null;
		try {
			jf = new JarFile(file);
			Manifest mf = jf.getManifest();
			result = getBundleVersionFromManifest(mf);
		} catch (FileNotFoundException e) {
			throw new InitBundleInfoException(e);
		} catch (IOException e) {
			throw new InitBundleInfoException(e);
		} catch (InitBundleInfoException e) {
			throw new InitBundleInfoException(String.format("%s seems" +
					" to be no bundle!", file.getAbsolutePath()));
		} finally {
			if(jf != null) {
				try {
					jf.close();
				} catch (IOException ign) {}
			}
		}

		return result;
	}

	public static String getSymbolicNameFromManifest(Manifest mf)
			throws InitBundleInfoException {
		if(mf == null) {
			throw new InitBundleInfoException("mf must not be null!");
		}

		String symbolicName = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		if(symbolicName == null) {
			// no bundle!
			throw new InitBundleInfoException();
		}
		// cut off additional attributes like singleton:=true etc:
		int idxOf = symbolicName.indexOf(";");
		if(idxOf > 0) {
			symbolicName = symbolicName.substring(0, idxOf);
		}

		return symbolicName;
	}

	public static Version getBundleVersionFromManifest(Manifest mf)
			throws InitBundleInfoException {
		if(mf == null) {
			throw new InitBundleInfoException("mf must not be null!");
		}

		String version = mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		if(version == null) {
			// no bundle!
			throw new InitBundleInfoException("Can't get bundle version " +
					"from manifest -> jar is probably no bundle");
		}

		return new Version(version);
	}

	public static String getSymbolicNameFromWsLocation(File file) {
		BundleInfo bi = getBundleInfoFromWorkspaceLoc(file);
		if(bi != null) {
			return bi.getSymbolicName();
		}
		return null;
	}

	public static BundleInfo getBundleInfoFromWorkspaceLoc(File wsLoc) {
		BundleInfo result = null;
		File binDirectory = wsLoc;
		if(wsLoc.getName().equals("target")) {
			// mvn target ... we'll find all generated classes, manifest
			// and component description in the "classes" directory
			File[] classesDir = wsLoc.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return name.equals("classes");
				}
			});
			if(classesDir.length != 0) {
				binDirectory = classesDir[0];
			}
		}

		File[] metaInfDir = binDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.equals("META-INF");
			}
		});

		if(metaInfDir.length != 0) {
			// the manifest needs to be in bin directory
			// or we can't install/start the bundle appropriately
			File[] manifestFile = metaInfDir[0].listFiles(
					new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.equalsIgnoreCase("manifest.mf");
						}
					});

			if(manifestFile.length != 0) {
				result = parseManifest(manifestFile[0]);
				if(result != null) {
					result.setWorkspaceLocation(binDirectory);
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param manifestFile
	 * @return {@link BundleInfo} if a valid bundle were found else
	 * <code>null</code>
	 */
	private static BundleInfo parseManifest(File manifestFile) {
		BundleInfo result = null;

		try {
			Manifest mf = new Manifest(new FileInputStream(manifestFile));
			String symbName = BundleInfoUtil.getSymbolicNameFromManifest(mf);
			Version version = BundleInfoUtil.getBundleVersionFromManifest(mf);

			result = new BundleInfo(symbName, version);
		} catch (FileNotFoundException ignore) {
		} catch (IOException e) {
			OgemaLauncher.LOGGER.warning("Error while reading from manifest file: "
					+ manifestFile.getAbsolutePath() + ". Msg: "
					+ e.getLocalizedMessage());
		} catch (InitBundleInfoException e) {}

		return result;
	}
}