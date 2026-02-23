@echo off
setlocal
cd /d "%~dp0"

set /p VERSION=<..\VERSION
set JAR_NAME=eclipse.mcp.server_%VERSION%.jar
set DROPINS=C:\Users\max\eclipse2025\dropins
set PLUGINS=C:\Users\max\eclipse2025\plugins

echo Compiling (target Java 17)...
if not exist bin mkdir bin
javac --release 17 --add-modules jdk.httpserver -d bin -sourcepath src ^
  -cp "%PLUGINS%\org.eclipse.osgi_3.24.0.v20251126-0427.jar;%PLUGINS%\org.eclipse.equinox.common_3.20.300.v20251111-0312.jar;%PLUGINS%\org.eclipse.core.runtime_3.34.100.v20251111-1421.jar;%PLUGINS%\org.eclipse.core.resources_3.23.100.v20251106-1705.jar;%PLUGINS%\org.eclipse.core.jobs_3.15.700.v20250725-1147.jar;%PLUGINS%\org.eclipse.debug.core_3.23.200.v20251107-0507.jar;%PLUGINS%\org.eclipse.debug.ui_3.19.100.v20251114-0802.jar;%PLUGINS%\org.eclipse.ui_3.207.400.v20251015-1301.jar;%PLUGINS%\org.eclipse.ui.console_3.15.0.v20251113-1013.jar;%PLUGINS%\org.eclipse.ui.workbench_3.137.0.v20251114-0005.jar;%PLUGINS%\org.eclipse.jface_3.38.100.v20251108-1551.jar;%PLUGINS%\org.eclipse.swt_3.132.0.v20251124-0642.jar;%PLUGINS%\org.eclipse.swt.win32.win32.x86_64_3.132.0.v20251124-0642.jar;%PLUGINS%\com.google.gson_2.13.2.jar;%PLUGINS%\org.eclipse.jface.text_3.29.0.v20251112-0859.jar;%PLUGINS%\org.eclipse.text_3.14.500.v20251103-0730.jar" ^
  src\eclipse\mcp\*.java src\eclipse\mcp\tools\*.java
if errorlevel 1 (
    echo ERROR: Compilation failed.
    exit /b 1
)

echo Building plugin JAR (v%VERSION%)...
jar cfm "%JAR_NAME%" META-INF\MANIFEST.MF -C bin . plugin.xml
echo Created %JAR_NAME%

echo.
echo Removing old versions from dropins...
del /Q "%DROPINS%\eclipse.mcp.server_*.jar" 2>nul

echo Copying to dropins: %DROPINS%
if not exist "%DROPINS%" mkdir "%DROPINS%"
copy /Y "%JAR_NAME%" "%DROPINS%\"
echo.
echo Done! Restart Eclipse to load the plugin.
