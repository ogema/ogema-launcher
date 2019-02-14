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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.OgemaLauncher;

/**
 * Searches maven artifacts in local or remote repositories. Has support for
 * simple HTTP authentication configured in user's maven settings.
 *
 * @author jlapp
 * edited by mperez
 */
public class MavenResolver extends BundleResolver {
    
    /** System property ({@value}) holding the location of an optional maven repository
     configuration file (default={@value #REPOSITORY_CONFIG_DEFAULT}) */
    public static final String REPOSITORY_CONFIG = "ogema.launcher.repositories";
    public static final String REPOSITORY_CONFIG_DEFAULT = "launcher-repositories.properties";
    
	private static final String DEFAULT_RELEASE_UPDATE_POLICY = RepositoryPolicy.UPDATE_POLICY_NEVER;
	private static final String DEFAULT_SNAPSHOT_UPDATE_POLICY = RepositoryPolicy.UPDATE_POLICY_DAILY;
	private static final String DEFAULT_CHECKSUM_POLICY = RepositoryPolicy.CHECKSUM_POLICY_FAIL;
	
	private static final RepositoryPolicy DEF_DISABLED_POLICY = new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_NEVER, null);
	private static final RepositoryPolicy DEF_RELEASE_POLICY = new RepositoryPolicy(true, DEFAULT_RELEASE_UPDATE_POLICY, DEFAULT_CHECKSUM_POLICY);
	private static final RepositoryPolicy DEF_SNAPSHOT_POLICY = new RepositoryPolicy(true, DEFAULT_SNAPSHOT_UPDATE_POLICY, DEFAULT_CHECKSUM_POLICY);
	
	// TODO: command line switch for remoteFirst?
	protected RepositorySystem _repoSys;
	protected RepositorySystemSession _session;
	protected Settings _mavenSettings;
	protected List<RemoteRepository> _remoteRepos = new ArrayList<RemoteRepository>();

	private boolean _offline = false;
	private boolean _mavenRemoteFirst = true; //check remote repositories before local?
    private String _repositoryConfig;

	protected MavenResolver(boolean offline, String repositoryConfig) {
		this._offline = offline;
        _repositoryConfig = repositoryConfig == null ?
            System.getProperty(REPOSITORY_CONFIG, REPOSITORY_CONFIG_DEFAULT) :
                repositoryConfig;

		String mavenHome = System.getenv("M2_HOME");
		String user_home = System.getProperty("user.home");
		File mavenUserSettingsFile = new File(user_home, ".m2/settings.xml");
		File mavenGlobalSettingsFile = null;
		if (mavenHome != null) {
			mavenGlobalSettingsFile = new File(mavenHome, "conf/settings.xml");
		} else {
			OgemaLauncher.LOGGER.warning(this.getClass().getSimpleName() + ": M2_HOME not set");
			if (!mavenUserSettingsFile.exists()) {
				OgemaLauncher.LOGGER.warning(
						this.getClass().getSimpleName() + ": no user settings found, working without any configuration");
			}
		}
		try {
			init(mavenGlobalSettingsFile, mavenUserSettingsFile);
		} catch (SettingsBuildingException sbe) {
			throw new RuntimeException("Maven SettingsBuildingException: "
					+ sbe.getLocalizedMessage());
		}
	}

	private void init(File globalMavenSettings, File userMavenSettings) throws SettingsBuildingException {
		SettingsBuildingRequest sbreq = new DefaultSettingsBuildingRequest();
		if (globalMavenSettings != null && globalMavenSettings.exists()) {
			sbreq.setGlobalSettingsFile(globalMavenSettings);
		}
		if (userMavenSettings != null && userMavenSettings.exists()) {
			sbreq.setUserSettingsFile(userMavenSettings);
		}
		SettingsBuilder msb = new DefaultSettingsBuilderFactory().newInstance();
		SettingsBuildingResult sbr = msb.build(sbreq);
		_mavenSettings = sbr.getEffectiveSettings();

		String localRepository = _mavenSettings.getLocalRepository();
		if (localRepository == null) {
			localRepository = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
			OgemaLauncher.LOGGER.log(Level.FINER,
					this.getClass().getSimpleName() + ": using default local repository path: {0}", localRepository);
			OgemaLauncher.LOGGER.log(Level.FINER,
					this.getClass().getSimpleName() + ": default local repository path exists: {0}",
					new File(localRepository).exists());

		}
		File localRepoDir = new File(localRepository);

		LocalRepository localRepo = new LocalRepository(localRepoDir);
		_repoSys = newRepositorySystem();
		_session = newSession(_repoSys, localRepo);

		Map<String, RemoteRepository> prototypes = new LinkedHashMap<>();
		// adding default ogema repositories first... can be overridden by maven settings
		initDefaultRepositories(prototypes);

		/* Add all remote repositories found in local settings in all profiles. Memorize
		 * if central Maven repository "central" is amongst them. */
		boolean has_central = false;
		List<String> activeProfiles = _mavenSettings.getActiveProfiles();
		for (Profile p : _mavenSettings.getProfiles()) {
			if (activeProfiles.contains(p.getId())) {
				OgemaLauncher.LOGGER.finer(this.getClass().getSimpleName() + ": active profile: " + p.getId());
				for (Repository r : p.getRepositories()) {
					OgemaLauncher.LOGGER.finer(this.getClass().getSimpleName() + ": " + r.getId() + "=" + r.getUrl());
					if ("central".equalsIgnoreCase(r.getId())) {
						has_central = true;
					}

					Authentication auth = null;
					for (Server s : _mavenSettings.getServers()) {
						if (s.getId().equals(r.getId())) {
							OgemaLauncher.LOGGER.finer(this.getClass().getSimpleName() + ": have authentication for " + s.getId());
							auth = new AuthenticationBuilder()
							.addUsername(s.getUsername())
							.addPassword(s.getPassword()).build();
							break;
						}
					}
					
					// FIXME: there is no copy constructor or similar in aether? creating new objects
					// for policies for now ...
					org.apache.maven.settings.RepositoryPolicy snapshotPolicySettings = r.getSnapshots();
					// set default repo policy here because aether won't set defaults ...
					RepositoryPolicy snapshotPolicy = DEF_SNAPSHOT_POLICY;
					if(snapshotPolicySettings != null) {
						snapshotPolicy = new RepositoryPolicy(snapshotPolicySettings.isEnabled(),
								snapshotPolicySettings.getUpdatePolicy(), snapshotPolicySettings.getChecksumPolicy());
					} // else: use default
					
					org.apache.maven.settings.RepositoryPolicy releasePolicySettings = r.getReleases();
					// set default repo policy here because aether won't set defaults ...
					RepositoryPolicy releasePolicy = DEF_RELEASE_POLICY;
					if(releasePolicySettings != null) {
						releasePolicy = new RepositoryPolicy(releasePolicySettings.isEnabled(),
								releasePolicySettings.getUpdatePolicy(), releasePolicySettings.getChecksumPolicy());
					} // else: use default
					
					RemoteRepository remote = new RemoteRepository.Builder(
							r.getId(), "default", r.getUrl())
							.setAuthentication(auth).setSnapshotPolicy(snapshotPolicy)
							.setReleasePolicy(releasePolicy).build();

					prototypes.put(remote.getUrl(), remote);
				}
			}
		}

		/* If central repository has not been listed/overwritten, add it it to the list of
		 * remote repositories. */
		if (!has_central) {
			prototypes.put("http://repo1.maven.org/maven2", new RemoteRepository.Builder(
					"central", "default", "http://repo1.maven.org/maven2").setReleasePolicy(DEF_RELEASE_POLICY)
					.setSnapshotPolicy(DEF_SNAPSHOT_POLICY).build());
		}
		
		// set the proxies for those repositories that we've added:
		for(RemoteRepository prototype : prototypes.values()) {
			Proxy proxy = _session.getProxySelector().getProxy(prototype);
			if(proxy != null) {
				OgemaLauncher.LOGGER.finer(this.getClass().getSimpleName() + ": using proxy (" + proxy.getHost() + ") for remote repository: "
						+ prototype.getUrl());
			}
			_remoteRepos.add(new RemoteRepository.Builder(prototype).setProxy(
					proxy).build());
		}
	}
