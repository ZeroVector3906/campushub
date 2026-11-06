@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" -NoBuild
if errorlevel 1 (
  echo CampusHub startup failed. See .runtime\app.log and .runtime\app-error.log.
  pause
  exit /b 1
)
start "CampusHub" "http://localhost:8080/#/"
