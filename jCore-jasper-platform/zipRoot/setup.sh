#!/bin/sh
mkdir jApps
mkdir logs
cd jCore
tar xvfz mule-standalone-3.3.0.tar.gz
rm mule-standalone-3.3.0.tar.gz
chmod 755 jasperEngineStart.sh
cd ..
cp -R ../jApps/jApp-management jCore/mule-standalone-3.3.0/apps/
rm -R ../jApps/jApp-management
cp ../jApps/*.* jApps/
cp ../jasper-management.tar.gz ./
tar -pxvzf jasper-management.tar.gz
rm jasper-management.tar.gz