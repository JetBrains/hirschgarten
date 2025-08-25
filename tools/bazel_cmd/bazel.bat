@echo off

setlocal

REM Provide VC Runtime for Bazel
set ADDITIONAL_DDL_DIR=%~dp0%PROCESSOR_ARCHITECTURE%
if exist "%ADDITIONAL_DDL_DIR%" set "PATH=%PATH%;%ADDITIONAL_DDL_DIR%"

"%BAZEL_REAL%" %*
exit /b %ERRORLEVEL%
