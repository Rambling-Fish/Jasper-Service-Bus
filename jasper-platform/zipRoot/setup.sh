#!/bin/bash

function setup_jsb {
mkdir -p logs
cd jsb-core
chmod 755 bin/jsb
chmod 755 jsbAutoStart
chmod 600 config/jsb.jmxremote.password
mv wrapper.jar libs/
}

function setup_jta {
mkdir -p JTAs
mkdir -p logs
cd jsb-core
chmod 755 jtaAutoStart
if ! [ -d mule-standalone-3.3.0 ]; then
   tar xvfz mule-standalone-3.3.0.tar.gz
   rm mule-standalone-3.3.0.tar.gz
   tar xvfz default.tar.gz
   mv default mule-standalone-3.3.0/apps/
   rm default.tar.gz
fi

}

function setup_all {
mkdir -p JTAs
mkdir -p logs
cd jsb-core
if ! [ -d mule-standalone-3.3.0 ]; then
   tar xvfz mule-standalone-3.3.0.tar.gz
   rm mule-standalone-3.3.0.tar.gz
   tar xvfz default.tar.gz
   mv default mule-standalone-3.3.0/apps/
   rm default.tar.gz
   chmod 600 config/jsb.jmxremote.password
   mv wrapper.jar libs/
fi
chmod 755 bin/jsb
chmod 755 jsbAutoStart
chmod 755 jtaAutoStart

}

case "$1" in
    jsb)
        setup_jsb
        ;;
    jta)
        setup_jta
        ;;
    all)
        setup_all
        ;;
    *) echo "Usage: $0 {jsb|jta|all}"
;;    
esac
