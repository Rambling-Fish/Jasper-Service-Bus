#!/bin/bash

J_PID=""
UDE_PID_FILE="jsb-core/bin/jsb.java.pid"
JW_PID=""
UDEwrapper_PID_FILE="jsb-core/bin/jsbWrapper.pid"
M_PID=""
UNAME=`uname`
OS="$UNAME"
UDE=""
DTA=""

function get_j_pid {
    if [ -e "$UDE_PID_FILE" ]
     then 
        J_PID=`cat "$UDE_PID_FILE"`
    fi
    if [ -e "$UDEwrapper_PID_FILE" ]
     then     
        JW_PID=`cat "$UDEwrapper_PID_FILE"`
    fi
}

function get_m_pid {
    M_PID=""
    M_PID=`ps ax | grep mule | grep wrapper.pidfile | cut -d' ' -f1`
    if [ -z "$M_PID" ]
    then
      M_PID=`ps ax | grep mule | grep wrapper.pidfile | cut -d' ' -f2`
      if [ -z "$M_PID" ]
      then
      M_PID=`ps ax | grep mule | grep wrapper.pidfile | cut -d' ' -f3`
      fi
    fi 
}

function stop_j {
   get_j_pid
   if [ -z "$J_PID" ]; then
      echo "UDE is not running." 
   else
      echo "Stopping UDE.."
      cd jsb-core/bin/ 
      ./ude stop
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.1.0/udeAutoStart
      fi
      echo ".. Done."
   fi
}

function stop_m {
   get_m_pid
   if [ -z "$M_PID" ]; then
      echo "DTA Server is not running." 
   else
      echo -n "Stopping DTA Server.."
      cd jsb-core/mule-standalone-3.4.0/bin
      ./mule stop
      cd ../../../
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.1.0/dtaAutoStart
      fi 
      sleep 1
      echo ".. Done."
   fi
}

function force_stop_j {
   get_j_pid
   if [ -z "$J_PID" ]; then
      echo "UDE is not running." 
   else
      echo "Stopping UDE.."
      kill $J_PID 
      sleep 1
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.1.0/udeAutoStart
      fi
      rm $UDE_PID_FILE $UDEWrapper_PID_FILE
      echo ".. Done."
   fi
}

function force_stop_m {
   get_m_pid
   if [ -z "$M_PID" ]; then
      echo "DTA Server is not running." 
   else
      echo -n "Stopping DTA Server.."
      kill $M_PID
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.1.0/dtaAutoStart
      fi 
      sleep 1
      echo ".. Done."
   fi
}

function start_j {
   if ! [ -x jsb-core/bin/ude ]; then
      UDE='y'
      display_warning
      exit 0
   fi
   get_j_pid
  if [ -z "$J_PID" ]; then
      cd jsb-core/bin
      ./ude start
      cd ../../
      if [ "$OS" == 'Linux' ]; then
         if [ ! -L /opt/jasper/jasper-2.1.0/udeAutoStart ]; then
            ln -s /opt/jasper/jasper-2.1.0/jsb-core/udeAutoStart /opt/jasper/jasper-2.1.0/udeAutoStart
         fi
      fi
      else
      echo "UDE is already running, PID=$J_PID"
   fi
}

function start_m {
   if ! [ -d jsb-core/mule-standalone-3.4.0 ]; then
      DTA='y'
      display_warning
      exit 0
  fi
   get_m_pid
if [ -z "$M_PID" ]; then
      echo  "Starting DTA Server.."
      cd jsb-core/mule-standalone-3.4.0/bin
      ./mule start
      cd ../../../
      sleep 5
      get_m_pid
      echo "Done. PID=$M_PID" 
      if [ "$OS" == 'Linux' ]; then
         if [ ! -L /opt/jasper/jasper-2.1.0/dtaAutoStart ]; then
            ln -s /opt/jasper/jasper-2.1.0/jsb-core/dtaAutoStart /opt/jasper/jasper-2.1.0/dtaAutoStart
         fi
      fi     
   else
      echo "DTA Server is already running, PID=$M_PID"
   fi
}

function status_j {
   get_j_pid
   if [ -z  "$J_PID" ]; then
      echo "UDE is not running." 
   else
      echo "UDE is running, wrapper PID=$JW_PID process PID=$J_PID"
   fi
}

function status_m {
   get_m_pid
   if [ -z  "$M_PID" ]; then
      echo "DTA Server is not running." 
   else
      echo "DTA Server is running, PID=$M_PID"
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

function force_stop {
    force_stop_m
    sleep 5
    force_stop_j
    exit 1
}

function status {
    status_j
    status_m
    exit 1
}

function display_warning {
      echo ""
      echo "***********************************************"
      if [ "$UDE" == 'y' ]; then
         echo "***       Warning: UDE not setup            ***"
      else
         if [ "$DTA" == 'y' ]; then
            echo "***       Warning: DTA not setup            ***"
         fi
      fi
      echo "***     To run UDE server only:             ***"
      echo "***     './setup.sh ude'                    ***"
      echo "***     'jasper.sh start ude'               ***"
      echo "***                                         ***"
      echo "***     To run DTA server only:             ***"
      echo "***     './setup.sh dta'                    ***"
      echo "***     'jasper.sh start dta'               ***"
      echo "***                                         ***"
      echo "***     To run both:                        ***"
      echo "***     './setup.sh all'                    ***"
      echo "***     'jasper.sh start'                   ***"
      echo "***********************************************"
      echo ""
      exit 0
}

case "$1" in
    start)
        case "$2" in
        ude)
            start_j
        ;;
        dta)
            start_m
        ;;
        *)
            start
        ;;
        esac
    ;;            
    stop)
        case "$2" in
        ude)
            stop_j
        ;;
        dta)
            stop_m
        ;;
        *)
            stop
        ;;
        esac
    ;;
    kill)
        case "$2" in
        ude)
            force_stop_j
        ;;
        dta)
            force_stop_m
        ;;
        *)
            force_stop
        ;;
        esac
    ;;
    status)
        case "$2" in
        ude)
            status_j
        ;;
        dta)
            status_m
        ;;
        *)
            status
        ;;
        esac
    ;;
    *) echo "Usage: $0 {start|stop|kill|status} [ude|dta]"
;;    
esac
