@REM Maven Wrapper startup batch script
@REM -------------------------------------------------------------------
@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
set "MAVEN_PROJECTBASEDIR=%~dp0"
@REM Remove trailing backslash
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

"%JAVA_HOME%\bin\java.exe" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
