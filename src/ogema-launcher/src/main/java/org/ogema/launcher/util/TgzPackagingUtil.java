/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.util;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * Utility class for building an OGEMA distribution archive. Can be customized
 * via a properties file ("build.properties") placed in the launcher working
 * directory, see the constants defined in this class for details.
 *
 * @author jlapp
 * @author mperez
 */
public class TgzPackagingUtil extends TarPackagingUtil {
    
    private final byte[] buf = new byte[4096];
    
    public TgzPackagingUtil(String filename) {
        super(filename);
    }

    @Override
    protected ArchiveOutputStream createStream(OutputStream os) throws IOException {
        GzipCompressorOutputStream zipStream = new GzipCompressorOutputStream(os);
        TarArchiveOutputStream tarStream = new TarArchiveOutputStream(zipStream);
        tarStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return tarStream;
    }

    @Override
    protected String getFilename(String basename) {
        return basename + ".tar.gz";
    }    

}