/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.exceptions;

public class FrameworkConfigurationException extends Exception {
	
	private static final long serialVersionUID = 2907119329745559739L;

	public FrameworkConfigurationException() {
		super();
	}
	
	public FrameworkConfigurationException(String msg) {
		super(msg);
	}
	
	public FrameworkConfigurationException(Throwable cause) {
		super(cause);
	}
	
	public FrameworkConfigurationException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	// TODO: implement getLocalizedMessage() ...
}
