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
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.ogema.launcher.OgemaLauncher;

/**
 *
 * @author jlapp
 */
public class ZipPackagingUtil extends AbstractPackagingUtil {
    
    public ZipPackagingUtil() {
        super();
    }
    
    public ZipPackagingUtil(String filename) {
        super(filename);
    }

    @Override
    protected ArchiveOutputStream createStream(OutputStream os) throws IOException {
        return new ZipArchiveOutputStream(os);
    }

    @Override
    protected void writeEntry(ArchiveOutputStream os, String filename, File file) throws IOException {
        byte[] buf = new byte[4096];
        OgemaLauncher.LOGGER.fine("adding to archive: " + filename);
        ZipArchiveEntry entry = new ZipArchiveEntry(filename.replace(File.separator, "/"));
        if (isExecutable(file)){
            entry.setUnixMode(0755);
            OgemaLauncher.LOGGER.fine("set as executable: " + filename);
        }
		os.putArchiveEntry(entry);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			int l = 0;
			while ((l = fis.read(buf)) != -1) {
				os.write(buf, 0, l);
			}
		} finally {
			if(fis != null) {
				fis.close();
			}
			os.closeArchiveEntry();
		}
    }

    @Override
    protected String getFilename(String basename) {
        return basename + ".zip";
    }    
    
}
