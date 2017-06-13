#!/bin/bash

export gdspid=`ps -ef | grep "gdsservice" | grep -v "grep" | awk '{print $2}'`
kill -9 $gdspid
cd /opt/apps/flyhi/GDSServiceauto
rm -rf RUNNING_PID
cd /opt/apps/flyhi/GDSServiceauto/bin
./gdsservice  -Dconfig.file=/opt/apps/flyhi/GDSServiceauto/bin/conf/application.qa.conf > /dev/null 2>&1&
