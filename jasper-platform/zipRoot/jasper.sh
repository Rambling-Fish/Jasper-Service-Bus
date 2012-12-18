
#!/bin/bash

J_PID=""
M_PID=""
UNAME=`uname`
OS="$UNAME"
PWD=`pwd`

function get_j_pid {
    J_PID=""
    J_PID=`ps ax | grep java | grep JECore | cut -d' ' -f1`
    if [ -z "$J_PID" ]
    then 
      J_PID=`ps ax | grep java | grep JECore | cut -d' ' -f2`
      if [ -z "$J_PID" ]
      then 
      J_PID=`ps ax | grep java | grep JECore | cut -d' ' -f3`
      fi    
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
      echo -n "Stopping JSB.."
      kill $J_PID 
      sleep 1
      if [ "$OS" == 'Linux' ]; then
         rm /etc/init.d/jsbAutoStart
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
      kill $M_PID
      if [ "$OS" == 'Linux' ]; then
         rm /etc/init.d/jtaStart
      fi 
      sleep 1
      echo ".. Done."
   fi
}

function start_j {
   if ! [ -x jsb-core/jsbStart.sh ]; then
      echo "*** Warning: JSB has not been setup. Run ./setup jsb first"
      exit 0
   fi
   get_j_pid
if [ -z "$J_PID" ]; then
      echo  "Starting JSB.."
      cd jsb-core
      ./jsbStart.sh &
      cd ..
      sleep 5
      get_j_pid
      echo "Done. PID=$J_PID"
      if [ "$OS" == 'Linux' ]; then
         if [ ! -L /etc/init.d/jsbAutoStart ]; then
            ln -s "$PWD"/jsb-core/jsbAutoStart /etc/init.d/jsbAutoStart
         fi
      fi
      else
      echo "JSB is already running, PID=$J_PID"
   fi
}

function start_m {
   if ! [ -d jsb-core/mule-standalone-3.3.0 ]; then
      echo "*** Warning: JTA has not been setup. Run ./setup jta first"
      exit 0
  fi
   get_m_pid
if [ -z "$M_PID" ]; then
      echo  "Starting JTA Server.."
      cd jsb-core/mule-standalone-3.3.0/bin
      ./mule &
      cd ../../../
      sleep 5
      get_m_pid
      echo "Done. PID=$M_PID" 
      if [ "$OS" == 'Linux' ]; then
         if [ ! -L /etc/init.d/jtaStart ]; then
            ln -s "$PWD"/jsb-core/jtaStart /etc/init.d/jtaStart
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
      echo "JSB is running, PID=$J_PID"
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

function status {
    status_j
    status_m
    exit 1
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
    *) echo "Usage: $0 {start|stop|status} [jsb|jta]"
;;    
esac
