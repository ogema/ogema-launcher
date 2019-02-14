/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config.parser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.ogema.launcher.BundleInfo;
import org.ogema.launcher.LauncherConstants;
import org.ogema.launcher.OgemaLauncher;
import org.ogema.launcher.config.ConfigurationFactory;
import org.ogema.launcher.config.FrameworkConfiguration;
import org.ogema.launcher.config.xml.Configuration;
import org.ogema.launcher.config.xml.Bundle;
import org.ogema.launcher.config.xml.Configuration.Bundles;
import org.ogema.launcher.config.xml.Configuration.Properties;
import org.ogema.launcher.config.xml.Configuration.Properties.Property;
import org.ogema.launcher.exceptions.FrameworkConfigurationException;

public class XmlStaxConfigParser extends ConfigurationParser {

    private static final String FILE_SUFFIX = ".xml";

    @Override
    protected boolean canHandle(File file) {
        return file.getName().toLowerCase().endsWith(FILE_SUFFIX);
    }

    private Configuration staxParse(File f) throws IOException {
        Configuration config = new Configuration();
        try (InputStream in = new FileInputStream(f); BufferedInputStream bin = new BufferedInputStream(in)) {
            XMLInputFactory ifac = XMLInputFactory.newInstance();
            XMLStreamReader rawReader = ifac.createXMLStreamReader(bin);
            StreamFilter filter = new StreamFilter() {

                @Override
                public boolean accept(XMLStreamReader reader) {
                    int type = reader.getEventType();
                    return type == XMLStreamReader.START_ELEMENT ||
                            type == XMLStreamReader.END_ELEMENT ||
                            type == XMLStreamReader.ATTRIBUTE;
                }
            };
            XMLStreamReader reader = ifac.createFilteredReader(rawReader, filter);
            
            acceptConfiguration(config, reader);
            
        } catch (XMLStreamException xse) {
            throw new IOException(xse);
        }
        return config;
    }
    
    private Configuration acceptConfiguration(Configuration cfg, XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals("configuration")) {
            throw new IllegalStateException("expected configuration element " + reader.getLocation());
        }
        reader.next();
        Bundle frameworkBundle = acceptBundle(reader);
        cfg.setFrameworkbundle(frameworkBundle);
        
        reader.next();
        cfg.setBundles(acceptBundles(reader));
        