/*
	private void initDefaultRepositories(Map<String, RemoteRepository> prototypes) {
		prototypes.put(OGEMA_REPO_BASE_URL + LIBS_REL_URL,
				new RemoteRepository.Builder("ogema-releases", "default",
				OGEMA_REPO_BASE_URL + LIBS_REL_URL).setSnapshotPolicy(DEF_DISABLED_POLICY)
				.setReleasePolicy(DEF_RELEASE_POLICY).build());
		prototypes.put(OGEMA_REPO_BASE_URL + LIBS_SNAP_URL,
				new RemoteRepository.Builder("ogema-snapshots", "default",
				OGEMA_REPO_BASE_URL + LIBS_SNAP_URL).setReleasePolicy(DEF_DISABLED_POLICY)
				.setSnapshotPolicy(DEF_SNAPSHOT_POLICY).build());
		prototypes.put(OGEMA_REPO_BASE_URL + REMOTE_REPOS_URL,
				new RemoteRepository.Builder("remote-repos", "default",
				OGEMA_REPO_BASE_URL + REMOTE_REPOS_URL).setReleasePolicy(DEF_RELEASE_POLICY)
				.setSnapshotPolicy(DEF_SNAPSHOT_POLICY).build());
		prototypes.put(OGEMA_REPO_BASE_URL + EXT_OS_URL,
				new RemoteRepository.Builder("external-opensource", "default",
				OGEMA_REPO_BASE_URL + EXT_OS_URL).setReleasePolicy(DEF_RELEASE_POLICY)
				.setSnapshotPolicy(DEF_DISABLED_POLICY).build());
	}
*/
    private void initDefaultRepositories(Map<String, RemoteRepository> prototypes) {
        try {
            Properties p = new Properties();
            
            if (new File(_repositoryConfig).exists()) {
                try (InputStream is = new FileInputStream(new File(_repositoryConfig))) {
                    p.load(is);
                }
                OgemaLauncher.LOGGER.log(Level.FINE, String.format("configuring maven repositories from file %s", _repositoryConfig));
            } else {
                try (InputStream is = getClass().getResourceAsStream("/org/ogema/launcher/props/repositories.properties")) {
                    p.load(is);
                }
                OgemaLauncher.LOGGER.log(Level.FINE, "configuring maven repositories from internal configuration file");
            }
            
            String[] ids = p.get("ids").toString().split(",\\s*");
            for (String id: ids) {
                String url = p.getProperty(id).toString();
                
                RepositoryPolicy snapshotPolicy = DEF_SNAPSHOT_POLICY;
                String snapshotPolicyParams = (String) p.getProperty(id+".snapshot-policy");
                if (snapshotPolicyParams != null) {
                    snapshotPolicy = parsePolicy(snapshotPolicyParams);
                }
                
                RepositoryPolicy releasePolicy = DEF_RELEASE_POLICY;
                String releasePolicyParams = (String) p.getProperty(id+".release-policy");
                if (releasePolicyParams != null) {
                    releasePolicy = parsePolicy(releasePolicyParams);
                }
                RemoteRepository repo = new RemoteRepository.Builder(id, "default", url)
                        .setReleasePolicy(releasePolicy).setSnapshotPolicy(snapshotPolicy).build();
                prototypes.put(url, repo);
            }
            
            if (OgemaLauncher.LOGGER.isLoggable(Level.FINE)) {
                OgemaLauncher.LOGGER.log(Level.FINE, String.format("%d remote repositories:", prototypes.size()));
                for (Map.Entry<String, RemoteRepository> entry : prototypes.entrySet()) {
                    OgemaLauncher.LOGGER.log(Level.FINE, entry.getValue().toString());
                }
            }
        } catch (Exception ex) {
            OgemaLauncher.LOGGER.log(Level.SEVERE,
                    String.format("error parsing repository configuration (%s?): %s", _repositoryConfig, ex.getMessage()), ex);
        }
    }
    
    private static RepositoryPolicy parsePolicy(String propertyValue) {
        String[] params = propertyValue.split(",\\s*");
        boolean enabled = params[0].equalsIgnoreCase("enabled");
        String updatePolicy = params[1];
        String checksumPolicy = params.length > 2 ? params[2] : null;
        return new RepositoryPolicy(enabled, updatePolicy, checksumPolicy);
    }
    
	private static RepositorySystem newRepositorySystem() {
		/*
         * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
         * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
         * factories.
         */
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
            {
                exception.printStackTrace();
            }
        } );

        return locator.getService( RepositorySystem.class );
	}

	private RepositorySystemSession newSession(RepositorySystem system, LocalRepository localRepo) {
		DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
		system.newLocalRepositoryManager(session, localRepo);
		DefaultProxySelector dps = new DefaultProxySelector();
		for(org.apache.maven.settings.Proxy p : _mavenSettings.getProxies()) {
			Authentication auth = new AuthenticationBuilder().addUsername(
					p.getUsername()).addPassword(p.getPassword()).build();
			Proxy proxy = new Proxy(p.getProtocol(), p.getHost(), p.getPort(), auth);
			dps.add(proxy, p.getNonProxyHosts());
		}
		session.setProxySelector(dps);

		// TODO add transfer listener so that the user gets informed about process ?
//		session.setTransferListener(new ConsoleTransferListener(System.out));
//		session.setRepositoryListener(new ConsoleRepositoryListener());

		return session;
	}

	/**
	 * Try to locate an artifact in local repository.
	 * 
	 * @return {@link File} of the resolved artifact or <code>null</code> if
	 * it can't be resolved.
	 */
	public File findLocalArtifact(Artifact art) {
		if (art == null) {
			throw new IllegalArgumentException("null argument");
		}

		if(isVersionRange(art)) {
			try {
				art = resolveVersionRange(art, null);
			} catch (VersionRangeResolutionException e) {
				// not resolvable
				return null;
			}
		}

		ArtifactRequest req = new ArtifactRequest(art, null, null);
		try {
			ArtifactResult artRes = _repoSys.resolveArtifact(_session, req);
			OgemaLauncher.LOGGER.log(Level.FINER, this.getClass().getSimpleName() + ": found {0} = {1}",
					new Object[]{art, artRes.getArtifact().getFile().getName()});
			return artRes.getArtifact().getFile();
		} catch (ArtifactResolutionException are) {
			return null;
		}
	}

	/**
	 * Try to locate an artifact in remote repositories.
	 * 
	 * @return {@link File} of the resolved artifact or <code>null</code> if
	 * it can't be resolved.
	 */
	public File findRemoteArtifact(Artifact art) {
		if (art == null) {
			throw new IllegalArgumentException("null argument");
		}
		
		if(isVersionRange(art)) {
			try {
				art = resolveVersionRange(art, _remoteRepos);
			} catch (VersionRangeResolutionException e) {
				// not resolvable
				return null;
			}
		}
		
		ArtifactRequest req = new ArtifactRequest(art, _remoteRepos, null);
		try {
			ArtifactResult artRes = _repoSys.resolveArtifact(_session, req);
			OgemaLauncher.LOGGER.log(Level.FINER, this.getClass().getSimpleName() + ": found {0} = {1}",
					new Object[]{art, artRes.getArtifact().getFile().getName()});
			return artRes.getArtifact().getFile();
		} catch (ArtifactResolutionException ex) {
		}
		return null;
	}

	/**
	 * Try to find artifact in local and remote repositories.
	 *
	 * @param bi
	 */
	public File findArtifact(BundleInfo bi) {
		if (bi == null || bi.getMavenCoords() == null) {
			throw new IllegalArgumentException("null argument");
		}
		File result;
		DefaultArtifact art = new DefaultArtifact(bi.getMavenCoords());

		if (_offline) {
			result = findLocalArtifact(art);
		} else {
			if(_mavenRemoteFirst) {
				// FIXME: find remote not always work ...
				result = findRemoteArtifact(art);
				if(result == null) result = findLocalArtifact(art);
			} else {
				result = findLocalArtifact(art);
				if(result == null) result = findRemoteArtifact(art);
			}
		}
		
		return result;
	}

	public void setMavenRemoteFirst(boolean mavenRemoteFirst) {
		this._mavenRemoteFirst = mavenRemoteFirst;
	}

	public void setOffline(boolean offline) {
		this._offline = offline;
	}

	private boolean isVersionRange(Artifact art) {
		// check if we have a version range:
		VersionScheme versionScheme = new GenericVersionScheme();
		try {
			VersionConstraint versionConstraint = versionScheme.parseVersionConstraint(art.getVersion());
			if(versionConstraint.getRange() != null) {
				// we have a version range
				return true;
			}
		} catch(InvalidVersionSpecificationException e) {
			// invalid version specified!
			OgemaLauncher.LOGGER.warning( String.format(
					this.getClass().getSimpleName() + ": invalid version specified: %s:%s:%s -> " +
							"trying to find the latest version and continuing",
							art.getGroupId(), art.getArtifactId(),
							art.getVersion()) );
			art.setVersion("[0,");
			isVersionRange(art);
		}
		// no range ... version specified.
		return false;
	}

	private Artifact resolveVersionRange(Artifact art, List<RemoteRepository> repositories)
			throws VersionRangeResolutionException {
		VersionRangeRequest rangeRequest = new VersionRangeRequest(art, repositories, null);
		rangeRequest.setArtifact(art);
		// FIXME: resolving version range in offline mode won't work ... aether is looking for 
		// maven-metadata.xml which won't be updated / created without deploying 
		VersionRangeResult rangeResult = _repoSys.resolveVersionRange( _session, rangeRequest );
		return art.setVersion(rangeResult.getHighestVersion().toString());
	}

	@Override
	protected boolean canHandle(BundleInfo bi) {
		if(bi != null && bi.getMavenCoords() != null && !bi.getMavenCoords().isEmpty()) {
			// expected string -> <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
			String mavenCoordPattern = "([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)";
			if(bi.getMavenCoords().matches(mavenCoordPattern)) {
				return true;
			} else {
				OgemaLauncher.LOGGER.warning(this.getClass().getSimpleName() + ": illegal maven coordinates - " +
						"please check your config file. Maven coordinates: \"" +
						bi.getMavenCoords() + "\"\nExpected: " +
						mavenCoordPattern);
			}
		}
		return false;
	}

	@Override
	protected boolean resolveBundle(BundleInfo bi) {
		File artifactFile = findArtifact(bi);

		if (artifactFile != null) {
			bi.setMavenArtifactLocation(artifactFile.toURI());
			bi.setPreferredLocation(artifactFile.toURI());
			return true;
		} else {
			OgemaLauncher.LOGGER.log(Level.FINE, this.getClass().getSimpleName() + " - WARNING: {0} not found via maven",
					bi.getMavenCoords());
		}

		return false;
	}

}
