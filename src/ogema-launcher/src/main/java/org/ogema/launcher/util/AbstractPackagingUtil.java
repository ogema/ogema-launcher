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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.util.FileUtils;
import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.LauncherConstants;
import org.ogema.launcher.OgemaLauncher;

import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveOutputStream;


/**
 * Utility class for building an OGEMA distribution archive. Can be customized
 * via a properties file ("build.properties") placed in the launcher working
 * directory, see the constants defined in this class for details.
 *
 * @author jlapp
 * @author mperez
 */
public abstract class AbstractPackagingUtil {

	/**
	 * Build property: Filename regex patterns to exclude from archive. Match
	 * only file names, not entire paths. If a directory matches, the directory
         * and all its contents will be excluded.
	 */
	public static String EXCLUDE_PATTERNS = "exclude";
	/**
	 * Build property: Prefix used for files in the archive, default: "ogema/".
	 */
	public static String ZIP_PREFIX = "prefix";
	/**
	 * Build property: Name of launcher jar, default: leave as is.
	 */
	public static String LAUNCHER_NAME = "launcher";
	/**
	 * Build property: Base name of the archive, default: "ogema".
	 */
	public static String ARCHIVE_NAME = "archive";
	/**
	 * Build property: Libraries required for the launcher without maven/aether,
	 * used with -buildoffline switch (-build switch reads all dependencies from
	 * the launcher's Class-Path manifest entry). Use paths relative to launcher
	 * jar (as in manifest).
	 */
	public static String OFFLINE_LIBS = "offline_libs";
	private final Properties props;

	// TODO exclude javadoc and sources ...
	private static final String LAUNCHER_REGEX = "(?=(.*ogema-launcher.*\\.jar))(^(?!.*(javadoc|sources)).*$)";
    
    protected final String outputFilename;
    
	public AbstractPackagingUtil(String filename) {
		Properties defaults = new Properties();
		try {
			defaults.load(AbstractPackagingUtil.class.getResourceAsStream("/org/ogema/launcher/props/build.properties"));
		} catch (IOException ioex) {
			throw new RuntimeException(ioex);
		}
		props = new Properties(defaults);
        this.outputFilename = filename;
	}
    
    public AbstractPackagingUtil() {
        this(null);
    }

	/**
	 * Build the archive according to properties.
	 *
	 * @param offlinebuild include only libraries set in the
	 * {@link #OFFLINE_LIBS} property.
	 * @param verbose 
	 * @throws URISyntaxException
	 */
	public void build(boolean offlinebuild, Set<BundleInfo> bundles, boolean verbose)
			throws IOException, URISyntaxException {
		// FIXME: only zip those files in bin that are also in config ... for now
		// all are added ...
		copyBundlesToBuildLocation(bundles);

		File propsFile = new File("build.properties");
		if (propsFile.exists()) {
			props.load(new FileInputStream(propsFile));
		}

		final File base = new File(".");
		Collection<File> files = collectFiles(base);

		OgemaLauncher.LOGGER.fine(files.toString());

		String baseString = base.getAbsolutePath();
		for (File f : files) {
			OgemaLauncher.LOGGER.fine(f.getAbsolutePath().substring(baseString.length() + 1));
		}

		final String zipPrefix = props.getProperty(ZIP_PREFIX);

        String filename = outputFilename != null ? outputFilename : getFilename(props.getProperty(ARCHIVE_NAME));
		File archive = new File(filename).getCanonicalFile();
        if (archive.exists() && archive.isFile()) {
            archive.delete();
        }
        
		FileOutputStream fos = null;
		ArchiveOutputStream os = null;
		try {
			fos = new FileOutputStream(archive);
			os = createStream(fos);

			File launcherFileRundir = null;
			for (File f : files) {
				// check if launcher already is in rundir
				if(f.getName().matches(LAUNCHER_REGEX)) {
					// don't add to zip yet
					launcherFileRundir = f;
					continue;
				}
                if (f.getCanonicalFile().equals(archive)) {
                    continue;
                }
				String relname = f.getAbsolutePath().substring(baseString.length() + 1);
				writeEntry(os, zipPrefix + relname, f);
			}

			// add launcher and launcher libs
			String path = AbstractPackagingUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			OgemaLauncher.LOGGER.fine(decodedPath);
			File launcherFile = new File(path);
			
			// check if launcher already existed in rundir and if we've started that one:
			if(launcherFileRundir != null && launcherFile.equals(launcherFileRundir)) {
				// we're running the launcher file in rundir ... simply add that one:
				launcherFile = launcherFileRundir;
			} else {
				// check if we're running from workspace within an IDE
				if(launcherFile.isDirectory()) {
					// launcher were started from target/classes directory
					// -> create launcher jar in target with mvn install:
					OgemaLauncher.LOGGER.fine("Launcher were started from target/classes "
							+ "(probably within an IDE): trying to build the jar - mvn install ...");
					MavenCli cli = new MavenCli();
					String projectDir = launcherFile.getParentFile().getParent();

					// ignore output from mvn clean and install here
					
					PrintStream outStream, errStream;
					if(verbose) {
						outStream = System.out;
						errStream = System.err;
					} else {
						// don't print output from MavenCli:
						OutputStream devnull = new OutputStream() {

                            @Override
                            public void write(int b) throws IOException {}
                        };
						outStream = new PrintStream(devnull);
						errStream = new PrintStream(devnull);
					}
					
					int ret = cli.doMain(new String[]{"clean", "install"}, projectDir, outStream, errStream);
					if(ret != 0) {
						// error occured ...
						OgemaLauncher.LOGGER.warning("Error while trying to build ogema-launcher "
								+ "via mvn install ... run with verbose flag (-v) for further information.");
						if(launcherFileRundir != null) {
							// using the launcher that already exists in rundir:
							OgemaLauncher.LOGGER.warning("Will use the launcher that already "
									+ "exists in the rundir ...");
							launcherFile = launcherFileRundir;
						} else {
							OgemaLauncher.LOGGER.warning("No launcher available for build ... "
									+ "please build manually and try to build again.");
							archive.deleteOnExit();
							return;
						}
					} else {
						// build finished ... get launcher jar
						File[] launcher = launcherFile.getParentFile().listFiles(new FilenameFilter() {
							@Override
							public boolean accept(File dir, String name) {
								return name.matches(LAUNCHER_REGEX);
							}
						});
						if(launcher.length > 1) {
							OgemaLauncher.LOGGER.warning("Found multiple launcher files ... using first");
							launcherFile = launcher[0];
						} else if(launcher.length == 0) {
							OgemaLauncher.LOGGER.warning("No launcher file found!");
							if(launcherFileRundir != null) {
								OgemaLauncher.LOGGER.warning("Using launcher in rundir ...");
								launcherFile = launcherFileRundir;
							} else {
								OgemaLauncher.LOGGER.warning("Exiting ...");
								return;
							}
						} else {
							launcherFile = launcher[0];
						}
					}
				}
			}
			String relname = launcherFile.getName();
                        String launcherName = relname;
                        if (props.containsKey(LAUNCHER_NAME)){
                            launcherName = props.get(LAUNCHER_NAME).toString();
                        }
			writeEntry(os, zipPrefix + launcherName, launcherFile);
		} finally {
			if(os != null) {
				os.close();
			}
			// fos should be already closed by zos.close() but if ZipOutputStream
			// constructor has thrown an exception it'll be still opened:
			if(fos != null) {
				fos.close();
			}
		}
		OgemaLauncher.LOGGER.fine("built archive " + archive.getAbsolutePath());
	}