        reader.next();
        if (reader.getLocalName().equals("properties")){
            cfg.setProperties(acceptProperties(reader));
            reader.next();
        }
        if (reader.getLocalName().equals("deleteList")){
            cfg.setDeleteList(acceptDeleteList(reader));
        }
        return cfg;
    }
    
    private Properties acceptProperties(XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals("properties")) {
            throw new IllegalStateException("expected properties element " + reader.getLocation());
        }
        Properties p = new Configuration.Properties();
        while (reader.next() != XMLStreamReader.END_ELEMENT) {
            p.getProperty().add(acceptProperty(reader));
        }        
        return p;
    }
    
    private Property acceptProperty(XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals("property")) {
            throw new IllegalStateException("expected property element " + reader.getLocation());
        }
        Property p = new Configuration.Properties.Property();
        p.setKey(reader.getAttributeValue(null, "key"));
        p.setValue(reader.getAttributeValue(null, "value"));
        while (reader.next() != XMLStreamReader.END_ELEMENT) {
        }
        //System.out.printf("%s=%s%n", p.getKey(), p.getValue());
        return p;
    }
    
    private Configuration.DeleteList acceptDeleteList(XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals("deleteList")) {
            throw new IllegalStateException("expected deleteList element " + reader.getLocation());
        }
        Configuration.DeleteList dl = new Configuration.DeleteList();
        while (reader.next() != XMLStreamReader.END_ELEMENT) {
            dl.getFile().add(acceptDeleteFile(reader));
        }
        return dl;
    }
    
    private String acceptDeleteFile(XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals("file")) {
            throw new IllegalStateException("expected deleteList element " + reader.getLocation());
        }
        String filename = reader.getElementText();
        //System.out.println(filename);
        return filename;
    }
    
    private Configuration.Bundles acceptBundles(XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals("bundles")) {
            throw new IllegalStateException("expected bundles element " + reader.getLocation());
        }
        Bundles bundles = new Configuration.Bundles();
        while (reader.next() != XMLStreamReader.END_ELEMENT) {
            bundles.getBundle().add(acceptBundle(reader));
        }
        return bundles;
    }
    
    private Bundle acceptBundle(XMLStreamReader reader) throws XMLStreamException {
        if (reader.getEventType() != XMLStreamReader.START_ELEMENT || !(reader.getLocalName().equals("bundle")||reader.getLocalName().equals("frameworkbundle"))) {
            throw new IllegalStateException("expected bundle or frameworkbundle element " + reader.getLocation());
        }
        Bundle b = new Bundle();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String name = reader.getAttributeLocalName(i);
            String v = reader.getAttributeValue(i);
            switch (name) {
                case "artifactId" : b.setArtifactId(v); break;
                case "dir" : b.setDir(v); break;
                case "file" : b.setFile(v); break;
                case "groupId" : b.setGroupId(v); break;
                case "start" : b.setStart(Boolean.valueOf(v)); break;
                case "startLevel" : b.setStartLevel(BigInteger.valueOf(Long.valueOf(v))); break;
                case "version" : b.setVersion(v); break;
            }
        }
        while (reader.next() != XMLStreamReader.END_ELEMENT) {
        }
        //System.out.println(b);
        return b;
    }

    @Override
    protected FrameworkConfiguration parseFile(File configFile)
            throws FrameworkConfigurationException {
        FrameworkConfiguration result = null;
        try {
            long memBefore = Runtime.getRuntime().totalMemory();
            long now = System.currentTimeMillis();
            
            Configuration cfg = staxParse(configFile);

            Bundle frameworkbundle = cfg.getFrameworkbundle();
            BundleInfo fwkBundle = null;
            if (frameworkbundle != null) {
                fwkBundle = getBundleInfo(frameworkbundle);
            }
            if (fwkBundle == null) {
				// no frameworkbundle entry in config or error in config.xml (-> already logged)
                // use default fwk bundle:
                fwkBundle = LauncherConstants.DEF_FRAMEWORK_BUNDLE;
            }

            String mvnCoords = fwkBundle.getMavenCoords();
            result = ConfigurationFactory.createFrameworkConfiguration(
                    mvnCoords != null ? mvnCoords : fwkBundle.getFileName());

            result.setFrameworkBundle(fwkBundle);

            for (Bundle b : cfg.getBundles().getBundle()) {
                BundleInfo bi = getBundleInfo(b);
                result.addToBundles(bi);
            }

            if (cfg.getProperties() != null) {
                for (Property p : cfg.getProperties().getProperty()) {
                    result.addFrameworkProperty(p.getKey(), p.getValue());
                }
            }

            if (cfg.getDeleteList() != null) {
                result.setDeleteList(cfg.getDeleteList().getFile());
            }

            OgemaLauncher.LOGGER.fine(String.format("read configuration, time=%dms, delta mem=%d",
                    System.currentTimeMillis() - now, Runtime.getRuntime().totalMemory() - memBefore));
        } catch (IOException e) {
            OgemaLauncher.LOGGER.warning(this.getClass().getSimpleName()
                    + ": " + e);
        }

        return result;
    }

    private BundleInfo getBundleInfo(Bundle b) {
        if (b.getFile() == null && (b.getGroupId() == null
                || b.getArtifactId() == null || b.getVersion() == null)) {
            OgemaLauncher.LOGGER.warning("Invalid config file entry: missing "
                    + "file location and/or group id, artifact id and version for bundle "
                    + b + ". Skipping this entry ...");
            return null;
        }

        BundleInfo result = new BundleInfo();
        result.setFileName(b.getFile());
        result.setBinDir(b.getDir());

        if (b.getGroupId() != null || b.getArtifactId() != null
                || b.getVersion() != null) {
			// we need all attributes (group id, artifact id and version)
            // if one is missing print an error msg
            if (b.getGroupId() == null || b.getArtifactId() == null
                    || b.getVersion() == null) {
                OgemaLauncher.LOGGER.warning("Invalid config file: missing "
                        + "either group id, artifact id or version for bundle "
                        + getBundleIdentifier(b) + ". The file location "
                        + "will be used and maven coordinates is ignored.");
            } else {
                result.setMavenCoords(b.getGroupId() + ":" + b.getArtifactId()
                        + ":" + b.getVersion());
            }
        }

        result.setStart(b.isStart());
        result.setStartLevel(b.getStartLevel().intValue());

        return result;
    }

    private String getBundleIdentifier(Bundle b) {
        if (b.getFile() != null) {
            return b.getFile();
        }

        if (b.getArtifactId() != null) {
            return b.getArtifactId();
        }

        return "unknown";
    }
    
}
