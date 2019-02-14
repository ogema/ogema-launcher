/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarFileBuilder {
	public static void buildJar(File loc, File target) throws IOException {
		JarOutputStream jarOutputStream = null;
		try {
			if(!target.getParentFile().exists()) {
				target.getParentFile().mkdirs();
			}
			jarOutputStream = new JarOutputStream(new FileOutputStream(target, false));
			add(loc, jarOutputStream, loc);
		} finally {
			if(jarOutputStream != null) {
				jarOutputStream.close();
			}
		}
	}

	private static void add(File source, JarOutputStream jarOutputStream,
			File baseDir) throws IOException {
		BufferedInputStream in = null;
		try {
			String name = baseDir.toURI().relativize(
					source.toURI()).getPath();
			if (source.isDirectory()) {
				if (!name.isEmpty()) {
					JarEntry entry = new JarEntry(name);
					entry.setTime(source.lastModified());
					jarOutputStream.putNextEntry(entry);
					jarOutputStream.closeEntry();
				}
				for (File nestedFile: source.listFiles()) {
					add(nestedFile, jarOutputStream, baseDir);
				}
			} else {
				JarEntry entry = new JarEntry(name);
				entry.setTime(source.lastModified());
				jarOutputStream.putNextEntry(entry);
				in = new BufferedInputStream(new FileInputStream(source));

				byte[] buffer = new byte[1024];
				int count = -1;
				while ((count = in.read(buffer)) != -1) {
					jarOutputStream.write(buffer, 0, count);
				}
				jarOutputStream.closeEntry();
			}
		}
		finally {
			if (in != null) {
				in.close();
			}
		}
	}
}