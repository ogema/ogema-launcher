/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.ogema.launcher.LauncherConstants.KnownProgOptions;
import org.ogema.launcher.config.LauncherConfiguration;
import org.ogema.launcher.exceptions.FrameworkConfigurationException;

/**
 * Main class that initializes the configuration and is launching the framework.
 * </br></br>
 * The following program arguments are allowed:
 * <ul>
 * <li>-?,--help</li>
 * <li>-b,--build</li>
 * <li>-bo,--buildoffline</li>
 * <li>-c,--console</li>
 * <li>-ca,--configarea <config area></li>
 * <li>-cfg,--config <config file></li>
 * <li>-clean</li>
 * <li>-o,--offline</li>
 * <li>-p,--properties <properties file(s)></li>
 * <li>-v,--verbose</li>
 * <li>-w,--workspaceloc <workspace location></li>
 * </ul>
 *
 * @author mperez
 */
@SuppressWarnings("deprecation")
public class OgemaLauncher {
    
    private static String version;
    public static final Logger LOGGER = setupLogger();

    public static void main(final String[] args) {
        Options options = setupOptions();
        try {
            final CommandLine cmdLine = parseOptions(options, args);
            if (cmdLine.hasOption(KnownProgOptions.HELP.getSwitch())) {
                printHelp(options);
                return;
            }

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        new OgemaLauncher(cmdLine, args).run();
                    } catch (FrameworkConfigurationException | ParseException | IOException e) {
                        LOGGER.severe("Error: " + e.getLocalizedMessage());
                    }
                }
            };
            new Thread(r).start();
            //new OgemaLauncher(cmdLine).run();
        } catch (ParseException e) {
            // oops, something went wrong
            LOGGER.warning("Parsing failed. Reason: " + e.getLocalizedMessage() + "\n");
            printHelp(options);
        }/* catch (FrameworkConfigurationException e) {
            LOGGER.warning("Error: " + e.getLocalizedMessage());
        }*/
    }

    private static void printHelp(Options options) {
        System.out.printf("OGEMA launcher version %s%n", getVersion());
        HelpFormatter formatter = new HelpFormatter();
        
        formatter.setWidth(120);
        formatter.printHelp("java -jar ogema-launcher-"+getVersion()+".jar", options, true);
    }

    private static CommandLine parseOptions(Options options, String[] args) throws ParseException {
    	// Recommended as replacement for the deprecated Gnu parser, but 
    	// not working - it interprets the -ub option as a filename for the properties file
//        CommandLineParser parser = new DefaultParser(); 
    	CommandLineParser parser = new GnuParser();
        // parse the command line arguments
        return parser.parse(options, args);
    }

    private static Logger setupLogger() {
        if (!System.getProperties().containsKey("java.util.logging.SimpleFormatter.format")) {
            System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
        }
        ConsoleHandler handler = new ConsoleHandler();

        if (System.getProperties().getProperty("os.name")
                .toLowerCase().startsWith("window")) {
            try {
                handler.setEncoding("CP850");
            } catch (UnsupportedEncodingException uee) {
                System.err.println(uee);
            }
        }
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());

        Logger l = Logger.getLogger("launcher");
        l.setUseParentHandlers(false);
        l.addHandler(handler);
