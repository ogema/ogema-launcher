/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.resolver;

import org.apache.commons.cli.CommandLine;
import org.ogema.launcher.LauncherConstants;

/**
 * Used to create all available {@link BundleResolver}s. 
 * @author mperez
 */
public class ResolverFactory {
	
	/**
	 * Creates a chain of responsibility with all available resolvers
	 * regarding to the given command line options.
	 * @param options - Command line options
	 * @return - The head of the chain of responsibility
	 */
	public static BundleResolver createResolverChain(CommandLine options) {
		boolean rundirOnly = options.hasOption(LauncherConstants.KnownProgOptions.USE_RUNDIR_ONLY.getSwitch());
		if(rundirOnly) {
			// we will only use the binaries that can be found in the rundir
			BundleResolver.setWorkspaceResolver(new WorkspaceBundleResolver(null));
			return new BundleFileResolver();
		}
		// the workspace resolver is set explicitly as a static reference to
		// the BundleResolver chain -> for more detailed info @see BundleResolver
		String workspaceLoc = options.getOptionValue(
				LauncherConstants.KnownProgOptions.WORKSPACE_LOC.getSwitch());
		BundleResolver.setWorkspaceResolver(new WorkspaceBundleResolver(workspaceLoc));

		// highest priority has the maven resolver so he is the first in
		// our chain of responsibility:
		boolean offline = options.hasOption(LauncherConstants.KnownProgOptions.OFFLINE.getSwitch());
        String repCfgFile = options.getOptionValue(LauncherConstants.KnownProgOptions.REPOSITORIES.getLongSwitch(), null);
		BundleResolver result = new MavenResolver(offline, repCfgFile);
		
		// result must not change... use result.setNext(...).setNext(...) ... 
		// to add new resolver to this chain otherwise the first resolver
		// that was stored in result won't be involved. Use a "BundleResolver tmp"
		// var if necessary ...
		result.setNext(new BundleFileResolver());

		return result;
	}
}