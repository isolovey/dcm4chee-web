@echo off
rem -----------------------------------------------------------------------------------
rem copy DCM4CHEE WEB3 components into DCM4CHEE Archive installation
rem -----------------------------------------------------------------------------------

if "%OS%" == "Windows_NT"  setlocal
set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

set WEB3_HOME=%DIRNAME%..
set WEB3_SERV=%WEB3_HOME%\server\default
set VERS=3.0.1
set WEB3_CHECK=%WEB3_SERV%\lib\dcm4chee-web-urlprovider-%VERS%.jar

if exist "%WEB3_CHECK%" goto found_web3
echo Could not locate %WEB3_CHECK%. Please check that you are in the
echo bin directory when running this script.
goto end

:found_web3
if not [%1] == [] goto found_arg1
echo "Usage: install <path-to-dcm4chee-directory>"
goto end

:found_arg1
set DCM4CHEE_HOME=%1
set DCM4CHEE_SERV=%DCM4CHEE_HOME%\server\default

if exist "%DCM4CHEE_SERV%\lib\dcm4chee.jar" goto found_dcm4chee
echo Could not locate dcm4chee archive in %DCM4CHEE_HOME%.
goto end

:found_dcm4chee
copy "%WEB3_SERV%\lib\*.jar" "%DCM4CHEE_SERV%\lib"

copy "%WEB3_SERV%\deploy\dcm4chee-web-ear-%VERS%-*.ear" "%DCM4CHEE_SERV%\deploy"

copy "%WEB3_SERV%\conf\auditlog\*.xml" "%DCM4CHEE_SERV%\conf\dcm4chee-auditlog"

:end
if "%OS%" == "Windows_NT" endlocal
