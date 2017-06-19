#!/bin/bash

export gdspid=`ps -ef | grep "gdsservice" | grep -v "grep" | awk '{print $2}'`
kill -9 $gdspid
cd /opt/apps/flyhi/GDSService
rm -rf RUNNING_PID
cd /opt/apps/flyhi/GDSService/bin
#./gdsservice  -Dconfig.file=/opt/apps/flyhi/GDSService/bin/conf/application.qa.conf > /dev/null 2>&1&
./gdsservice -Dhttp.port=9080 -Dconfig.file=/opt/apps/flyhi/GDSService/bin/conf/application.qa.conf  > /dev/null 2>&1&
