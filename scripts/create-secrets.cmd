@echo off
REM CMD helper to create docker swarm secret from file
REM Usage: create-secrets.cmd jwt_access_secret .\secrets\jwt_access_secret.txt
if "%~1"=="" (
  echo Usage: %~n0 SECRET_NAME FILE_PATH
  exit /b 1
)
if "%~2"=="" (
  echo Usage: %~n0 SECRET_NAME FILE_PATH
  exit /b 1
)
set SECRET_NAME=%1
set FILE_PATH=%2

echo Creating Docker secret %SECRET_NAME% from %FILE_PATH% ...
docker secret create %SECRET_NAME% %FILE_PATH%
if errorlevel 1 (
  echo Failed to create secret
  exit /b 1
)
echo Created.