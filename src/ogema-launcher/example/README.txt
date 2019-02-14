*****************************************************************
*								*
*	         		OGEMA launcher sample rundir 				*
*								*
*****************************************************************

This is a minimalistic rundir for the OGEMA launcher. It starts 
an OSGi framework and a couple of bundles, which are specified in
the file

	config/config.xml

------------
Quick Start 
------------

1) Start a command line (typically Bash or cmd), go the run-directory,
type
	
	java -jar ogema-launcher.jar -clean
	
The "-clean" option deletes all data from previous runs; without it, 
the framework starts in the configuration it had when it was last stopped.
In particular, changes to the config.xml file have no effect in an
unclean start. Or use the start scripts:
On Windows: 
	start.cmd -clean
On Linux:
	./start.sh -clean

Download the OGEMA demokit for a more interesting run configuration:
https://www.ogema-source.net/wiki/display/OGEMA/OGEMA+Demokit