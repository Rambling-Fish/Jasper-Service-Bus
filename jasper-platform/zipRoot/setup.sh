#!/bin/bash

CDIR=`pwd`

function setup_ude {
mkdir -p logs
cd jsb-core
chmod 755 bin/ude
chmod 755 exec/wrapper*
chmod 755 udeAutoStart
chmod 755 pdpAutoStart
chmod 600 config/jsb.jmxremote.password
mv *.jar libs/
mkdir activemq-data
if [ ! -L exec/activemq-data ]
  then
    cd exec
    ln -s ../activemq-data .
    cd ..
fi
}

function setup_jmc_discovery {
  if [ -e jsb-core/jmp.tar.gz ]; then
    cd $CDIR
    cd ..
    cp $CDIR/jsb-core/jmp.tar.gz .
    tar -xzf jmp.tar.gz
    rm jmp.tar.gz
    cd $CDIR
  fi
}

function setup_dta {
if [ ! -e ../jmp ]; then
  setup_jmc_discovery
fi
mkdir -p DTAs
mkdir -p logs
cd jsb-core
chmod 755 dtaAutoStart
if ! [ -d mule-standalone-3.4.0 ]; then
   tar xvfz mule-standalone-3.4.0.tar.gz
   rm mule-standalone-3.4.0.tar.gz
   tar xvfz default.tar.gz
   mv default mule-standalone-3.4.0/apps/
   rm default.tar.gz
   cd mule-standalone-3.4.0
   ln -s ../keystore keystore
fi
}

function setup_all {
if [ ! -e ../jmp ]; then
  setup_jmc_discovery
fi
mkdir -p DTAs
mkdir -p logs
cd jsb-core
if ! [ -d mule-standalone-3.4.0 ]; then
   tar xvfz mule-standalone-3.4.0.tar.gz
   rm mule-standalone-3.4.0.tar.gz
   tar xvfz default.tar.gz
   mv default mule-standalone-3.4.0/apps/
   rm default.tar.gz
   cd mule-standalone-3.4.0
   ln -s ../keystore keystore
   cd ..
   chmod 600 config/jsb.jmxremote.password
   mv *.jar libs/
fi
chmod 755 bin/ude
chmod 755 exec/wrapper*
chmod 755 udeAutoStart
chmod 755 dtaAutoStart
mkdir activemq-data
if [ ! -L exec/activemq-data ]
  then
    cd exec
    ln -s ../activemq-data .
    cd ..
fi
}

case "$1" in
    ude)
        setup_ude
        ;;
    dta)
        setup_dta
        ;;
    all)
        setup_all
        ;;
    *) echo "Usage: $0 {ude|dta|all}"
;;
esac
