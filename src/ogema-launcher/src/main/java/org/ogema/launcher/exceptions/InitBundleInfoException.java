/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.exceptions;

public class InitBundleInfoException extends Exception {

	private static final long serialVersionUID = -3941824526313785097L;

	public InitBundleInfoException() {
		super();
	}
	
	public InitBundleInfoException(String msg) {
		super(msg);
	}
	
	public InitBundleInfoException(Throwable cause) {
		super(cause);
	}
	
	public InitBundleInfoException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
