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
import org.ogema.launcher.OgemaLauncher;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * Utility class for building an OGEMA distribution archive. Can be customized
 * via a properties file ("build.properties") placed in the launcher working
 * directory, see the constants defined in this class for details.
 *
 * @author jlapp
 * @author mperez
 */
public class TarPackagingUtil extends AbstractPackagingUtil {
    
    private final byte[] buf = new byte[4096];
    
    public TarPackagingUtil(String filename) {
        super(filename);
    }

    @Override
	protected void writeEntry(ArchiveOutputStream zos, String filename, File file) throws IOException {
        OgemaLauncher.LOGGER.fine("adding to archive: " + filename);
        TarArchiveEntry entry = new TarArchiveEntry(file, filename.replace(File.separator, "/"));
        if (isExecutable(file)){
            entry.setMode(0755);
            OgemaLauncher.LOGGER.fine("mark as executable: " + filename);
        }
		zos.putArchiveEntry(entry);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			int l = 0;
			while ((l = fis.read(buf)) != -1) {
				zos.write(buf, 0, l);
			}
		} finally {
			if(fis != null) {
				fis.close();
			}
			zos.closeArchiveEntry();
		}
	}
    
    @Override
    protected ArchiveOutputStream createStream(OutputStream os) throws IOException {
        TarArchiveOutputStream tarStream = new TarArchiveOutputStream(os);
        tarStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return tarStream;
    }

    @Override
    protected String getFilename(String basename) {
        return basename + ".tar";
    }

}