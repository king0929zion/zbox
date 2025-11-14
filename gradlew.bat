@ECHO OFF

SETLOCAL

set DIR=%~dp0
IF "%DIR%"=="" SET DIR=.
SET APP_BASE_NAME=%~n0
SET APP_HOME=%DIR%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

IF NOT "%JAVA_HOME%"=="" GOTO findJavaFromJavaHome

set JAVA_EXE=java
%JAVA_EXE% -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 GOTO execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the

echo location of your Java installation.

GOTO fail

:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

IF EXIST "%JAVA_EXE%" GOTO execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the

echo location of your Java installation.

echo.

GOTO fail

:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:END
ENDLOCAL
exit /b 0

:fail
ENDLOCAL
exit /b 1
