@echo off
set JAVA_HOME=C:\Users\iamas\openjdk-21\jdk-21.0.12+8
set PATH=%JAVA_HOME%\bin;%PATH%
call gradlew.bat :web:wasmJsBrowserDevelopmentRun %*
