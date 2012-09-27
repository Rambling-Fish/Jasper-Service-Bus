#!/bin/bash

J_PID=""
M_PID=""

function get_j_pid {
    J_PID=`ps ax | grep java | grep JECore | cut -d " " -f 1`
    if [ -z $J_PID ]; then
      J_PID=`ps ax | grep java | grep JECore | cut -d " " -f 2`
    fi
}

function get_m_pid {
    M_PID=`ps ax | grep mule | grep wrapper.pidfile | cut -d " " -f 1`
    if [ -z $M_PID ]; then
      M_PID=`ps ax | grep mule | grep wrapper.pidfile | cut -d " " -f 2`
    fi 
}

function stop_j {
   get_j_pid
   if [ -z $J_PID ]; then
      echo "Jasper Engine is not running." 
   else
      echo -n "Stopping Jasper Engine.."
      kill $J_PID 
      sleep 1
      echo ".. Done."
   fi
}

function stop_m {
   get_m_pid
   if [ -z $M_PID ]; then
      echo "jApps Server is not running." 
   else
      echo -n "Stopping jApps Server.."
      kill $M_PID 
      sleep 1
      echo ".. Done."
   fi
}

function start_j {
   get_j_pid
if [ -z $J_PID ]; then
      echo  "Starting Jasper Engine.."
      cd jCore
      ./jasperEngineStart.sh &
      cd ..
      get_j_pid
      echo "Done. PID=$J_PID"
   else
      echo "Jasper Engine is already running, PID=$J_PID"
   fi
}

function start_m {
   get_m_pid
if [ -z $M_PID ]; then
      echo  "Starting jApps Server.."
      cd jCore/mule-standalone-3.3.0/bin
      ./mule &
      cd ../../../
      get_m_pid
      echo "Done. PID=$M_PID"
   else
      echo "jApps Server is already running, PID=$M_PID"
   fi
}

function status_j {
   get_j_pid
   if [ -z  $J_PID ]; then
      echo "Jasper Engine is not running." 
   else
      echo "Jasper Engine is running, PID=$J_PID"
   fi
}

function status_m {
   get_m_pid
   if [ -z  $M_PID ]; then
      echo "jApps Server is not running." 
   else
      echo "jApps Server is running, PID=$M_PID"
   fi
}

function start {
    start_j
    sleep 5
    start_m
    exit 1
}

function stop {
    stop_m
    sleep 5
    stop_j
    exit 1
}

function status {
    status_j
    status_m
    exit 1
}

case "$1" in
   start)
     start
   ;;
   stop)
     stop
   ;;
   status)
     status
   ;;
   *)
      echo "Usage: $0 {start|stop|status}"
esac
