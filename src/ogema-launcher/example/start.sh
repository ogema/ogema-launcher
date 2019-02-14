#!/bin/bash

#
# Start OGEMA with default configuration (Equinox OSGi and no security)
#

LAUNCHER=${OGEMA_LAUNCHER:-./ogema-launcher.jar}
CONFIG=${OGEMA_CONFIG:-config/config.xml}
PROPERTIES=${OGEMA_PROPERTIES:-config/ogema.properties}
OPTIONS=$OGEMA_OPTIONS
JAVA=${JAVA_HOME:+${JAVA_HOME}/bin/}java
EXTENSIONS=bin/ext$(find bin/ext/ -iname "*jar" -printf :%p)
VMOPTS="$VMOPTS -cp $LAUNCHER:EXTENSIONS"

# Determine java version
jver=$(java -version 2>&1 | grep -i version | sed 's/.*version ".*\.\(.*\)\..*"/\1/; 1q')
# remove all non-numeric entries
jver=${jver//[^[:digit:]]/}
if [[ ${jver:0:1} == 1 ]]; then jver="${jver:1:2}"; else jver="${jver:0:1}"; fi
if [[ $jver == 9 ]]; then JAVA="${JAVA} --add-modules java.xml.bind"; fi

if [[ " $@ " =~ " --verbose " ]]; then
VMOPTS="$VMOPTS -Dequinox.ds.debug=true -Dequinox.ds.print=true"
echo $JAVA $VMOPTS org.ogema.launcher.OgemaLauncher --config $CONFIG --properties $PROPERTIES $OPTIONS $*
fi

while true; do
    $JAVA $VMOPTS org.ogema.launcher.OgemaLauncher --config $CONFIG --properties $PROPERTIES $OPTIONS $*
	RETURNCODE=$?
	# 127 = -4
    if [ $RETURNCODE != 127 ]; then
        break
    else
		if [ "$OPTIONS" == "${OPTIONS/-restart/}" ]; then
			OPTIONS="$OPTIONS -restart"
		fi
    fi
done