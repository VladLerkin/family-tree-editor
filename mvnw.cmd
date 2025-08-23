@ECHO OFF
setlocal ENABLEDELAYEDEXPANSION

REM ---------------------------------------------------------------------------
REM Maven Wrapper startup script for Windows
REM ---------------------------------------------------------------------------

REM Resolve Java executable
set "JAVA_EXE=java.exe"
if not "%JAVA_HOME%"=="" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

REM Resolve paths
set "WRAPPER_JAR_PATH=%~dp0.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

REM Download maven-wrapper.jar if not present
if not exist "%WRAPPER_JAR_PATH%" (
  for /f "usebackq tokens=1,2 delims==" %%a in ("%WRAPPER_PROPERTIES%") do (
    if "%%a"=="wrapperUrl" set "WRAPPER_URL=%%b"
  )
  if "!WRAPPER_URL!"=="" (
    echo [ERROR] Cannot find wrapperUrl in "%WRAPPER_PROPERTIES%"
    exit /b 1
  )
  echo Downloading Maven Wrapper from !WRAPPER_URL!
  if not exist "%~dp0.mvn\wrapper" (
    mkdir "%~dp0.mvn\wrapper"
  )
  powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $client = New-Object System.Net.WebClient; $client.DownloadFile('!WRAPPER_URL!', '%WRAPPER_JAR_PATH%')"
  if errorlevel 1 (
    echo [ERROR] Failed to download Maven Wrapper JAR
    exit /b 1
  )
)

REM Determine project base directory (strip trailing backslash)
set "MAVEN_PROJECTBASEDIR=%~dp0."
for %%i in ("%MAVEN_PROJECTBASEDIR%") do set "MAVEN_PROJECTBASEDIR=%%~fi"

REM Pass through Maven opts if defined
set "MAVEN_OPTS=%MAVEN_OPTS%"

REM Execute Maven Wrapper
"%JAVA_EXE%" %MAVEN_OPTS% -classpath "%WRAPPER_JAR_PATH%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*

endlocal
