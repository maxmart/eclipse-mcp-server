@echo off
setlocal
cd /d "%~dp0"

set /p VERSION=<..\VERSION
set JAR_NAME=eclipse.mcp.server_%VERSION%.jar
set DROPINS=C:\Users\max\eclipse\java-2023-06\eclipse\dropins
set PLUGINS=C:\Users\max\.p2\pool\plugins

echo Compiling (target Java 17)...
if not exist bin mkdir bin
javac --release 17 --add-modules jdk.httpserver -d bin -sourcepath src ^
  -cp "%PLUGINS%\org.eclipse.osgi_3.19.0.v20240213-1246.jar;%PLUGINS%\org.eclipse.equinox.common_3.19.0.v20240214-0846.jar;%PLUGINS%\org.eclipse.core.runtime_3.31.0.v20240215-1631.jar;%PLUGINS%\org.eclipse.core.resources_3.20.100.v20240209-1706.jar;%PLUGINS%\org.eclipse.core.jobs_3.15.200.v20231214-1526.jar;%PLUGINS%\org.eclipse.debug.core_3.21.300.v20240109-1022.jar;%PLUGINS%\org.eclipse.debug.ui_3.18.300.v20240213-1843.jar;%PLUGINS%\org.eclipse.ui_3.205.100.v20240131-1023.jar;%PLUGINS%\org.eclipse.ui.console_3.14.0.v20240129-1403.jar;%PLUGINS%\org.eclipse.ui.workbench_3.131.100.v20240221-2107.jar;%PLUGINS%\org.eclipse.jface_3.33.0.v20240214-1640.jar;%PLUGINS%\org.eclipse.swt_3.125.0.v20240227-1638.jar;%PLUGINS%\org.eclipse.swt.win32.win32.x86_64_3.125.0.v20240227-1638.jar;%PLUGINS%\com.google.gson_2.10.1.v20230109-0753.jar;%PLUGINS%\org.eclipse.jface.text_3.25.0.v20240207-1054.jar;%PLUGINS%\org.eclipse.text_3.14.0.v20240207-1054.jar" ^
  src\eclipse\mcp\*.java src\eclipse\mcp\tools\*.java
if errorlevel 1 (
    echo ERROR: Compilation failed.
    exit /b 1
)

echo Building plugin JAR (v%VERSION%)...
jar cfm "%JAR_NAME%" META-INF\MANIFEST.MF -C bin . plugin.xml
echo Created %JAR_NAME%

echo.
echo Copying to dropins: %DROPINS%
if not exist "%DROPINS%" mkdir "%DROPINS%"
copy /Y "%JAR_NAME%" "%DROPINS%\"
echo.
echo Done! Restart Eclipse to load the plugin.