	public static void copyBundlesToBuildLocation(Set<BundleInfo> bundles)
			throws IOException, URISyntaxException {
		for(Iterator<BundleInfo> iter = bundles.iterator(); iter.hasNext();) {
			BundleInfo bi = iter.next();
			if(!bi.isResolved()) {
				iter.remove();
			}
			
			File fileLoc = bi.getFileLocation();
			if(fileLoc != null) {
				// we have to set the directory where it should be copied if
				// we're building a zip package
				if(fileLoc.isAbsolute()
						&& !fileLoc.getAbsolutePath()
						.contains(new File(".").getAbsolutePath())) {
					// if we have an absolute path that is outside of our run
					// directory then we will set the build location into our
					// run directory so that our zip package will contain all bundles
					bi.setBuildLocation(new File("bin/" + fileLoc.getName()));
				} else {
					// otherwise set the build directory path to the same path as
					// file loc
					bi.setBuildLocation(fileLoc);
				}

				File target = bi.getBuildLocation();
				URI preferredLocation = bi.getPreferredLocation();
				if(preferredLocation.getScheme() != null
						&& preferredLocation.getScheme().equals("reference")) {
					// bundle that should be added to zip is in workspace -> build jar
					// remove "reference:" from uri because it is OSGi specific
					OgemaLauncher.LOGGER.finer("creating jar at " +
							target.getAbsolutePath());
					String adjustedUri =
							preferredLocation.toString().replace("reference:", "");
					preferredLocation = new URI(adjustedUri);
					JarFileBuilder.buildJar(new File(preferredLocation), target);
				} else {
					// preferred location references a jar file -> copy it to build location
					OgemaLauncher.LOGGER.finer("copying maven artifact to " +
							target.getAbsolutePath());
					if(preferredLocation.isOpaque()) {
						// file util will throw an exception if uri is opaque:
						preferredLocation = new URL(new File(".").toURI().toURL(), preferredLocation.toString()).toURI();
					}
					FileUtils.copyFile(new File(preferredLocation), target);
				}

			} // else bundle is not resolved but user decided to build anyway ...
		}
	}
    
    protected abstract ArchiveOutputStream createStream(OutputStream os) throws IOException;

	protected abstract void writeEntry(ArchiveOutputStream os, String filename, File file) throws IOException;
    
    protected abstract String getFilename(String basename);
    
    protected boolean isExecutable(File file){
        String filename = file.getName();
        return file.canExecute() || filename.endsWith("sh") ||
                filename.endsWith("bat") || filename.endsWith("cmd");
    }

	public Collection<File> collectFiles(File base) {
		Collection<File> v = new ArrayList<>(250);

		Deque<File> subdirStack = new ArrayDeque<>();

		/* match file names (NOT entire paths!) against these patterns: */
		List<Pattern> dontIncludePatterns = new ArrayList<>();
		for (String p : props.getProperty(EXCLUDE_PATTERNS).split("\\s+")) {
			dontIncludePatterns.add(Pattern.compile(p));
		}
		dontIncludePatterns.add(Pattern.compile(getFilename(props.getProperty(ARCHIVE_NAME, "ogema"))));
		dontIncludePatterns.add(Pattern.compile(LauncherConstants.LOCK_FILE));

		subdirStack.push(base);
		while (!subdirStack.isEmpty()) {
			File dir = subdirStack.pop();

			for (File f : dir.listFiles()) {
				boolean include = true;
				for (Pattern p : dontIncludePatterns) {
					if (p.matcher(f.getName()).matches()) {
						include = false;
						break;
					}
				}
				if (include) {
					if (f.isDirectory()) {
						subdirStack.push(f);
					} else {
						v.add(f);
					}
				}
			}
		}

		return v;
	}
}
