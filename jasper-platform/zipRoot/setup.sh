#!/bin/bash

function setup_jsb {
mkdir -p logs
cd jsb-core
chmod 755 jsbStart.sh
}

function setup_jta {
mkdir -p JTAs
mkdir -p logs
cd jsb-core
if ! [ -d mule-standalone-3.3.0 ]; then
   tar xvfz mule-standalone-3.3.0.tar.gz
   rm mule-standalone-3.3.0.tar.gz
fi

}

function setup_all {
mkdir -p JTAs
mkdir -p logs
cd jsb-core
if ! [ -d mule-standalone-3.3.0 ]; then
   tar xvfz mule-standalone-3.3.0.tar.gz
   rm mule-standalone-3.3.0.tar.gz
fi
chmod 755 jsbStart.sh

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
