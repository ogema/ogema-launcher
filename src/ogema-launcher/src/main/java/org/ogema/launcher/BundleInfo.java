/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher;

import java.io.File;
import java.net.URI;

import org.ogema.launcher.resolver.BundleFileResolver;
import org.ogema.launcher.resolver.WorkspaceBundleResolver;
import org.osgi.framework.Version;

public class BundleInfo implements Comparable<BundleInfo> {
	/**
	 * The symbolic name of the bundle. Needed by the {@link WorkspaceBundleResolver}
	 * and the update process.
	 */
	private String symbolicName;
	// location where the bundle will be copied to if the build flag is set
	private File buildLocation;
	/**
	 * The version of the bundle. Needed by the {@link WorkspaceBundleResolver}
	 * and the update process.
	 */
	private Version version;
	// maven coords -> groupId:artifactId:version
	private String mavenCoords = "";
	// maven artifact location
	private URI mavenArtifactLocation;
	// OSGi start level
	private int startLevel = 4;
	// flag that determines if the bundle should be started
	private boolean start = true;
	// if the bundle is available as project in the workspace we'll
	// prefer starting it from there -> hot code replacement!
	private File workspaceLocation;
	/** Location where the bundle file can be found. */
	private File fileLocation;
	
	/**
	 * The file name of the bundle. It is used by the {@link BundleFileResolver} to
	 * find the bundle (combined with {@link #binDir}. If the file name is not given
	 * (e.g. within a xml config file) then it will be derived from the maven
	 * coordinates ({@link #mavenCoords}).
	 */
	private String fileName;
	/** Relative or absolute path to the bundle file */
	private String binDir = "bin";
	
	private boolean resolved = false;
	
	private URI preferredLocation;
	
	public BundleInfo() {}
	
	public BundleInfo(String symbolicName, Version version) {
		this.symbolicName = symbolicName;
		this.version = version;
	}
	
	public BundleInfo(String groupId, String artifactId, String version, String binDir) {
		this.mavenCoords = groupId + ":" + artifactId + ":" + version;
		this.version = new Version(version);
		this.binDir = binDir;
	}
	
	public BundleInfo(String groupId, String artifactId, Version version, String binDir) {
		this.mavenCoords = groupId + ":" + artifactId + ":" + version.toString();
		this.version = version;
		this.binDir = binDir;
	}
	
	public String getSymbolicName() {
		return symbolicName;
	}

	public File getBuildLocation() {
		return buildLocation;
	}

	public void setBuildLocation(File location) {
		this.buildLocation = location;
	}

	public Version getVersion() {
		return version;
	}

	public String getMavenCoords() {
		return mavenCoords;
	}

	public void setMavenCoords(String mavenCoords) {
		this.mavenCoords = mavenCoords;
	}

	public URI getMavenArtifactLocation() {
		return mavenArtifactLocation;
	}

	public void setMavenArtifactLocation(URI uri) {
		this.mavenArtifactLocation = uri;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public void setStartLevel(int startLevel) {
		this.startLevel = startLevel;
	}

	public boolean isStart() {
		return start;
	}

	public void setStart(boolean start) {
		this.start = start;
	}

	public File getWorkspaceLocation() {
		return workspaceLocation;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public void setWorkspaceLocation(File workspaceLocation) {
		this.workspaceLocation = workspaceLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((binDir == null) ? 0 : binDir.hashCode());
		result = prime * result
				+ ((buildLocation == null) ? 0 : buildLocation.hashCode());
		result = prime * result
				+ ((fileLocation == null) ? 0 : fileLocation.hashCode());
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result	+ ((mavenArtifactLocation == null) ?
				0 : mavenArtifactLocation.hashCode());
		result = prime * result	+ ((mavenCoords == null) ? 0 : mavenCoords.hashCode());
		result = prime * result	+ ((preferredLocation == null) ?
				0 : preferredLocation.hashCode());
		result = prime * result + (resolved ? 1231 : 1237);
		result = prime * result + (start ? 1231 : 1237);
		result = prime * result + startLevel;
		result = prime * result
				+ ((symbolicName == null) ? 0 : symbolicName.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result	+ ((workspaceLocation == null) ?
				0 : workspaceLocation.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BundleInfo other = (BundleInfo) obj;
		if (binDir == null) {
			if (other.binDir != null)
				return false;
		} else if (!binDir.equals(other.binDir))
			return false;
		if (buildLocation == null) {
			if (other.buildLocation != null)
				return false;
		} else if (!buildLocation.equals(other.buildLocation))
			return false;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (mavenArtifactLocation == null) {
			if (other.mavenArtifactLocation != null)
				return false;
		} else if (!mavenArtifactLocation.equals(other.mavenArtifactLocation))
			return false;
		if (mavenCoords == null) {
			if (other.mavenCoords != null)
				return false;
		} else if (!mavenCoords.equals(other.mavenCoords))
			return false;
		if (preferredLocation == null) {
			if (other.preferredLocation != null)
				return false;
		} else if (!preferredLocation.equals(other.preferredLocation))
			return false;
		if (resolved != other.resolved)
			return false;
		if (start != other.start)
			return false;
		if (startLevel != other.startLevel)
			return false;
		if (symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!symbolicName.equals(other.symbolicName))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (workspaceLocation == null) {
			if (other.workspaceLocation != null)
				return false;
		} else if (!workspaceLocation.equals(other.workspaceLocation))
			return false;
		return true;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public boolean isResolved() {
		return resolved;
	}

	/**
	 * 
	 * @return The preferred location: maven artifact file location or
	 * workspace location or relative/absolute location given in cfg file.
	 */
	public URI getPreferredLocation() {
		return preferredLocation;
	}

	public void setPreferredLocation(URI preferredLocation) {
		this.preferredLocation = preferredLocation;
	}

	public File getFileLocation() {
		if(fileLocation == null) {
			// try to init file location ...
			if(fileName != null && !fileName.isEmpty()) {
				fileLocation = new File(binDir + "/" + fileName);
			} else if(mavenCoords != null && !mavenCoords.isEmpty()) {
				// else use maven coordinates
				int colonIndex = mavenCoords.indexOf(":");
				String groupId = mavenCoords.substring(0, colonIndex);
				int secondColonIndex = mavenCoords.substring(colonIndex+1).indexOf(":");
				String artifactId = mavenCoords.substring(colonIndex+1, colonIndex+1 + secondColonIndex);
				String mvnVersion = mavenCoords.substring(colonIndex+1 + secondColonIndex+1);
				
				fileLocation = new File(binDir + "/" + groupId + "." + artifactId + "-" + mvnVersion + ".jar");
			} else if(symbolicName != null && version != null) {
				// use symbolic name + version to define unique file name:
				String versionString = version.toString();
				if(versionString.contains(".SNAPSHOT")) {
					versionString = versionString.replace(".SNAPSHOT", "-SNAPSHOT");
				}
				fileLocation = new File(binDir + "/" +
						symbolicName + "-" + versionString + ".jar");
			} 
		}
		
		return fileLocation;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getBinDir() {
		return binDir;
	}

	public void setBinDir(String binDir) {
		this.binDir = binDir;
	}

	@Override
	public int compareTo(BundleInfo o) {
		return this.getSymbolicName().compareTo(o.getSymbolicName());
	}
	
    @Override
	public String toString() {
		return "Name: " + symbolicName + ", Version: " + version + ", start level: " +
				startLevel + ", start: " + start + ", resolved: " + resolved +
                ", preferredLocation: " + preferredLocation + ", buildLocation: " + buildLocation +
                ", maven: " + mavenCoords + ", file location: " + fileLocation; 
	}
}