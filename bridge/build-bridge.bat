@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\eclipse\mcp\bridge\StdioBridge.java
jar cfe eclipse-mcp-bridge.jar eclipse.mcp.bridge.StdioBridge -C out .
echo Build complete.
echo   JAR: eclipse-mcp-bridge.jar
echo   Run with: java -jar eclipse-mcp-bridge.jar
