@echo off
setlocal
cd /d "%~dp0"

set /p VERSION=<..\VERSION
set JAR_NAME=eclipse.mcp.server_%VERSION%.jar
set DROPINS=C:\Users\max\eclipse\java-2023-06\eclipse\dropins

echo Building plugin JAR (v%VERSION%)...
if not exist bin (
    echo ERROR: bin directory not found. Open the project in Eclipse first so it compiles.
    exit /b 1
)

jar cfm "%JAR_NAME%" META-INF\MANIFEST.MF -C bin . plugin.xml
echo Created %JAR_NAME%

echo.
echo Copying to dropins: %DROPINS%
if not exist "%DROPINS%" mkdir "%DROPINS%"
copy /Y "%JAR_NAME%" "%DROPINS%\"
echo.
echo Done! Restart Eclipse to load the plugin.
