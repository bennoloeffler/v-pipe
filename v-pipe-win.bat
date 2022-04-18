@echo off
set ABSPATH=%CD%
echo starting v-pipe here:
echo %ABSPATH%
cd "%ABSPATH%"
set JAVA_HOME=%ABSPATH%/jre
"%JAVA_HOME%/bin/java" -cp "%ABSPATH%/lib/*" application.MainGui
rem -Dfile.encoding=UTF-8 --- app needs to be compiled with utf-8 (default win = windows-1252)
