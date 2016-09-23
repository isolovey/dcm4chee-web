#!/bin/sh
# -----------------------------------------------------------------------------------
# copy DCM4CHEE WEB3 components into DCM4CHEE Archive installation
# -----------------------------------------------------------------------------------

DIRNAME=`dirname $0`
WEB3_HOME="$DIRNAME"/..
WEB3_SERV="$WEB3_HOME"/server/default
VERS=3.0.1

if [ x$1 = x ]; then
  echo "Usage: $0 <path-to-dcm4chee-directory>"
  exit 1
fi

DCM4CHEE_HOME="$1"
DCM4CHEE_SERV="$DCM4CHEE_HOME"/server/default

if [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-db2.ear ]; then
  WEB3_DB=db2
elif [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-firebird.ear ]; then
  WEB3_DB=firebird
elif [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-hsql.ear ]; then
  WEB3_DB=hsql
elif [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-mssql.ear ]; then
  WEB3_DB=mssql
elif [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-mysql.ear ]; then
  WEB3_DB=mysql
elif [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-oracle.ear ]; then
  WEB3_DB=oracle
elif [ -f "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-psql.ear ]; then
  WEB3_DB=psql
else
  echo Could not locate dcm4chee-web3 in "$WEB3_HOME"
  exit 1
fi

cp -v "$WEB3_SERV"/deploy/dcm4chee-web-ear-${VERS}-${WEB3_DB}.ear "$DCM4CHEE_SERV"/deploy

cp -v "$WEB3_SERV"/lib/*.jar "$DCM4CHEE_SERV"/lib
cp -v "$WEB3_SERV"/conf/auditlog/*.xml "$DCM4CHEE_SERV"/conf/dcm4chee-auditlog
