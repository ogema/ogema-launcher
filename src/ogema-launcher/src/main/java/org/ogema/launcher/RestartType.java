package org.ogema.launcher;

public enum RestartType {

	/**
	 * Quit
	 */
	EXIT,
	
	/**
	 * Restart with same classloader 
	 */
	RESTART,
	
	/**
	 * Restart with new classloader
	 */
	NEW_CLASSLOADER,
	
	/**
	 * Restart in a fresh JVM
	 */
	NEW_VM

}
