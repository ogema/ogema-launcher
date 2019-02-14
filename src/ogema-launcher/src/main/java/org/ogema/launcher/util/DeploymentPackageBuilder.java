package org.ogema.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.OgemaLauncher;
import org.osgi.framework.Constants;

public class DeploymentPackageBuilder {
	
	private static final Map<String, ?> PROPS = Collections.singletonMap("create", "true");
	private static final String LAUNCHER_TEMP_DIR = "data/launcherTemp";
	
	public static Path build(Set<BundleInfo> bundles, boolean tagSnapshots, boolean isDiff) throws IOException {
		Path dest = getNewFile();
		final URI uri = URI.create("jar:" + dest.toUri());
		String dateString = null;
		Path tempFolder = null;
		if (tagSnapshots) {
			Date d = new Date(System.currentTimeMillis());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			dateString = sdf.format(d);
			Path p = Paths.get(LAUNCHER_TEMP_DIR);
			tempFolder = Files.createDirectories(p);
		}
		try (FileSystem jarfs = FileSystems.newFileSystem(uri, PROPS)) {
			final Manifest manifest = new Manifest();
		    Attributes mainAttributes = manifest.getMainAttributes();
		    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		    mainAttributes.putValue("DeploymentPackage-SymbolicName", "gateway"); // XXX?
		    mainAttributes.putValue("DeploymentPackage-Version", "0.0.0"); // XXX?
		    Map<String,Attributes> entries = manifest.getEntries();
		    for (BundleInfo bi : bundles) {
		    	Attributes att = new Attributes(2);
		    	att.putValue(Constants.BUNDLE_SYMBOLICNAME, bi.getSymbolicName());
		    	String version = bi.getVersion().toString();
		    	if (tagSnapshots) 
		    		version = adaptVersion(version, dateString);
	    		att.putValue(Constants.BUNDLE_VERSION, version);
		    	entries.put(bi.getSymbolicName() + "-" + version + ".jar", att);
		    }
		    Files.createDirectory(jarfs.getPath("META-INF/"));
		    try (OutputStream os = Files.newOutputStream(jarfs.getPath("META-INF/MANIFEST.MF"))) {
		        manifest.write(os);
		    }
		    for (BundleInfo bi : bundles) {
		    	URI loc = bi.getPreferredLocation();
		    	String version = bi.getVersion().toString();
		    	InputStream is;
		    	if (tagSnapshots && bi.getVersion().getQualifier().equals("SNAPSHOT")) {
		    		is = tagSnapshot(loc, tempFolder, dateString);
		    		version = adaptVersion(version, dateString);
		    	}
		    	else
		    		is = loc.toURL().openStream();
		    	try {
		    		Files.copy(is, jarfs.getPath(bi.getSymbolicName() + "-" + version + ".jar"));
		    	} finally {
		    		try {
		    			is.close();
		    		} catch (Exception ignore) {}
		    	}
		    }
		    return jarfs.getPath(".");
		}
	}
	
	private static final String adaptVersion(String version, String qualifier) {
		if (version.endsWith("SNAPSHOT"))
			version += "-" + qualifier;
		return version;
	}
	
	public static InputStream tagSnapshot(URI bundleURI, Path tempFolder, String qualifier) throws IOException {
		Path temp = getNextTempFile(tempFolder);	
		Files.copy(bundleURI.toURL().openStream(), temp);
		// manipulate MANIFEST and pom.xml
		return tagSnapshot2(temp, qualifier);
	}
	
	private static final InputStream tagSnapshot2(Path tempFile, String qualifier) throws IOException {
		URI uri = tempFile.toUri();
		Path temp2 = Paths.get(tempFile.toString()+ "_modified");
		try (FileSystem jarfs = FileSystems.newFileSystem(URI.create("jar:" + temp2.toUri()), PROPS)) {
			Files.createDirectories(jarfs.getPath("META-INF"));
			Manifest manifest;
			// TODO write manifest first, then copy other files
			// Key: BundleSymbolic name
			try (InputStream is = uri.toURL().openStream(); ZipInputStream zis = new ZipInputStream(is)) {
				ZipEntry entry; // must be the manifest entry
				while ((entry = zis.getNextEntry()) != null) {
					try {
						String entryName = entry.getName();
						char z = entryName.charAt(0);
						if (z == '/' || z=='\\')
							entryName = entryName.substring(1);
						Path entryPath = jarfs.getPath(entryName);
						if (entry.isDirectory() || entryName.endsWith("/") || entryName.endsWith("\\")) {
							Files.createDirectories(entryPath);
							continue;
						}
						if (entryName.equals("META-INF/MANIFEST.MF")) {
							manifest = new Manifest(zis);
							Attributes main = manifest.getMainAttributes();
							String symbName = main.getValue(Constants.BUNDLE_SYMBOLICNAME);
							String version = main.getValue(Constants.BUNDLE_VERSION);
							if (symbName == null || version == null) {
								OgemaLauncher.LOGGER.log(Level.SEVERE,"Bundle manifest does not provide version and/or symbolic name: "+ symbName + "-" + version);
								return null;
							}
							version = adaptVersion(version, qualifier);
							main.putValue(Constants.BUNDLE_VERSION, version);
						    try (OutputStream os = Files.newOutputStream(jarfs.getPath("META-INF/MANIFEST.MF"))) {
						        manifest.write(os);
						    }
						}
						else
							Files.copy(zis,entryPath);
					} catch (Exception e) {
						OgemaLauncher.LOGGER.log(Level.SEVERE,"Error extracting zip file ",e);
						e.printStackTrace();
					}
				}
			}
			try {
				Files.delete(tempFile);
			} catch (IOException e) {
				OgemaLauncher.LOGGER.log(Level.WARNING, "Could not delete temp file " + tempFile.toString(),e);
			}
		} 
		return new TempFileInputStream(temp2);	
	}
	
	private static final Path getNextTempFile(Path path) {
		int cnt = 0;
		Path temp;
		while (true) {
			temp = path.resolve("temp" + cnt++);		
			if (!Files.exists(temp))
				return temp;
		}
	}

	private static Path getNewFile() {
		int cnt = 0;
		while (true) {
			Path p = Paths.get(cnt++ + ".0.0");
			if (!Files.exists(p))
				return p;
		}
	}
	
	private static final class TempFileInputStream extends InputStream {
		
		private final Path file;
		private final InputStream is;
		
		public TempFileInputStream(Path path) throws MalformedURLException, IOException {
			this.file = path;
			this.is = path.toUri().toURL().openStream();
		}

		@Override
		public void close() throws IOException {
			try {
				is.close();
			} catch (Exception ignore) {}
			try {
				Files.delete(file);
			} catch (Exception ignore) {}
		}
		
		@Override
		public int read() throws IOException {
			return is.read();
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return is.read(b, off, len);
		}
		
		@Override
		public int read(byte[] b) throws IOException {
			return is.read(b);
		}
		
		@Override
		public synchronized void reset() throws IOException {
			is.reset();
		}
		
		@Override
		public long skip(long n) throws IOException {
			return is.skip(n);
		}
		
		@Override
		public boolean markSupported() {
			return is.markSupported();
		}
		
		@Override
		public synchronized void mark(int readlimit) {
			is.mark(readlimit);
		}
		
		@Override
		public int available() throws IOException {
			return is.available();
		}
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			try {
				return new TempFileInputStream(file);
			} catch (IOException e) {
				throw new CloneNotSupportedException("" + e);
			}
		}
		

		
	}
	
	
}
