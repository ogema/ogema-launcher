/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher;

import static org.ogema.launcher.LauncherConstants.DEF_REFRESH_TIMEOUT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.ogema.launcher.LauncherConstants.KnownProgOptions;
import org.ogema.launcher.config.ConfigurationConstants;
import org.ogema.launcher.config.EquinoxConfiguration;
import org.ogema.launcher.config.FrameworkConfiguration;
import org.ogema.launcher.config.LauncherConfiguration;
import org.ogema.launcher.resolver.BundleResolver;
import org.ogema.launcher.resolver.ResolverFactory;
import org.ogema.launcher.util.AbstractPackagingUtil;
import org.ogema.launcher.util.DeploymentPackageBuilder;
import org.ogema.launcher.util.FrameworkUtil;
import org.ogema.launcher.util.TarPackagingUtil;
import org.ogema.launcher.util.TgzPackagingUtil;
import org.ogema.launcher.util.ZipPackagingUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.packageadmin.PackageAdmin;

// We need the deprecated class PackageAdmin here because of backwards compatibility. 
// TODO atually test backwards compatibility, or remove the respective code
@SuppressWarnings("deprecation")
public class OgemaFramework {

	private final Object fwkLock = new Object();
	private long refreshTimeout = DEF_REFRESH_TIMEOUT;
	/**
	 * Felix is restarting the framework bundle if the system bundle or a system
	 * extension is updated in startup process. Equinox won't restart the framework
	 * bundle. If we receive an event that the framework bundle were stopped we will
	 * have to wait for the framework being reinitialized (by checking it's state in
	 * an own thread because we won't receive an event from Felix). Otherwise we
	 * won't wait and can continue with starting OGEMA.
	 */
	// private volatile boolean waitForFrameworkRestart = false;

	volatile CountDownLatch startLatch = new CountDownLatch(1);
	volatile CountDownLatch startLevelLatch = new CountDownLatch(1);
	private volatile Framework framework; // set in start method
	// private volatile boolean isHookRegistered = false;
	private volatile Thread shutdownHook; // if this is null, the hook is not registered yet

	private LauncherConfiguration configuration = null;
	private final SecurityManager initialSecurityManager = System.getSecurityManager();

	// for testing
	public void reset(LauncherConfiguration config) {
		// fwkLock = new Object();
		this.configuration = config;
		// waitForFrameworkRestart = false;
		startLatch = new CountDownLatch(1);
		startLevelLatch = new CountDownLatch(1);
	}

	// for testing
	public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
		return startLevelLatch.await(timeout, unit);
	}

	// for testing
	public boolean awaitStop(long timeout) throws InterruptedException {
		final Framework framework = this.framework;
		if (framework == null)
			return true;
		FrameworkEvent event = framework.waitForStop(timeout);
		int type = event.getType();
		OgemaLauncher.LOGGER.log(Level.INFO, "Stop with event type " + event.toString());
		if (type == FrameworkEvent.STOPPED || type == FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED
				|| type == FrameworkEvent.STOPPED_UPDATE || type == 1024) // new in R7
			return true;
		else
			return false;
		// return stopLatch.await(timeout, unit);
	}

	// for testing
	public void stop() throws BundleException {
		// shutdownJVM = false;
		final Framework framework = this.framework;
		if (framework != null)
			framework.stop();
	}

	// for testing
	public BundleContext getBundleContext() {
		return framework.getBundleContext();
	}

	/**
	 * may be null (before framework has been started) 
	 * @return
	 */
	// for testing
	public Framework getFramework() {
		return framework;
	}
	
	// for testing
	public boolean stopFramework(long timeout) throws BundleException, InterruptedException {
		// framework.getBundleContext().removeBundleListener(bundleListener);
		stop();
		// long t0 = System.currentTimeMillis();
		// Assert.assertTrue("Framework failed to stop within 30 seconds",
		// ogemaFramework.awaitStop(30000));
		boolean stopped = awaitStop(timeout);
		// long delta = (System.currentTimeMillis() - t0)/1000;
		return stopped;
	}

	public void discard() {
		final Thread shutdownHook = this.shutdownHook;
		if (shutdownHook != null) {
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		}
	}

	final FrameworkListener frameworkListener = new FrameworkListener() {
		@Override
		public void frameworkEvent(FrameworkEvent fe) {
			// if (OgemaLauncher.LOGGER.isLoggable(Level.FINER))
			// OgemaLauncher.LOGGER.log(Level.FINER, "Framework event: " + fe.getType());
			switch (fe.getType()) {
			case FrameworkEvent.STARTED:
				startLatch.countDown();
				break;
			case FrameworkEvent.STARTLEVEL_CHANGED:
				FrameworkStartLevel fsl = (FrameworkStartLevel) fe.getBundle().adapt(FrameworkStartLevel.class);
				OgemaLauncher.LOGGER.fine("Reached framework start level: " + fsl.getStartLevel());
				startLevelLatch.countDown();
				break;
			case FrameworkEvent.WARNING:
				OgemaLauncher.LOGGER.warning(
						"Warning: " + fe.getSource() + (fe.getThrowable() != null ? " - " + fe.getThrowable() : ""));
				break;
			case FrameworkEvent.ERROR:
				String errMsg = "Error occured: " + fe.getThrowable() != null ? fe.getThrowable().getMessage() + " "
						: "";
				errMsg += fe.getThrowable().getCause() != null ? "- cause: " + fe.getThrowable().getCause().getMessage()
						: "";
				OgemaLauncher.LOGGER.severe(errMsg);
				break;
			default:
				// ignore
				// OgemaLauncher.LOGGER.fine("unused framework event: " + fe.getType());
			}
		}
	};
	
	private final void checkFrameworkState() {
		Runnable checkFrameworkState = new Runnable() {
			@Override
			public void run() {
				synchronized (fwkLock) {
					long maxWait = System.currentTimeMillis() + refreshTimeout; // for now hard coded 5
																				// seconds
					while (framework.getState() != Bundle.ACTIVE && System.currentTimeMillis() < maxWait) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException ignore) {
						}
					}
					if (OgemaLauncher.LOGGER.isLoggable(Level.FINE)) {
						OgemaLauncher.LOGGER.log(Level.FINE,
								"Finished wait for framework update. Current state is starting or active: "
										+ (framework.getState() == Bundle.STARTING
												|| framework.getState() == Bundle.ACTIVE)
										+ "; state: " + framework.getState());
					}
					// if the framework did not restart by itself, it either requires a new classloader
					// (framework extension updated), or even a fresh VM (bootstrap extension fragment updated)
					// -> we better call the start();
					if (framework.getState() == Bundle.INSTALLED || framework.getState() == Bundle.RESOLVED) { 
						// this would  lead to an immediate failure
						OgemaLauncher.LOGGER.log(Level.WARNING,
								"Framework did not recover from refreshing bundles. Trying to start again.");
//						maxWait = System.currentTimeMillis() + refreshTimeout; // for now hard coded 5
//																				// seconds
						if (System.getSecurityManager() != null	&& System.getSecurityManager() != initialSecurityManager)
							System.setSecurityManager(initialSecurityManager);

							// instead of starting the same instance, we restart the framework with a new classloader
//							fwk.start();
//							while (fwk.getState() != Bundle.STARTING && fwk.getState() != Bundle.ACTIVE	&& System.currentTimeMillis() < maxWait) {
//								Thread.sleep(100);
//							}
					}
					try {
						fwkLock.notifyAll();
					} catch (IllegalMonitorStateException e) {
						/* happens on restart, e.g. in tests */ }
				}
			}
		};
		Thread restartThread = new Thread(checkFrameworkState);
		restartThread.setName("launcher-restart-observer");
		restartThread.start();
	}
