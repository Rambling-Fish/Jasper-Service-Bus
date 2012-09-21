#!/bin/sh
cd jCore
./jasperEngineStart.sh > ../logs/jasperEngine.log &
cd mule-standalone-3.3.0/bin
./mule > ../../../logs/jApps.log &

