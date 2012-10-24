#!/bin/sh
mkdir JTAs
mkdir logs
cd jsb-core
tar xvfz mule-standalone-3.3.0.tar.gz
rm mule-standalone-3.3.0.tar.gz
chmod 755 jsbStart.sh
