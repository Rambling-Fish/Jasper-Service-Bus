#!/bin/bash

J_PID=""
JW_PID=""
M_PID=""
UNAME=`uname`
OS="$UNAME"
JSB=""
JTA=""

function get_j_pid {
    J_PID=""
    J_PID=`ps ax | grep java | grep 'JSB Server' | cut -d' ' -f1`
    if [ -z "$J_PID" ]
    then 
      J_PID=`ps ax | grep java | grep 'JSB Server' | cut -d' ' -f2`
      if [ -z "$J_PID" ]
      then 
      J_PID=`ps ax | grep java | grep 'JSB Server' | cut -d' ' -f3`
      fi    
    fi
    if [ -e "jsb-core/bin/jsb.pid" ]
    then
      JW_PID=`cat jsb-core/bin/jsb.pid` 
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
      echo "JSB is not running." 
   else
      echo "Stopping JSB.."
      cd jsb-core/bin/ 
      ./jsb stop
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.0/jsbAutoStart
      fi
      echo ".. Done."
   fi
}

function stop_m {
   get_m_pid
   if [ -z "$M_PID" ]; then
      echo "JTA Server is not running." 
   else
      echo -n "Stopping JTA Server.."
      cd jsb-core/mule-standalone-3.3.0/bin
      ./mule stop
      cd ../../../
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.0/jtaAutoStart
      fi 
      sleep 1
      echo ".. Done."
   fi
}

function force_stop_j {
   get_j_pid
   if [ -z "$J_PID" ]; then
      echo "JSB is not running." 
   else
      echo "Stopping JSB.."
      kill $J_PID 
      sleep 1
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.0/jsbAutoStart
      fi
      echo ".. Done."
   fi
}

function force_stop_m {
   get_m_pid
   if [ -z "$M_PID" ]; then
      echo "JTA Server is not running." 
   else
      echo -n "Stopping JTA Server.."
      kill $M_PID
      if [ "$OS" == 'Linux' ]; then
         rm /opt/jasper/jasper-2.0/jtaAutoStart
      fi 
      sleep 1
      echo ".. Done."
   fi
}

function start_j {
   if ! [ -x jsb-core/bin/jsb ]; then
      JSB='y'
      display_warning
      exit 0
   fi
   get_j_pid
  if [ -z "$J_PID" ]; then
      cd jsb-core/bin
      ./jsb start
      cd ../../
      if [ "$OS" == 'Linux' ]; then
         if [ ! -L /opt/jasper/jasper-2.0/jsbAutoStart ]; then
            ln -s /opt/jasper/jasper-2.0/jsb-core/jsbAutoStart /opt/jasper/jasper-2.0/jsbAutoStart
         fi
      fi
      else
      echo "JSB is already running, PID=$J_PID"
   fi
}

function start_m {
   if ! [ -d jsb-core/mule-standalone-3.3.0 ]; then
      JTA='y'
      display_warning
      exit 0
  fi
   get_m_pid
if [ -z "$M_PID" ]; then
      echo  "Starting JTA Server.."
      cd jsb-core/mule-standalone-3.3.0/bin
      ./mule start
      cd ../../../
      sleep 5
      get_m_pid
      echo "Done. PID=$M_PID" 
      if [ "$OS" == 'Linux' ]; then
         if [ ! -L /opt/jasper/jasper-2.0/jtaAutoStart ]; then
            ln -s /opt/jasper/jasper-2.0/jsb-core/jtaAutoStart /opt/jasper/jasper-2.0/jtaAutoStart
         fi
      fi     
   else
      echo "JTA Server is already running, PID=$M_PID"
   fi
}

function status_j {
   get_j_pid
   if [ -z  "$J_PID" ]; then
      echo "JSB is not running." 
   else
      echo "JSB is running, wrapper PID=$JW_PID proccess PID=$J_PID"
   fi
}

function status_m {
   get_m_pid
   if [ -z  "$M_PID" ]; then
      echo "JTA Server is not running." 
   else
      echo "JTA Server is running, PID=$M_PID"
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
      if [ "$JSB" == 'y' ]; then
         echo "***       Warning: JSB not setup            ***"
      else
         if [ "$JTA" == 'y' ]; then
            echo "***       Warning: JTA not setup            ***"
         fi
      fi
      echo "***     To run JSB server only:             ***"
      echo "***     './setup.sh jsb'                    ***"
      echo "***     'jasper.sh start jsb'               ***"
      echo "***                                         ***"
      echo "***     To run JTA server only:             ***"
      echo "***     './setup.sh jta'                    ***"
      echo "***     'jasper.sh start jta'               ***"
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
        jsb)
            start_j
        ;;
        jta)
            start_m
        ;;
        *)
            start
        ;;
        esac
    ;;            
    stop)
        case "$2" in
        jsb)
            stop_j
        ;;
        jta)
            stop_m
        ;;
        *)
            stop
        ;;
        esac
    ;;
    kill)
        case "$2" in
        jsb)
            force_stop_j
        ;;
        jta)
            force_stop_m
        ;;
        *)
            force_stop
        ;;
        esac
    ;;
    status)
        case "$2" in
        jsb)
            status_j
        ;;
        jta)
            status_m
        ;;
        *)
            status
        ;;
        esac
    ;;
    *) echo "Usage: $0 {start|stop|kill|status} [jsb|jta]"
;;    
esac
