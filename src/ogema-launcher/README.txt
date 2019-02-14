*****************************************************************
*								*
*	         	OGEMA launcher build Instruction				*
*								*
*****************************************************************

The OGEMA launcher provides a mechanism to start an OSGi framework,
and is used in particular as the default launcher for OGEMA (www.ogema.org).

-------------
Prerequisites
-------------

- Maven 3 or higher
- Java 7 SDK or higher

------------
Quick Start 
------------

1) Build
	To compile the launcher from the sources go to the base directory and execute:
	
		mvn clean install -DskipTests
		mvn install -DskipTests -offline
	
2) Rundirectory
	Rename the generated .jar file (the shaded version) in the target folder to "ogema-launcher.jar", and
	copy it to the OSGi run directory (e.g. the "example" folder, or the OGEMA 
	demokit, see http://www.ogema.org/). The run directory requires two configuration
	files, config/config.xml and config/ogema.properties (see example).
	
3a) Run from Console
	To start the OGEMA framework from the console you’ll have to change to your run directory and start it via 
	
		java –jar ogema-launcher.jar -clean [optional arguments]
		
	The launcher tries to download the required bundles from Maven central, therefore the first start takes some time,
	and requires an internet connection; alternatively, the bundles can be loaded from the local Maven repository, if
	they are available there already.
	If the configuration and/or properties files are not the default ones, they need to be specified via a --config and/or --properties option. 
	In the OGEMA demokit rundir, there is also a batch, respectively cmd file for running the system on Linux and Windows.
	The "-clean" option deletes all data from previous runs; without it, the framework starts in the configuration it had when it was last stopped.
	In particular, changes to the config.xml file have no effect in an unclean start. 
	
3b) Run within Eclipse
	To start the OGEMA framework from Eclipse (or another IDE) you’ll have to create a new run configuration first.
	You can do that manually or simply right click on the ogema-launcher project and choose “Run As -> Java Application”. 
	Right afterwards you’ll be prompted to select the appropriate Java Application from a list. Simply select OgemaLauncher from the list and continue.
	At the first start an error will be thrown in the console because it can’t find the configuration file. 
	Go to the “Run” menu and choose “Run configurations…”. Select the newly created ogema-launcher configuration and add your 
	run directory as the working directory in the “Arguments” tab. Now you can add the parameters that you need into the text field below “Program arguments”, 
	such as "-clean".
	
4) Further information
	See 
	https://www.ogema-source.net/wiki/display/OGEMA/OGEMA+Demokit
	https://www.ogema-source.net/wiki/display/OGEMA/Rundirectories
