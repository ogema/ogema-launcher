/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.resolver;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.LauncherConstants.KnownProgOptions;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.util.BundleInfoUtil;

/**
 * This class will go recursively through the given workspace location 
 * {@link KnownProgOptions#WORKSPACE_LOC} and will store all bundles that
 * could be found in an internal map.
 * {@link BundleInfo#setPreferredLocation(java.net.URI)} will be invoked
 * if a bundle could be found in the workspace.
 */
public class WorkspaceBundleResolver extends BundleResolver {
	private Map<String, List<BundleInfo>> workspaceBundles =
			new HashMap<String, List<BundleInfo>>();
	private final File workspaceLoc;

	/**
	 * Creates a new workspace bundle resolver. It'll be searched 
	 * for all available bundles in the workspace while the object
	 * is created.
	 * @param workspaceLoc - Location to workspace. 
	 */
	protected WorkspaceBundleResolver(String workspaceLoc) {
		if(workspaceLoc != null) {
			this.workspaceLoc = new File(workspaceLoc);
			resolveBundlesFromWorkspace();
		} else {
			this.workspaceLoc = null;
		}
	}

	@Override
	protected boolean canHandle(BundleInfo bi) {
		// we need the symbolic name to have a clear assignemt:
		return workspaceLoc != null && bi != null
				&& bi.getSymbolicName() != null	&& bi.getVersion() != null;
	}

	@Override
	protected boolean resolveBundle(BundleInfo bi) {
		if(addWorkspaceLoc(bi)) {
			try {
				bi.setPreferredLocation(new URI("reference:" +
								bi.getWorkspaceLocation().toURI().toString()));
			} catch (URISyntaxException e) {
				OgemaLauncher.LOGGER.warning(e.getLocalizedMessage());
				return false;
			}
			
			OgemaLauncher.LOGGER.fine("Using workspace location for bundle "
					+ bi.getSymbolicName() + "-" + bi.getVersion() + ": "
					+ bi.getWorkspaceLocation());
			return true;
		}
		
		return false;
	}

	/**
	 * Checks if the given bundle is available in the workspace.
	 * If it is available then the workspace location will be added
	 * to the {@link BundleInfo}.
	 * @param bi
	 */
	private boolean addWorkspaceLoc(BundleInfo bi) {
		List<BundleInfo> list = workspaceBundles.get(bi.getSymbolicName());
		if(list != null) {
			for(BundleInfo info : list) {
				if(info.getVersion().equals(bi.getVersion())) {
					// found
					bi.setWorkspaceLocation(info.getWorkspaceLocation());
					return true;
				}
			}
		}
		return false;
	}

	private void findBundles(File directory) {
		File[] subDirs = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if(dir.getAbsolutePath().contains("src" + File.separator + "main")
						|| dir.getAbsolutePath().contains("src" + File.separator + "test")
						|| name.equals("doc")
						|| name.equals("META-INF")
						|| name.equals("OSGI-INF") || name.equals("apidocs")
						|| name.equals("generated-sources")
						|| name.equals("javadoc-bundle-options")
						|| name.equals("test-classes")
						|| name.startsWith(".")
						|| name.equals("temp") || name.equals("tmp")
						|| !(new File(dir, name).isDirectory())) {
					return false;
				}
				return true;
			}
		});
		
		// go through all subdirectories and try to find target / bin folder
		for(File dir : subDirs) {
			// symbolic links from windows are recognized as directories but listFiles
			// will return null... resolve symbolic link first.
			if(Files.isSymbolicLink(dir.toPath())) {
				try {
					// on MAC / Unix this will return a relative path ... on Windows an absolute
					Path newPath = Files.readSymbolicLink(dir.toPath());
					if(newPath.isAbsolute()) {
						dir = newPath.toFile();
					} else {
						// newPath.toFile() returns wrong path on MAC/Unix -> quickfix create file manually...
						dir = new File(directory, newPath.toString());
					}
					if(!dir.getAbsoluteFile().isDirectory()) {
						continue;
					}
				} catch (IOException e) {
					continue;
				}
			}
			
			File[] binDirectories = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.equals("target");
				}
			});

			if(binDirectories.length != 0) {
				for(File binDir : binDirectories) {
					BundleInfo bi = BundleInfoUtil.getBundleInfoFromWorkspaceLoc(binDir);
					if(bi != null) {
						OgemaLauncher.LOGGER.finer("Workspace location found for " +
								"bundle: " + bi.getSymbolicName() + "-" + 
								bi.getVersion() + ". Location: " +
								bi.getWorkspaceLocation());
						List<BundleInfo> list = workspaceBundles.get(bi.getSymbolicName());
						if(list == null) {
							list = new ArrayList<BundleInfo>();
						}
						list.add(bi);
						workspaceBundles.put(bi.getSymbolicName(), list);
						continue;
					}
				}
			}
			findBundles(dir);
		}
	}

	private void resolveBundlesFromWorkspace() {
		workspaceBundles.clear();
	
		if(workspaceLoc.exists()) {
			findBundles(workspaceLoc);
		} else {
			OgemaLauncher.LOGGER.warning("Cannot find given workspace " +
					"location: " + workspaceLoc.getAbsolutePath());
		}
	}
}