/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.exceptions.InitBundleInfoException;
import org.ogema.launcher.util.BundleInfoUtil;

/**
 * Tries to resolve the bundle location. Structured as chain of responsibility.
 * @author mperez
 */
public abstract class BundleResolver {
	/**
	 * We can't add the workspace resolver to the chain because it explicitly 
	 * needs the symbolic name of the bundle which can only be retrieved from
	 * an existing jar file (found by another resolver). Assumptions can't be
	 * made always except if maven coordinates are given and even then it is
	 * possible that the symbolic name was overwritten with another naming
	 * convention.</br>
	 * The workspace location will always be preferred and 
	 * if the bundle could be resolved then the workspace resolver will be invoked
	 * to check if the bundle is available in the workspace... if so then the
	 * preferred location of the bundle will be updated.
	 */
	protected static BundleResolver workspaceResolver;

	protected BundleResolver next;

	/** Check if there is another resolver in the chain. */
	public boolean hasNext() {
		return (next != null);
	}

	/**
	 * @param next - next parser in chain of responsibility
	 * @return - Returns the setted {@link BundleResolver}. 
	 */
	public BundleResolver setNext(BundleResolver next) {
		this.next = next;
		return next;
	}

	/**
	 * Tries to find the bundle given by the bundle info object.
	 * @param bi - Bundle info object which is used to find the
	 * appropriate bundle.
	 * @return {@link true} if the bundle could be resolved, else
	 * {@link false}.
	 */
	public boolean resolve(BundleInfo bi) {
		if (bi == null) {
			OgemaLauncher.LOGGER.warning(this.getClass().getCanonicalName()
					+ ": BundleInfo is null...");
			return false;
		}

		if(!canHandle(bi)) {
			if(hasNext()) {
				next.resolve(bi);
			}
		} else {
			bi.setResolved(resolveBundle(bi));
			if(bi.isResolved()) {
				try {
					// add symbolic name and version for upcoming update process ...
					BundleInfoUtil.addSymbolicNameToBundleInfo(bi);
					BundleInfoUtil.addVersionToBundleInfo(bi);
				} catch (InitBundleInfoException e) {
					OgemaLauncher.LOGGER.warning(e.getMessage());
				}
				
				// if we were able to resolve the bundle then we'll try to find
				// it in the workspace (if a location is given)
				if(workspaceResolver.canHandle(bi)) {
					workspaceResolver.resolveBundle(bi);
				}

			} else if(hasNext()) {
				// couldn't resolve the bundle --> try the next resolver in chain
				next.resolve(bi);
			}
		}

		return bi.isResolved();
	}

	/**
	 * Tries to resolve the given list of bundles and will return
	 * a list of bundles that couldn't be resolved as result.
	 * @param bundleInfos - Bundles to resolve
	 * @return A list of bundles that couldn't be resolved.
	 */
	public List<BundleInfo> resolveBundles(Collection<BundleInfo> bundleInfos) {
		// give user an output so that he knows where we at ...
		OgemaLauncher.LOGGER.info("Resolving bundles ...");
		List<BundleInfo> result = new ArrayList<BundleInfo>();
		for(BundleInfo bi: bundleInfos) {
			if(!resolve(bi)) {
				result.add(bi);
			}
		}
		OgemaLauncher.LOGGER.info("resolving done.");
		return result;
	}

	public static BundleResolver getWorkspaceResolver() {
		return workspaceResolver;
	}

	public static void setWorkspaceResolver(BundleResolver workspaceResolver) {
		BundleResolver.workspaceResolver = workspaceResolver;
	}

	/**
	 * Checks whether this {@link BundleResolver} can handle this
	 * {@link BundleInfo} or not.
	 *
	 * @param file The bundle info to be handled. This argument must not be {@link null}.
	 *
	 * @return {@link true} if the resolver can handle this bundle info else {@link false}.
	 */
	protected abstract boolean canHandle(BundleInfo bi);

	/**
	 * Resolves the bundle with given information and will set the needed
	 * bundle info attributes: {@link BundleInfo#setResolved(boolean)},
	 * {@link BundleInfo#setPreferredLocation(java.net.URI)},
	 * {@link BundleInfo#setSymbolicName(String)}
	 * @param bundleInfo
	 * @return  {@link true} the bundle could be resolved else {@link false}.
	 */
	protected abstract boolean resolveBundle(BundleInfo bi);
}