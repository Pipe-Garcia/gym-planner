@echo off
setlocal
set BASE_DIR=%~dp0
set WRAPPER_PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
  echo Missing Maven wrapper properties: "%WRAPPER_PROPERTIES%" 1>&2
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_PROPERTIES%") do (
  if "%%A"=="distributionUrl" set DISTRIBUTION_URL=%%B
)

if "%DISTRIBUTION_URL%"=="" (
  echo distributionUrl is not configured in "%WRAPPER_PROPERTIES%" 1>&2
  exit /b 1
)

for %%A in ("%DISTRIBUTION_URL%") do set ARCHIVE_NAME=%%~nxA
set MAVEN_VERSION=%ARCHIVE_NAME:apache-maven-=%
set MAVEN_VERSION=%MAVEN_VERSION:-bin.zip=%
if "%MAVEN_USER_HOME%"=="" (
  set WRAPPER_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%
) else (
  set WRAPPER_DIR=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%
)
set ARCHIVE=%WRAPPER_DIR%\%ARCHIVE_NAME%
set MAVEN_DIR=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -Command "New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ARCHIVE%'; Expand-Archive -Force '%ARCHIVE%' '%WRAPPER_DIR%'"
  if errorlevel 1 exit /b 1
)

"%MAVEN_DIR%\bin\mvn.cmd" %*