/*
	final BundleListener bundleListener = new SynchronousBundleListener() {

		@Override
		public void bundleChanged(BundleEvent event) {

			switch (event.getType()) {
			case BundleEvent.STOPPING:
				OgemaLauncher.LOGGER.log(Level.FINE, "Framework bundle stop event received");
				// we're only interested if the framework bundle is going to stop ...
				if (event.getSource() instanceof Framework) {
					framework.getBundleContext().removeBundleListener(this);
					final Framework fwk = (Framework) event.getSource();
					// framework bundle is stopping ...
					Runnable checkFrameworkState = new Runnable() {
						@Override
						public void run() {
							synchronized (fwkLock) {
								long maxWait = System.currentTimeMillis() + refreshTimeout; // for now hard coded 5
																							// seconds
								while (fwk.getState() != Bundle.STARTING && fwk.getState() != Bundle.ACTIVE
										&& System.currentTimeMillis() < maxWait) {
									try {
										Thread.sleep(100);
									} catch (InterruptedException ignore) {
									}
								}
								if (OgemaLauncher.LOGGER.isLoggable(Level.FINE)) {
									OgemaLauncher.LOGGER.log(Level.FINE,
											"Finished wait for framework update. Current state is starting or active: "
													+ (fwk.getState() == Bundle.STARTING
															|| fwk.getState() == Bundle.ACTIVE)
													+ "; state: " + fwk.getState());
								}
								// if the framework did not restart by itself, it either requires a new classloader
								// (framework extension updated), or even a fresh VM (bootstrap extension fragment updated)
								// -> we better call the start();
								if (fwk.getState() == Bundle.INSTALLED || fwk.getState() == Bundle.RESOLVED) { 
									// this would  lead to an immediate failure
									OgemaLauncher.LOGGER.log(Level.WARNING,
											"Framework did not recover from refreshing bundles. Trying to start again.");
//									maxWait = System.currentTimeMillis() + refreshTimeout; // for now hard coded 5
//																							// seconds
									if (System.getSecurityManager() != null	&& System.getSecurityManager() != initialSecurityManager)
										System.setSecurityManager(initialSecurityManager);

										// instead of starting the same instance, we restart the framework with a new classloader
//										fwk.start();
//										while (fwk.getState() != Bundle.STARTING && fwk.getState() != Bundle.ACTIVE	&& System.currentTimeMillis() < maxWait) {
//											Thread.sleep(100);
//										}
								}
								try {
									fwkLock.notifyAll();
								} catch (IllegalMonitorStateException e) {
									// happens on restart, e.g. in tests 
								}
							}
						}
					};
					Thread restartThread = new Thread(checkFrameworkState);
					restartThread.setName("launcher-restart-observer");
					restartThread.start();
				}
				break;
			}
		}
	};
	*/

	public OgemaFramework(LauncherConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * @return whether to restart when the method has finished
	 */
	public RestartType start(final ClassLoader baseClassLoader) {
		FrameworkConfiguration frameworkConfig = configuration.getFrameworkConfig();
		CommandLine options = configuration.getOptions();

		final boolean isRestart = options.hasOption(KnownProgOptions.RESTART.getSwitch());
		boolean clean = !isRestart && options.hasOption(KnownProgOptions.CLEAN.getSwitch());
		final boolean isBuildSwitchSet = options.hasOption(KnownProgOptions.BUILD.getSwitch());
		final boolean isConsoleActivated = options.hasOption(KnownProgOptions.CONSOLE.getSwitch());
		final boolean hasRefreshTimeout = options.hasOption(KnownProgOptions.REFRESH_TIMEOUT.getSwitch());
		final boolean updateBundles = !isRestart && options.hasOption(KnownProgOptions.UPDATE_BUNDLES.getSwitch());
		final boolean createDeploymentPackage = options.hasOption(KnownProgOptions.DEPLOYMENT_PACKAGE.getSwitch());
		final boolean strictMode = options.hasOption(KnownProgOptions.STRICT_MODE.getSwitch());
		final boolean startLevelSwitchSet = options.hasOption(KnownProgOptions.STARTLEVEL.getSwitch());

		if (hasRefreshTimeout) {
			try {
				refreshTimeout = Long.parseLong(options.getOptionValue(KnownProgOptions.REFRESH_TIMEOUT.getSwitch()));
				if (refreshTimeout == 0) {
					// 0 would block Object.wait(...) until notify is called but user wants no
					// refresh timeout: set to 1ms
					refreshTimeout = 1;
				}
			} catch (NumberFormatException e) {
				OgemaLauncher.LOGGER.warning("Error: update timeout argument is no long value!"
						+ " Using the default of " + DEF_REFRESH_TIMEOUT + ".");
			}
		}
		if (isConsoleActivated || isBuildSwitchSet) {
			String port = options.getOptionValue(LauncherConstants.KnownProgOptions.CONSOLE.getSwitch());
			if (port == null && isBuildSwitchSet) {
				// also add all console bundles if we want to build package:
				// port must be != null so we simply add a dummy value as port ...
				port = "1234";
			}
			frameworkConfig.activateOsgiBuiltInConsole(port);
		}
		List<BundleInfo> bundles = frameworkConfig.getBundles();

		BundleResolver resolverChain = ResolverFactory.createResolverChain(options);

		// resolve fwk bundle ...
		if (!resolverChain.resolve(frameworkConfig.getFrameworkBundle())) {
			OgemaLauncher.LOGGER.warning("Error: cannot resolve framework bundle!");
			return RestartType.EXIT;
		}
		if (frameworkConfig instanceof EquinoxConfiguration) {
			fixEquinoxFrameworkBundleLocation(frameworkConfig.getFrameworkBundle());
		}
		final URLClassLoader frameworkClassLoader = FrameworkUtil
				.addFwkBundleToClasspath(frameworkConfig.getFrameworkBundle(), baseClassLoader);
		Set<BundleInfo> bundlesWithoutDuplicates = new LinkedHashSet<>();
		boolean frameworkClean = !FrameworkUtil.frameworkStorageExists(frameworkConfig);
		clean |= frameworkClean;
		boolean installOrUpdateBundles = clean || updateBundles;
		if (installOrUpdateBundles || isBuildSwitchSet || createDeploymentPackage) {
			// resolve bundles
			List<BundleInfo> missingBundles = resolverChain.resolveBundles(bundles);
			logMissingBundles(missingBundles);

			// remove duplicates:
			bundlesWithoutDuplicates.addAll(bundles);
			bundlesWithoutDuplicates.removeAll(missingBundles);
		}
		// bundles are resolved -> if build flag is set start build process
		if (isBuildSwitchSet) {
			bundlesWithoutDuplicates.add(frameworkConfig.getFrameworkBundle());
			try {
				boolean verbose = configuration.getOptions().hasOption(KnownProgOptions.VERBOSE.getSwitch());
				AbstractPackagingUtil packer;
				// = new ZipPackagingUtil();
				if (configuration.getOptions().hasOption(KnownProgOptions.OUTFILE.getSwitch())) {
					String filename = configuration.getOptions().getOptionValue(KnownProgOptions.OUTFILE.getSwitch());
					if (filename.endsWith(".tar")) {
						OgemaLauncher.LOGGER.fine("building tar archive");
						packer = new TarPackagingUtil(filename);
					} else if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
						OgemaLauncher.LOGGER.fine("building compressed tar archive");
						packer = new TgzPackagingUtil(filename);
					} else if (filename.endsWith(".zip")) {
						OgemaLauncher.LOGGER.fine("building zip archive");
						packer = new ZipPackagingUtil(filename);
					} else if (!filename.equalsIgnoreCase("none")) {
						OgemaLauncher.LOGGER.warning(String.format(
								"could not determine archive type from filename ('%s'), building zip archive",
								filename));
						packer = new ZipPackagingUtil(filename);
					}
					else {
						packer = null;
						AbstractPackagingUtil.copyBundlesToBuildLocation(bundlesWithoutDuplicates);
					}
				} else {
					OgemaLauncher.LOGGER.fine("building zip archive");
					packer = new ZipPackagingUtil();
				}
				if (packer != null)
					packer.build(false, bundlesWithoutDuplicates, verbose);
			} catch (IOException | URISyntaxException ex) {
				throw new RuntimeException(ex);
			}
			return RestartType.EXIT;
		}
		if (createDeploymentPackage) {
			boolean tagSnapshots = options.hasOption(KnownProgOptions.TAG_SNAPSHOTS.getSwitch());
			String isDiffStr = configuration.getOptions().getOptionValue(KnownProgOptions.TAG_SNAPSHOTS.getSwitch());
			boolean isDiff = false;
			try {
				isDiff = Boolean.parseBoolean(isDiffStr);
			} catch (Exception ignore) {
			}

			try {
				String result = DeploymentPackageBuilder.build(bundlesWithoutDuplicates, tagSnapshots, isDiff)
						.toString();
				OgemaLauncher.LOGGER.log(Level.INFO, "Deployment package available: " + result);
			} catch (Exception e) {
				OgemaLauncher.LOGGER.log(Level.SEVERE, "Deployment package generation failed", e);
				e.printStackTrace();
			}
			return RestartType.EXIT;
		}

		Map<String, List<BundleInfo>> bundlesToInstall = initBundlesToInstall(bundlesWithoutDuplicates);
		FrameworkUtil.doCleanStart(clean, frameworkConfig, frameworkConfig.getDeleteList());

		if (!FrameworkUtil.createOgemaUserdataPath()) {
			OgemaLauncher.LOGGER.warning("Unable to create OGEMA userdata path!");
		}

		if (!FrameworkUtil.createFrameworkStorage(frameworkConfig)) {
			OgemaLauncher.LOGGER.warning("Unable to create framework storage!");
		}
		// FIXME required?
		FrameworkUtil.fixJavaFxClasspath(frameworkClassLoader);
		boolean enableSecurity = configuration.getOptions().hasOption(KnownProgOptions.SECURITY.getLongSwitch());
		if (enableSecurity) {
			if (System.getSecurityManager() != null) // allows to start multiple framework instances with enabled security (for tests)
				System.setSecurityManager(null);
			frameworkConfig.addFrameworkProperty(Constants.FRAMEWORK_SECURITY, Constants.FRAMEWORK_SECURITY_OSGI);
		}
		frameworkConfig.addFrameworkProperty(ConfigurationConstants.OGEMA_SECURITY, String.valueOf(enableSecurity));
		if (framework == null)
			framework = FrameworkUtil.getFramework(frameworkConfig.getFrameworkProperties(),
					frameworkConfig.getFrameworkBundle(), frameworkClassLoader);
		if (framework == null) // Java 9 quick fix (probably not required any more)
			framework = FrameworkUtil.getFrameworkImpl(frameworkConfig.getFrameworkProperties(),
					frameworkConfig.getFrameworkBundle(), frameworkClassLoader);
		if (framework == null) {
			OgemaLauncher.LOGGER.warning("No OSGi FrameworkFactory on classpath! Exiting ...");
			return RestartType.EXIT;
		}
		try {
			if (shutdownHook == null) {
				installShutdownHook();
			}
			framework.init(); // FileNotFoundException
			// in Felix there is a race condition: framework.init() will asynchronously
			// cause a check whether any packages need to be
			// refreshed; in case we have already reached the update-method by then, this
			// will cause the framework to restart
			// (if there are framework extension bundles to update), and the remaining
			// bundle update s fail -> so better wait here
			// note: it is not possible to wait for the refresh check by means of a listener
			// or anything
			if (updateBundles) {
				Thread.sleep(5000);
			}
			framework.getBundleContext().addFrameworkListener(frameworkListener);
			OgemaLauncher.LOGGER.log(Level.INFO,
					"Actual framework bundle version is " + framework.getBundleContext().getBundle(0).getVersion(),
					(Throwable) null);

			/*
			 * boolean relaunch = true; if (!relaunch) {
			 */
			String security = frameworkConfig.getFrameworkProperties().get(Constants.FRAMEWORK_SECURITY);
			if (clean && security != null && security.equals(Constants.FRAMEWORK_SECURITY_OSGI)) {
				try {
					String policyFile = configuration.getOptions()
							.getOptionValue(KnownProgOptions.SECURITY.getLongSwitch(), "config/ogema.policy");
					OgemaLauncher.LOGGER.log(Level.FINE, "installing policies from file {0}", policyFile);
					installSecurity(framework, policyFile);
				} catch (IOException ex) {
					OgemaLauncher.LOGGER.log(Level.SEVERE, "Launcher could not install permissions: " + ex, ex);
				}
			}
			int highestStartLevel = 10;
			Bundle[] installedBundles = getBundlesFromFramework();
			if (installOrUpdateBundles) {
				Map<String, List<Bundle>> currInstalledBundles = initCurrInstalledBundles(installedBundles);
				installOrUpdateBundles(currInstalledBundles, bundlesToInstall, framework, strictMode);
				if (updateBundles) { // felix: refresh causes reinitialization of the OSGi security
//					framework.getBundleContext().addBundleListener(bundleListener);
					final RestartType restart = refreshBundles();
					if (restart != null) {
						return restart;
					}
				}
				highestStartLevel = startBundles(
						getBundlesWithStartLevels(bundlesToInstall, getBundlesFromFrameworkStable()));
			} else {
				FrameworkWiring fw = framework.adapt(FrameworkWiring.class);
				if (!fw.resolveBundles(Arrays.asList(installedBundles))) {
					OgemaLauncher.LOGGER.log(Level.WARNING, "not all bundles could be resolved");
					if (strictMode)
						throw new RuntimeException("Not all bundles could be resolved");
				}
				highestStartLevel = getStartLevel(installedBundles);
			}
			if (startLevelSwitchSet) {
				String startlevel = configuration.getOptions().getOptionValue(KnownProgOptions.STARTLEVEL.getSwitch());
				try {
					int newStartLevel = Integer.parseInt(startlevel) - 1;
					if (newStartLevel < -1) {
						throw new IllegalArgumentException("Start level must be non-negative");
					}
					highestStartLevel = newStartLevel;
					OgemaLauncher.LOGGER.log(Level.INFO,
							"Start level set via start option: " + (highestStartLevel + 1));
				} catch (Exception e) {
					OgemaLauncher.LOGGER.log(Level.WARNING, "Invalid start level specified: " + startlevel
							+ ". Using default instead: " + (highestStartLevel + 1), e);
				}
			}
			// start framework now -> starting it earlier will lead to non updated
			// bundles at the initial start if clean flag isn't set.
			startFramework();
			setFrameworkStartLevel(highestStartLevel + 1);
			// release all references to bundles
			bundlesToInstall.clear();
			bundlesWithoutDuplicates.clear();
			installedBundles = null;
			resolverChain = null;
			return waitForStop();
		} catch (SecurityException e) {
			OgemaLauncher.LOGGER.log(Level.SEVERE, "Security Exception in OGEMA launcher " + e, e);
		} catch (BundleException e) {
			OgemaLauncher.LOGGER.log(Level.SEVERE, "Bundle Exception in OGEMA launcher " + e, e);
		} catch (InterruptedException ie) {
			OgemaLauncher.LOGGER.log(Level.SEVERE, "OGEMA launcher interrupted" + ie, ie);
			Thread.currentThread().interrupt();
		}
		return RestartType.EXIT;
	}

	private static int getStartLevel(Bundle[] bundles) {
		int highest = 0;
		for (Bundle b : bundles) {
			BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
			int sl = bsl.getStartLevel();
			if (sl > highest)
				highest = sl;
		}
		return highest;
	}

	// FIXME required for Knopflerfish with -ub option, in particular with security
	// enabled. Otherwise the wrong start level is set,
	// because one gets a reduced number of bundles from getBundlesFromFramework
	// immediately after the refresh operation
	private Bundle[] getBundlesFromFrameworkStable() throws InterruptedException {
		Bundle[] bundles;
		Bundle[] check;
		do {
			bundles = getBundlesFromFramework();
			Thread.sleep(1500);
			check = getBundlesFromFramework();
		} while (bundles.length < check.length);
		return bundles;
	}

	private Bundle[] getBundlesFromFramework() throws InterruptedException {
		try {
			return framework.getBundleContext().getBundles();
		} catch (IllegalStateException | NullPointerException e) {
			OgemaLauncher.LOGGER.log(Level.SEVERE, "Unexpected unrecoverable framework exception " + e);
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
	private int startBundles(Bundle[] installedBundles) {
		int maxStartLevel = 1;
		for (Bundle b : installedBundles) {
			String location = b.getLocation();
			if (FrameworkUtil.isFragment(b.adapt(BundleRevision.class)) || location.equalsIgnoreCase("system bundle")) {
				continue;
			}
			int sl = b.adapt(BundleStartLevel.class).getStartLevel();
			if (sl > maxStartLevel) {
				maxStartLevel = sl;
			}

			try {
				startBundle(sl, b, true);
			} catch (BundleException e) {
				OgemaLauncher.LOGGER
						.warning("Failed to start: " + b.getSymbolicName() + ", cause: " + e.getLocalizedMessage());
			}
		}

		return maxStartLevel;
	}

	/**
	 * Compare currently installed bundles with those that should be installed.
	 * Update, install or uninstall if necessary.
	 */
	private static void cmpAndProcessBundles(List<Bundle> currInstalled, List<BundleInfo> toInstall,
			BundleContext context, boolean strictMode) throws FileNotFoundException, BundleException {
		// check if multiple versions of the bundle are installed -> if so choose the
		// one
		// that is the closest to this version and update it:
		ArrayList<BundleInfo> tmpToInstall = new ArrayList<BundleInfo>(toInstall);
		ArrayList<Bundle> tmpCurrInstalled = new ArrayList<Bundle>(currInstalled);
		for (Iterator<Bundle> i = tmpCurrInstalled.iterator(); i.hasNext() && !tmpToInstall.isEmpty();) {
			Bundle installedBundle = i.next();
			BundleInfo closestBundle = FrameworkUtil.getClosestBundle(tmpToInstall, installedBundle);
			if (closestBundle == null) {
				OgemaLauncher.LOGGER.log(Level.WARNING, "Bundle not found: " + installedBundle.getSymbolicName());
				continue;
			}
			if (OgemaLauncher.LOGGER.isLoggable(Level.FINER)) {
				if (installedBundle.getVersion().equals(closestBundle.getVersion())) {
					OgemaLauncher.LOGGER.finer("updating bundle: " + installedBundle.getSymbolicName() + "-"
							+ installedBundle.getVersion());
				} else {
					OgemaLauncher.LOGGER.finer(
							"updating bundle: " + installedBundle.getSymbolicName() + "-" + installedBundle.getVersion()
									+ " to " + closestBundle.getSymbolicName() + "-" + closestBundle.getVersion());
				}
			}
			try {
				updateBundle(installedBundle, closestBundle);
			} catch (BundleException | NullPointerException | FileNotFoundException e) {
				OgemaLauncher.LOGGER.log(Level.WARNING, "Bundle update failed", e);
				if (strictMode)
					throw e;
				continue;
			}
			tmpToInstall.remove(closestBundle);
			i.remove();
		}

		// check if there are more bundles to install
		for (BundleInfo info : tmpToInstall) {
			OgemaLauncher.LOGGER.finer("installing bundle: " + info.getPreferredLocation());
			context.installBundle(info.getPreferredLocation().toString());
		}
	}

	@SuppressWarnings("unused")
	private void dumpBundleInstallInfo(Bundle framework) throws IOException {
		PrintStream out = new PrintStream("felixInstallProperties.txt");

		Map<Integer, Collection<Bundle>> slbundleMap = new TreeMap<Integer, Collection<Bundle>>();
		for (Bundle b : framework.getBundleContext().getBundles()) {
			int sl = b.adapt(BundleStartLevel.class).getStartLevel();
			Collection<Bundle> c = slbundleMap.get(sl);
			if (c == null) {
				c = new ArrayList<>();
				slbundleMap.put(sl, c);
			}
			c.add(b);
		}
		for (Map.Entry<Integer, Collection<Bundle>> e : slbundleMap.entrySet()) {
			// StringBuilder sb = new StringBuilder();
			out.format("felix.auto.start.%d=", e.getKey());
			for (Bundle b : e.getValue()) {
				out.format(" \\\n  %s", b.getLocation());
			}
			out.println();
			out.println();
		}
		out.flush();
		out.close();
	}

	/**
	 * This method puts all bundles into a {@link TreeMap} which is in ascending
	 * order regarding to their appropriate start level.
	 *
	 * @param bundlesToInstall
	 *            - the {@link BundleInfo} map, that contains all bundles that
	 *            should (have) be(en) installed.
	 * @param bundles
	 *            - all currently installed bundles in the framework.
	 * @return This method will return an intersection of those bundles described in
	 *         bundlesToInstalled and bundles with the appropriate start level as
	 *         key.
	 */
	private static TreeMap<Integer, Map<Bundle, Boolean>> getBundlesWithStartLevels(
			Map<String, List<BundleInfo>> bundlesToInstall, Bundle[] bundles) {
		TreeMap<Integer, Map<Bundle, Boolean>> result = new TreeMap<Integer, Map<Bundle, Boolean>>();
		Map<String, List<BundleInfo>> tmpToInstall = new HashMap<String, List<BundleInfo>>(bundlesToInstall);
		for (Bundle b : bundles) {
			List<BundleInfo> biList = tmpToInstall.get(b.getSymbolicName());
			if (biList != null) {
				BundleInfo bi;
				try {
					bi = FrameworkUtil.getClosestBundle(biList, b);
				} catch (Exception e) { // may happen, for instance if trying to load non-bundle jar file; in this case
										// a warning is issued earlier already
					OgemaLauncher.LOGGER.log(Level.WARNING, "Could not determine bundle info for a bundle", e);
					continue;
				}
				Map<Bundle, Boolean> bundleMap = result.get(bi.getStartLevel());
				if (bundleMap == null) {
					bundleMap = new HashMap<>();
				}
				bundleMap.put(b, bi.isStart());

				result.put(bi.getStartLevel(), bundleMap);
			}
		}
		return result;
	}

	private Map<String, List<BundleInfo>> initBundlesToInstall(Set<BundleInfo> bundlesWithoutDuplicates) {
		Map<String, List<BundleInfo>> result = new LinkedHashMap<String, List<BundleInfo>>();

		// check the jar manifest file for symbolic name and version and
		// store the bundle info objects in a map for easier access
		// (key -> symbolic name, value -> list with bundle info [we
		// use a list because there could be the same bundle with varying
		// versions])
		for (BundleInfo bi : bundlesWithoutDuplicates) {
			List<BundleInfo> list = result.get(bi.getSymbolicName());
			if (list == null) {
				list = new ArrayList<BundleInfo>();
			}
			list.add(bi);

			result.put(bi.getSymbolicName(), list);
		}
		return result;
	}

	private Map<String, List<Bundle>> initCurrInstalledBundles(Bundle[] bundles) {
		Map<String, List<Bundle>> result = new HashMap<String, List<Bundle>>();
		for (Bundle b : bundles) {
			String location = b.getLocation();
			if (location.equalsIgnoreCase("system bundle")) {
				continue;
			}

			String symbolicName = b.getSymbolicName();

			List<Bundle> list = result.get(symbolicName);
			if (list == null) {
				list = new ArrayList<Bundle>();
			}
			list.add(b);

			result.put(symbolicName, list);
		}
		return result;
	}

	/**
	 * This method will install or update (if unclean start and the bundle were
	 * already installed) the bundles given in the parameter bundlesToInstall.
	 * Additionally it will create a map of bundles that were installed / updated.
	 * The map contains as key the specified startlevel (from config file) and as
	 * value another map that contains the bundle and a boolean value that is
	 * indicating if the bundle should be started or not.
	 *
	 * @param currInstalledBundles
	 *            - A map that contains all bundles that are currently installed in
	 *            the OSGi framework. Key: symbolic name, value: a list of bundles
	 *            (same bundle with different versions or a list with size 1 if only
	 *            one version of the specific bundle is installed)
	 * @param bundlesToInstall
	 *            - A map that contains all the bundles that should be installed
	 *            (regarding to the config file). Key: symbolic name, value: list of
	 *            bundles (same bundle with different versions or a list with size 1
	 *            if only one version of the specific bundle should be installed)
	 * @param fwkContext
	 *            - The bundle context of the framework bundle.
	 * @return This method will return a list of bundles that should be uninstalled.
	 */
	private List<Bundle> installOrUpdateBundles(Map<String, List<Bundle>> currInstalledBundles,
			Map<String, List<BundleInfo>> bundlesToInstall, final Framework framework, boolean strictMode) {
		Map<String, List<Bundle>> tmpInstalledBundlesNotInConfig = new HashMap<String, List<Bundle>>(
				currInstalledBundles);
		BundleContext fwkContext = framework.getBundleContext();
		for (String symbolicName : bundlesToInstall.keySet()) {
			for (BundleInfo bi : bundlesToInstall.get(symbolicName)) {
				try {
					if (tmpInstalledBundlesNotInConfig.containsKey(symbolicName)) {
						// already installed: check if we have multiple versions installed of this
						// bundle
						// and update the bundle whose version is the closest to the one in the cfg file
						cmpAndProcessBundles(tmpInstalledBundlesNotInConfig.get(symbolicName),
								bundlesToInstall.get(symbolicName), fwkContext, strictMode);

						// is in config -> remove from tmp list ...
						tmpInstalledBundlesNotInConfig.remove(symbolicName);
					} else {
						// not installed yet:
						OgemaLauncher.LOGGER.finer("installing bundle: " + bi.getPreferredLocation());
                        URI preferedUri = bi.getPreferredLocation();
                        String installUrlString = preferedUri.toString();
                        if (configuration.getOptions().hasOption(LauncherConstants.KnownProgOptions.REFERENCE.getLongSwitch())) {
                            if (preferedUri.getScheme().equalsIgnoreCase("file")) {
                                installUrlString = "reference:" + preferedUri.toString();
                                OgemaLauncher.LOGGER.finer("installing bundle as reference: " + installUrlString);
                            }
                        }
						fwkContext.installBundle(installUrlString);

					}
					// ??
				} catch (IllegalStateException e) {
					OgemaLauncher.LOGGER.warning("Error initializing bundle " + bi + ": " + e.getLocalizedMessage());
					if (strictMode)
						throw new RuntimeException("Error while initializing the framework: ", e);
					fwkContext = framework.getBundleContext();
				} catch (FileNotFoundException | BundleException | ArrayIndexOutOfBoundsException e) {
					OgemaLauncher.LOGGER.warning("Error initializing bundle " + bi + ": " + e.getLocalizedMessage());
					if (strictMode)
						throw new RuntimeException("Error while initializing the framework: ", e);
				}
			}
		}

		List<Bundle> installedBundlesNotInConfig = new ArrayList<>();
		// those bundles left in tmpInstalledBundlesNotInConfig are not in config file
		// but installed
		for (List<Bundle> l : tmpInstalledBundlesNotInConfig.values()) {
			for (Bundle b : l) {
				installedBundlesNotInConfig.add(b);
			}
		}

		return installedBundlesNotInConfig;
	}

	private void installSecurity(Framework framework, String policyFileName) throws IOException {
		ServiceReference<ConditionalPermissionAdmin> srCPA = framework.getBundleContext()
				.getServiceReference(ConditionalPermissionAdmin.class);
		if (srCPA != null) {
			ConditionalPermissionAdmin cpa = framework.getBundleContext().getService(srCPA);

			ConditionalPermissionUpdate cpu = cpa.newConditionalPermissionUpdate();
			if (cpu == null) {
				// this will happen with felix, so do not log it as warning
				OgemaLauncher.LOGGER.fine("ConditionalPermissionAdmin not available");
				return;
			}

			List<ConditionalPermissionInfo> perms = cpu.getConditionalPermissionInfos();

			Map<String, ConditionalPermissionInfo> existingPerms = new HashMap<>();
			if (!perms.isEmpty()) {
				OgemaLauncher.LOGGER.fine("existing permissions: ");
				for (ConditionalPermissionInfo cpi : perms) {
					existingPerms.put(cpi.getName(), cpi);
					OgemaLauncher.LOGGER.fine(cpi.toString());
				}
			}

			FileReader fr = new FileReader(policyFileName);
			BufferedReader in = null;
			try {
				in = new BufferedReader(fr);
				StringBuilder policy = new StringBuilder();
				for (String line = in.readLine(); line != null; line = in.readLine()) {
					if (line.trim().startsWith("#")) {
						continue;
					}
					// OGEMA2 user rights proxy stuff
					line = line.replaceFirst("(.*userName)[ \t]*\"([a-zA-Z_0-9]*)\"",
							"$1 \"file:./ogema/users/$2/urp$2\"");
					line = line.replaceFirst("(.*)userName(.*)",
							"$1org.osgi.service.condpermadmin.BundleLocationCondition$2");

					policy.append(line);
					if (line.contains("}")) {
						ConditionalPermissionInfo cpi = cpa.newConditionalPermissionInfo(policy.toString());
						if (existingPerms.containsKey(cpi.getName())) {
							perms.remove(existingPerms.get(cpi.getName()));
							OgemaLauncher.LOGGER.log(Level.FINE, "replacing/updating permission {0}", cpi.getName());
						}
						perms.add(cpi);
						policy = new StringBuilder();
					}
				}
				cpu.commit();
				OgemaLauncher.LOGGER.fine("new permissions: ");
				for (ConditionalPermissionInfo cpi : perms) {
					OgemaLauncher.LOGGER.fine(cpi.toString());
				}
				OgemaLauncher.LOGGER.fine("permissions update complete.");
			} finally {
				fr.close();
				if (in != null) {
					in.close();
				}
			}
		} else {
			OgemaLauncher.LOGGER.fine("could not get a service reference for ConditionalPermissionAdmin"); // not a
																											// problem,
																											// will be
																											// set by
																											// the
																											// framework
		}
	}

	private void logMissingBundles(List<BundleInfo> missingBundles) {
		if (!missingBundles.isEmpty()) {
			OgemaLauncher.LOGGER.warning("Still missing the following bundles:");
			for (BundleInfo bi : missingBundles) {
				OgemaLauncher.LOGGER.warning("\t--> "
						+ (bi.getFileLocation() != null ? bi.getFileLocation().getName() : bi.getMavenCoords()));
			}
		}
	}

	/**
	 * @return 
	 * 		null: no restart required
	 */
	private RestartType refreshBundles() throws InterruptedException {
		synchronized (fwkLock) {
			// since OSGi R4 v4.3 the PackageAdmin is deprecated and the
			// Framework#adapt method should be implemented. To support older
			// OSGi implementations (and additionally support Knopferfish version 4
			// and 5 which is implementing OSGi R4 v4.3 but hasn't implemented
			// that method yet) we will catch the AbstractMethodError from this
			// method and use PackageAdmin instead:
			OgemaLauncher.LOGGER.log(Level.FINE, "Starting bundles refreshment");
			final AtomicReference<FrameworkEvent> refreshEvent = new AtomicReference<FrameworkEvent>(null);
//			boolean frameworkUpdate = false;
			try {
				// refresh all bundles so that after a non clean start recent changes
				// are applied:
				final FrameworkWiring frameworkWiring = (FrameworkWiring) framework.adapt(FrameworkWiring.class);
				final Collection<Bundle> pending = frameworkWiring
						.getDependencyClosure(frameworkWiring.getRemovalPendingBundles());

				if (pending.size() > 0) {
					final boolean containsFramework = containsFrameworkBundle(pending);
					if (containsFramework) {
//						frameworkUpdate = true;
						final CountDownLatch latch = new CountDownLatch(1);
						final long refreshTimeout = 5 * 60 * 1000; // 5 minutes
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									final FrameworkEvent event = framework.waitForStop(refreshTimeout);
									refreshEvent.set(event);
								} catch (InterruptedException e) {
									return;
								} finally {
									latch.countDown();
								}
							}
						}, "launcher-refresh-wait").start();
						
						frameworkWiring.refreshBundles(pending);
						if (!latch.await(refreshTimeout, TimeUnit.MILLISECONDS)) {
							OgemaLauncher.LOGGER.severe("Framework did not recover from package refresh; goodbye");
							return RestartType.EXIT;
						}
					} else {
						final CountDownLatch latch = new CountDownLatch(1);
						frameworkWiring.refreshBundles(pending, new FrameworkListener() {

							@Override
							public void frameworkEvent(FrameworkEvent event) {
								if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
									latch.countDown();
								}
							}

						});
						latch.await(1, TimeUnit.MINUTES);
					}

				}
			} catch (AbstractMethodError e) {
//				frameworkUpdate = true; // no idea, in fact, so to be on the safe side we do wait (anyway, this case
										// shouldn't be relevant any more)
				refreshViaPackageAdmin(framework.getBundleContext(), (Arrays.asList(getBundlesFromFramework()))); 
				Thread.sleep(10000); // ?
			}
			OgemaLauncher.LOGGER.log(Level.FINE, "Refreshing done");
			final FrameworkEvent fe = refreshEvent.get();
			if (fe == null)
				return null;
			switch (fe.getType()) {
			case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
				return RestartType.NEW_VM;
			case 1024: // STOPPED_SYSTEM_REFRESHED
				return RestartType.NEW_CLASSLOADER;
			default:
				checkFrameworkState();
				try {
					fwkLock.wait(4 * refreshTimeout);
				} catch (InterruptedException ignore) {
				}
				return null;
			}
		}
	}

	private final static boolean containsFrameworkBundle(final Collection<Bundle> bundles) {
		if (bundles == null || bundles.isEmpty())
			return false;
		for (Bundle b : bundles) {
			if (b.getBundleId() == 0) {
				return true;
			} else {
				final BundleRevision rev = b.adapt(BundleRevision.class);
				if (rev == null)
					continue;
				if ((rev.getTypes() & BundleRevision.TYPE_FRAGMENT) > 0) {
					// check whether it is really a framework extension bundle
					final List<Requirement> requirements = rev.getRequirements(BundleRevision.HOST_NAMESPACE);
					for (Requirement rq : requirements) {
						final String extension = rq.getDirectives().get("extension");
						if (extension != null && (extension.equals("framework") || extension.equals("bootclasspath"))) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * This method uses an old mechanism of OSGi R4 v4.2 and less to refresh the
	 * packages... it is only used if the {@link Framework#adapt} method isn't
	 * implemented in the framework that we're using (e.g. the newer Knopflerfish
	 * versions still doesn't support the new mechanism).
	 *
	 * @param context
	 * @param c
	 */
	private void refreshViaPackageAdmin(BundleContext context, Collection<Bundle> c) {
		ServiceReference<?> sr = context.getServiceReference(PackageAdmin.class.getName());
		if (sr != null) {
			PackageAdmin pa = (PackageAdmin) context.getService(sr);
			pa.refreshPackages(c.toArray(new Bundle[] {}));
		} else {
			// we can't refresh the packages ... print an error msg
			// and continue:
			OgemaLauncher.LOGGER.warning("Refresh packages failed!" + " Bundles probably weren't updated properly.");
		}
	}

	private void setFrameworkStartLevel(int startLevel) {
		try {
			FrameworkStartLevel fsl = (FrameworkStartLevel) framework.adapt(FrameworkStartLevel.class);
			OgemaLauncher.LOGGER.fine("current startlevel: " + fsl.getStartLevel());
			OgemaLauncher.LOGGER.fine("requesting startlevel " + startLevel);
			fsl.setStartLevel(startLevel, frameworkListener);
			fsl.setInitialBundleStartLevel(startLevel); // set start level for newly installed apps
			startLevelLatch.await(60, TimeUnit.SECONDS);
		} catch (AbstractMethodError e) {
			// not implemented in OSGi R4 v4.2 and less and in some newer
			// Knopflerfish versions
		} catch (InterruptedException ie) {
			OgemaLauncher.LOGGER.log(Level.SEVERE, "Start levels failed, launcher interrupted", ie);
		} catch (IllegalArgumentException e) { // FIXME strange...
			OgemaLauncher.LOGGER.severe(" Illegal start level: " + startLevel);
			e.printStackTrace();
		}

	}

	/**
	 * Start all bundles given in the passed parameter.
	 *
	 * @param bundlesWithStartLevels
	 *            - bundles sorted in ascending order regarding to their start
	 *            level. return This method will return the highest start level one
	 *            or more bundles have.
	 */
	private static int startBundles(TreeMap<Integer, Map<Bundle, Boolean>> bundlesWithStartLevels) {
		for (Integer startLevel : bundlesWithStartLevels.keySet()) {
			Map<Bundle, Boolean> map = bundlesWithStartLevels.get(startLevel);
			for (Bundle bundle : map.keySet()) {
				// only non fragment bundles can be started:
				if (FrameworkUtil.isFragment(bundle.adapt(BundleRevision.class))) {
					continue;
				}

				if (bundle.getState() == Bundle.UNINSTALLED) {
					OgemaLauncher.LOGGER.warning("bundle " + bundle + " is in state UNINSTALLED");
					continue;
				}

				try {
					OgemaLauncher.LOGGER
							.fine(String.format("setting bundle start level for '%s' to %d", bundle, startLevel));
					startBundle(startLevel, bundle, map.get(bundle));
				} catch (BundleException e) {
					OgemaLauncher.LOGGER.warning(
							"Failed to start: " + bundle.getSymbolicName() + ", cause: " + e.getLocalizedMessage());
				}

			}
		}
		return bundlesWithStartLevels.isEmpty() ? 0 : bundlesWithStartLevels.lastKey();
	}

	private static void startBundle(Integer startLevel, Bundle bundle, boolean start) throws BundleException {
		try {
			BundleStartLevel bsl = (BundleStartLevel) bundle.adapt(BundleStartLevel.class);
			bsl.setStartLevel(startLevel);
		} catch (AbstractMethodError | IllegalArgumentException e) {
			// not implemented in OSGi R4 v4.2 and less and in some newer
			// Knopflerfish versions
			OgemaLauncher.LOGGER
					.severe(" Error setting start level " + startLevel + " for " + bundle.getSymbolicName() + ": " + e);
		}
		if (start) {
			bundle.start();
		}
	}

	private void startFramework() throws BundleException {
		framework.start(); // throws IllegalStateException "Bundle in unexpected state"
		if (framework.getState() != Bundle.ACTIVE) {
			try {
				startLatch.await(60, TimeUnit.SECONDS);
				OgemaLauncher.LOGGER.fine("framework started");
			} catch (InterruptedException ex) {
				OgemaLauncher.LOGGER.log(Level.SEVERE, "Framework start failed", ex);
			}
		}
	}

	@SuppressWarnings("unused")
	private void uninstallBundles(List<Bundle> installedBundlesNotInConfig) throws BundleException {
		for (Bundle b : installedBundlesNotInConfig) {
			OgemaLauncher.LOGGER.fine("uninstalling bundle: " + b.getLocation());
			b.uninstall();
		}
	}

	private static void updateBundle(Bundle oldBundle, BundleInfo newBundle)
			throws BundleException, FileNotFoundException {
		InputStream in = null;
		try {
			in = newBundle.getPreferredLocation().toURL().openStream();
		} catch (IOException ignore) {
		}

		if (in == null) {
			in = new FileInputStream(new File(newBundle.getPreferredLocation()));
		}
		oldBundle.update(in);
	}

	private RestartType waitForStop() throws InterruptedException {
		outer: while (true) {
			final FrameworkEvent stopReason = framework.waitForStop(0);
			OgemaLauncher.LOGGER.fine("framework stopped: " + stopReason.getType());
			// the latter case is STOPPED_SYSTEM_REFRESHED, introduced in a recent OSGi
			// version
			switch (stopReason.getType()) {
			case FrameworkEvent.STOPPED_UPDATE:
				final long maxWait = System.currentTimeMillis() + 10000;
				while (framework.getState() != Bundle.ACTIVE && framework.getState() != Bundle.STARTING
						&& System.currentTimeMillis() < maxWait) {
					Thread.sleep(500);
				}
				// if framework doesn't restart within 10s, try to force it
				switch (framework.getState()) {
				case Bundle.STOPPING:
					// wait for stop
					final FrameworkEvent ev = framework.waitForStop(60000);
					if (ev.getType() != FrameworkEvent.STOPPED) {
						OgemaLauncher.LOGGER.severe("Could not stop framework; exiting");
						return RestartType.EXIT;
					}
					// fallthrough
				case Bundle.RESOLVED:
				case Bundle.INSTALLED:
					return RestartType.RESTART; // restart framework
				default:
					continue outer; // framework has restarted itself, continue waiting for next stop operation
				}
			case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
				return RestartType.NEW_VM;
			case 1024: // new FrameworkEvent in R7: STOPPED_SYSTEM_REFRESHED -> system requires a fresh
						// class loader to restart,
				return RestartType.NEW_CLASSLOADER;
			case FrameworkEvent.STOPPED:
				return RestartType.EXIT;
			default:
				break; // continue
			}
		}
	}

	private void installShutdownHook() {
		final long timeout = 5000;

		this.shutdownHook = new Thread() {
			@Override
			public void run() {
				final Framework framework = OgemaFramework.this.framework;
				if (framework == null)
					return;
				System.out.println("Shutdown requested, stopping framework.");
				try {
					framework.stop();
					System.out.printf("waiting for shutdown (timeout %dms) ...%n", timeout);
					framework.waitForStop(timeout);
				} catch (BundleException | InterruptedException e) {
					System.err.println("OSGi shutdown failed: " + e.getMessage());
					OgemaLauncher.LOGGER.log(Level.SEVERE, "clean shutdown failed", e);
				}
			}
		};
		OgemaLauncher.LOGGER.finest("shutdown hook installed");
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private final static Version EQUINOX_FIX_VERSION = new Version(3, 12, 0, "v20170512-1932");

	// Equinox in elder version (<=3.12.0.v20170512-1932) cannot deal with a
	// relative framework bundle location... hence we need a workaround for the Equinox
	// framework bundle to set its location to an absolute path; must be done before adding the
	// bundle to the classpath
	private final static void fixEquinoxFrameworkBundleLocation(final BundleInfo bi) {
		if (bi.getVersion().compareTo(EQUINOX_FIX_VERSION) > 0)
			return;
		final URI location = bi.getPreferredLocation();
		if (location.getScheme().equals("file")) {
			try {
				File file = new File(bi.getPreferredLocation().getPath());
				if (!file.isAbsolute())
					file = new File(file.getAbsolutePath());
				bi.setPreferredLocation(file.toURI());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

	