//        l.setLevel(Level.WARNING);
        l.setLevel(Level.INFO);
        return l;
    }

    private static Options setupOptions() {
        Options options = new Options();
        for (KnownProgOptions progOpt : KnownProgOptions.values()) {
            Option opt = new Option(progOpt.getSwitch(), progOpt.getDescription());;
            switch (progOpt.getNmbOfArgs()) {
                case 0:
                    break;
                default:
                    opt.setArgs(progOpt.getNmbOfArgs());
                    opt.setArgName(progOpt.getArgName());
                    opt.setOptionalArg(progOpt.isArgOptional());
            }

            opt.setLongOpt(progOpt.getLongSwitch());

            options.addOption(opt);
        }

        return options;
    }

    private final CommandLine options;
    private final String[] args;
    private CommandLine secondOptions;

    public OgemaLauncher(CommandLine options, final String[] args) {
        this.options = options;
        this.args = args;
        if (options.hasOption(KnownProgOptions.VERBOSE.getSwitch())) {
            LOGGER.setLevel(Level.ALL);
        }
        initProperties(options);
    }
    
    private synchronized CommandLine getSecondOptions() throws ParseException {
    	if (secondOptions != null)
    		return secondOptions;
    	if (args == null || args.length == 0) {
        	secondOptions  = options;
        } else {
        	final String[] args2 = new String[args.length+1];
        	System.arraycopy(args, 0, args2, 0, args.length);
        	args2[args.length] = "-restart";
        	secondOptions = parseOptions(setupOptions(), args2);
        	/*
        	try {
				secondOptions = secondParser.parse(setupOptions(), args);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			*/
        }
    	return secondOptions;
    }
    
    private void run() throws FrameworkConfigurationException, ParseException, IOException {
        LauncherConfiguration configuration = new LauncherConfiguration(options);
        final OgemaFramework framework = new OgemaFramework(configuration);
        if (!requiresLock(configuration)) {
        	framework.start(ClassLoader.getSystemClassLoader());
        	return;
        }
        final Path lockFile = Paths.get(LauncherConstants.LOCK_FILE);
		if (!Files.exists(lockFile))
			Files.createFile(lockFile);
		FileLock lock = null;
		try (final RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw")) {
			lock = raf.getChannel().tryLock();
			if (lock == null) // locked by another process
				throw new OverlappingFileLockException();
	        while (true) {
	        	final  RestartType restart = framework.start(ClassLoader.getSystemClassLoader());
	        	switch (restart) {
	        	case NEW_CLASSLOADER:
	        		System.out.println("Framework terminated with request to restart with a fresh classloader... going to restart now");
	        	case RESTART:
	        		break; // restart framework with new classloader
	        	case NEW_VM:
	        		System.out.println("Framework terminated with request to restart in a fresh VM... bootclasspath modified");
	        		System.exit(-4); // launcher must be restarted in fresh VM // -4 is the convention used by bnd launcher
	        	case EXIT:
	        	default:
	        		System.exit(0);
	        	}
	        	// removing --clean, --build and -ub options, etc 
	        	framework.reset(new LauncherConfiguration(getSecondOptions(), configuration.getFrameworkConfig()));
	        }
		} catch (OverlappingFileLockException | ClosedChannelException e) {
			OgemaLauncher.LOGGER.severe("Could not launch the framework, maybe it is already running? Failed to acquire lock on file " + lockFile);
        } finally {
        	if (lock != null) {
        		try {
        			lock.release();
        		} catch (Exception ignore) {}
        	}
        }
    }
    
    // when the build switch is set we should not block the rundir
    private static boolean requiresLock(final LauncherConfiguration config) {
    	final CommandLine options = config.getOptions();
    	if (options.hasOption(KnownProgOptions.BUILD.getSwitch())
    			|| options.hasOption(KnownProgOptions.DEPLOYMENT_PACKAGE.getSwitch()))
    		return false;
    	return true;
    }
    
    private void initProperties(CommandLine options) {
        Properties properties = new Properties();
        // first load default props
        try {
            URL url = Thread.currentThread().getContextClassLoader()
                    .getResource(LauncherConstants.DEF_PROP_FILE);
            properties.load(url.openStream());
        } catch (IOException e) {
            OgemaLauncher.LOGGER.warning("Error while loading default "
                    + "properties: " + e.getMessage());
        }

        Properties props = new Properties();
        
        // load properties files -> will overwrite default props if there are duplicates
        if (options.hasOption(KnownProgOptions.PROPS.getSwitch())) {
            String[] propertieFilesAsString = options.getOptionValues(
                    KnownProgOptions.PROPS.getSwitch());

            for (String propFileAsString : propertieFilesAsString) {
                File f = locatePropertiesFile(propFileAsString);
				// check if file exists... if not the error msg were already
                // logged to the console
                if (f == null) {
                    continue;
                }

                try {
                    props.load(new FileInputStream(f));
                    properties.putAll(props);
                    OgemaLauncher.LOGGER.info(f.getAbsolutePath() + " - properties loaded.");
                } catch (IOException e) {
                    OgemaLauncher.LOGGER.warning("Error while loading properties "
                            + "from file: " + f.getAbsolutePath() + "\n"
                            + e.getMessage());
                }
            }
        } else {
        	// check if config/ogema.properties exist and load ...
        	File propertiesFile = locatePropertiesFile(LauncherConstants.DEFAULT_PROPERTIES_FILE);
        	if(propertiesFile != null) {
        		try {
					props.load(new FileInputStream(propertiesFile));
					properties.putAll(props);
					OgemaLauncher.LOGGER.info(propertiesFile.getAbsolutePath() + " - properties loaded.");
				} catch (FileNotFoundException e) {
					OgemaLauncher.LOGGER.warning("Error while loading properties "
							+ "from file: " + propertiesFile.getAbsolutePath() + "\n"
							+ e.getMessage());
				} catch (IOException e) {
					OgemaLauncher.LOGGER.warning("Error while loading properties "
							+ "from file: " + propertiesFile.getAbsolutePath() + "\n"
							+ e.getMessage());
				}
        	}
        }

        // allow the system to override any properties with -Dkey=value
        for (Object key : properties.keySet()) {
            String s = (String) key;
            String v = System.getProperty(s);
            if (v != null) {
                properties.put(key, v);
            }
        }
        System.getProperties().putAll(properties);
    }
    
    public static synchronized String getVersion(){
        if (version != null){
            return version;
        }
        try {
            URL url = Thread.currentThread().getContextClassLoader()
                    .getResource(LauncherConstants.VERSION_FILE);
            Properties properties = new Properties();
            properties.load(url.openStream());
            version = properties.getProperty("launcher.version", "unknown");
        } catch (IOException e) {
            OgemaLauncher.LOGGER.warning("Error while loading default "
                    + "properties: " + e.getMessage());
        }
        return version;
    }

    /**
     * Checks if the given properties file is a system resource or an
     * absolute/relative path to the file.
     *
     * @param propFileAsString
     * @return <code>null</code> if the file can't be found or wasn't readable
     * otherwise the appropriate {@link File} object.
     */
    private File locatePropertiesFile(String propFileAsString) {
        File tmp = null;
        // check system resources:
        URL sysRes = ClassLoader.getSystemResource(propFileAsString);
        if (sysRes != null) {
            try {
                tmp = new File(sysRes.toURI());
            } catch (URISyntaxException ignore) {
            }
        }

        if (tmp == null || !tmp.exists() || !tmp.canRead()) {
            tmp = new File(propFileAsString);
        }

        if (!tmp.exists() || !tmp.canRead()) {
            String addToMsg = !tmp.exists()
                    ? " doesn't exist" : " can't be read";

            OgemaLauncher.LOGGER.warning(String.format("Given properties "
                    + "file: %s %s", tmp.getAbsolutePath(), addToMsg));

            return null;
        }

        return tmp;
    }
   
    /*
    public static final CommandLineParser secondParser = new GnuParser() {
    	
    	private final KnownProgOptions[] IGNORED = new KnownProgOptions[] {
    		KnownProgOptions.BUILD,
    		KnownProgOptions.CLEAN,
    		KnownProgOptions.UPDATE_BUNDLES,
    		KnownProgOptions.DEPLOYMENT_PACKAGE
    	};
    	
        private final String normalize(final String arg) {
        	try {
        		int idx = 0;
    	    	while (arg.charAt(idx) == '-')
    	    		idx++;
    	    	return arg.substring(idx);
        	} catch (IndexOutOfBoundsException e) {
        		return arg;
        	}
        }

        @SuppressWarnings("unchecked")
		@Override
        protected void processOption(final String arg, @SuppressWarnings("rawtypes") final ListIterator iter) throws ParseException {
        	boolean ignore = false;
        	final String argRed = normalize(arg);
        	String swtch;
        	for (KnownProgOptions opt : IGNORED) {
        		swtch = opt.getSwitch();;
        		if (argRed.equals(swtch)) {
        			ignore = true;
        			break;
        		}
        		swtch = opt.getLongSwitch();
        		if (argRed.equals(swtch)) {
        			ignore = true;
        			break;
        		}
        	}
        	if (!ignore)
        		super.processOption(arg, iter);
        }

    };
    */
}
