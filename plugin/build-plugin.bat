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
  -cp "%PLUGINS%\*" ^
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
