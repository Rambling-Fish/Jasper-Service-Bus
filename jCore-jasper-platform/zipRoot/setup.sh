#!/bin/sh
mkdir jApps
mkdir logs
echo installing mule ESB server 
cd jCore
tar xvfz mule-standalone-3.3.0.tar.gz
rm mule-standalone-3.3.0.tar.gz
chmod 755 jasperEngineStart.sh
cd ..
echo cpoying Jasper Applications to jApps folder
cp -R ../jApps/jApp-management jCore/mule-standalone-3.3.0/apps/
rm -R ../jApps/jApp-management
cp ../jApps/*.* jApps/
cp ../jasper-management.tar.gz ./
echo Installing jasper-management console 
tar -pxvzf jasper-management.tar.gz
rm jasper-management.tar.gz
echo setup operation is completed
echo Now you run ./jasper.sh start - to start Jasper Engine server
echo for jApps deployment run ./jAppManagement.sh and follow the menu options
