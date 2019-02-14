@ECHO OFF
REM Start OGEMA using felix as OSGi framework.
setlocal
set LAUNCHER=ogema-launcher.jar
set CONFIG=config/config.xml
set PROPERTIES=config/ogema.properties
REM set OPTIONS="%OPTIONS% -clean"

set EXTENSIONS=bin\ext;bin\ext\jssc-2.8.0.jar;bin\ext\libusb4java-1.2.0-linux-arm.jar;bin\ext\libusb4java-1.2.0-linux-x86.jar;bin\ext\libusb4java-1.2.0-linux-x86_64.jar;bin\ext\libusb4java-1.2.0-windows-x86.jar;bin\ext\libusb4java-1.2.0-windows-x86_64.jar;bin\ext\usb-api-osgi-1.0.2.jar;bin\ext\usb4java-1.2.0.jar;bin\ext\usb4java-javax-1.2.0.jar;bin\ext\zwave4j-0.6-SNAPSHOT.jar
set OGEMA_CLASSPATH=%LAUNCHER%;%EXTENSIONS%
set VMOPTS=%VMOPTS% -cp %OGEMA_CLASSPATH%
set JAVA=java

REM find out java version; in case of java 9 add required modules
REM see https://stackoverflow.com/questions/17714681/get-java-version-from-batch-file
for /f tokens^=2-5^ delims^=.+-_^" %%j in ('%JAVA% -fullversion 2^>^&1') do @set "jver=%%j%%k%%l%%m"
if %jver:~0,1%==1 (
	@set "jver=%jver:~1,1%"
) else (
	@set "jver=%jver:~0,1%"
)
if %jver%==9 @set "JAVA=%JAVA% --add-modules java.xml.bind"

set FIRST=true
:loop
%JAVA% %VMOPTS% org.ogema.launcher.OgemaLauncher ^
  --config %CONFIG% --properties %PROPERTIES% ^
  %OPTIONS% %*
set /a RETURNCODE=%errorlevel%
if %FIRST%==true (
    @set "OPTIONS=%OPTIONS% -restart"
)
set FIRST=false
if %RETURNCODE%==-4 goto :loop
endlocal
