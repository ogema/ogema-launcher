/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.resolver.progress;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

public class ConsoleTransferListener extends AbstractTransferListener {

	// for each 5% make a dot in progress bar ...
	private static final float PROGRESS_UPDATE_PERCENTAGE = 5f;

	private PrintStream out = null;

	private Map<TransferResource, Long> progressedResources = new ConcurrentHashMap<TransferResource, Long>();

	public ConsoleTransferListener() {
	}

	public ConsoleTransferListener( PrintStream out ) {
		this.out = ( out != null ) ? out : System.out;
	}

	@Override
	public void transferInitiated(TransferEvent event) {
		out.println("initiated " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
		if(event.getResource().getContentLength() < 0) {
			// not found ...
			return;
		}
		out.println((event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading")
				+ ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName()
				+ " -> total size: " + event.getResource().getContentLength());
		out.print("Progress: [");
	}

	@Override
	public void transferProgressed(TransferEvent event) {
		out.println("progressed ... " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
		long newTransferredBytes = event.getTransferredBytes();
		Long oldTransferredBytes = progressedResources.put(event.getResource(), newTransferredBytes);
		long totalSize = event.getResource().getContentLength();
		if(oldTransferredBytes == null) {
			// in transfer initiated getContentLength() will return always -1 so we
			// print the initial message here:
			out.println((event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading")
					+ ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName()
					+ " -> total size: " + event.getResource().getContentLength());
			out.print("Progress: [");
			for(float f = 0; f < newTransferredBytes / (float) totalSize; f += PROGRESS_UPDATE_PERCENTAGE) {
				out.print(".");
			}
		} else {
			// we've already printed progress -> check if we need to update
			float oldState = oldTransferredBytes / (float) totalSize;
			float newState = newTransferredBytes / (float) totalSize;

			for(float f = oldState + PROGRESS_UPDATE_PERCENTAGE; Float.compare(f, newState) <= 0; oldState += PROGRESS_UPDATE_PERCENTAGE) {
				// update
				out.print(".");
			}
		}
	}

	@Override
	public void transferSucceeded( TransferEvent event ) {
//		out.println("transferSucceeded! " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
		out.println("]");
	}

	@Override
	public void transferFailed( TransferEvent event ) {
		out.println("Error: transfer failed (" + event.getResource().getRepositoryUrl() + event.getResource().getResourceName() + ")");
	}
}
