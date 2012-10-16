#!/bin/sh
mkdir jApps
mkdir logs
cd jCore
tar xvfz mule-standalone-3.3.0.tar.gz
rm mule-standalone-3.3.0.tar.gz
chmod 755 jasperEngineStart.sh
