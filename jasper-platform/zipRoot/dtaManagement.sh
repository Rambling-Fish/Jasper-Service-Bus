#!/bin/bash

M_PID=""
url_prefix="failover://(tcp://"
url_suffix=")"
comma=","
tcp="tcp://"
str1=""

function get_m_pid 
{
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

unzipFiles()
{
    cd DTAs
    find . -maxdepth 1 -name "*.zip" | while read filename; do unzip -o -d "`echo $filename | rev | cut -c 5- | rev | cut -c 3-`" "$filename"; done;
    rm *.zip
    cd ..
}

deploy()
{
    unzipFiles
    clear
    PS3='Please select DTA to deploy: '
    echo "DTA deployment"
    echo "---------------"
    echo
    DTAList=`ls DTAs`
    LicenseSuffix="jta-license.key"
    LicensePrefix=""
    LicenseFilename=""
    ValidLicense="n"
    get_m_pid 
    if [ -z "$M_PID" ]; then
              echo "Cannot deploy when DTA Server is not running. Use './jasper.sh start dta'"
              read -p "Press any key to continue"
              menuScreen
              break
    fi
    select CHOICE in ${DTAList[*]} Back
    do
        case "$CHOICE" in
        "")
            echo Hit Enter to see menu again!
            continue
            ;;
        Back)
            menuScreen
            break
            ;;
        *)
            #
            #   TODO: New checkLicense function: Get Vendor + AppName + Version + Postfix
            #
            #checkLicense
            #if [[ $ValidLicense != 'y' ]]; then
            #   echo ""
            #   read -p "Cannot deploy $CHOICE - License key file is missing"
            #   deploy
            #   break
            #fi
            echo ""
            echo "$CHOICE is being deployed...please wait"
            sleep 10s
            mv DTAs/"$CHOICE" jsb-core/mule-standalone-3.4.0/apps/"$CHOICE" 
            sleep 5s
            deploy
            break
        ;;
        esac
    done
}

undeploy()
{
    unzipFiles
    clear
    PS3='Please select DTA to undeploy: '
    echo "DTA undeployment"
    echo "-----------------"
    echo
    cd jsb-core/mule-standalone-3.4.0/apps/
    DTAList=`find . -maxdepth 1 -name "*-anchor.txt" | grep -v default-anchor.txt | rev | cut -c 12- | rev | cut -c 3-`
    cd ../../../
    get_m_pid
    if [ -z "$M_PID" ]; then
              echo "Cannot undeploy when DTA Server is not running. Use './jasper.sh start dta'"
              read -p "Press any key to continue"
              menuScreen
              break
    fi
    
    select CHOICE in ${DTAList[*]} Back
    do
        case "$CHOICE" in
        "")
            echo Hit Enter to see menu again!
            continue
            ;;
        Back)
            menuScreen
            break
            ;;
        *)
           echo ""
           echo "$CHOICE is being undeployed...please wait"
           sleep 10s
           cp -rf jsb-core/mule-standalone-3.4.0/apps/"$CHOICE" DTAs/"$CHOICE"
           rm jsb-core/mule-standalone-3.4.0/apps/"$CHOICE"-anchor.txt
           sleep 5s
           undeploy
           break
        ;;
        esac
    done    
}

configure()
{
    clear
    PS3='Please select DTA to configure: '
    echo "DTA configuration"
    echo "------------------"
    echo
    DTAList=`ls DTAs`
    
    select CHOICE in ${DTAList[*]} Back
    do
        case "$CHOICE" in
        "")
            echo Hit Enter to see menu again!
            continue
            ;;
        Back)
            menuScreen
            break
            ;;
        *)
            configure_DTA
            break
        ;;
        esac
    done    
}

configure_DTA()
{
    clear
    PS3='Please select property to configure: '
    
    echo "$CHOICE configuration"
    echo
    echo "Current configuration"
    echo "---------------------"
    FILENAME="DTAs/$CHOICE/mule-app.properties"
    grep -v '#' $FILENAME
    echo
    params=`grep -v '#' $FILENAME | cut -d "=" -f 1`
    
    select parm in ${params[*]} Back
    do
        case "$parm" in
        "")
            echo Hit Enter to see menu again!
            continue
            ;;
        Back)
            configure
            break
            ;;
        *)
            if [ `echo $parm | grep -c "jasperEngineURL" ` -gt 0 ]; then
               echo "Enter new value - format: host1:port1  [host2:port2] ..."
               echo "Host:port pairs must be separated by at least one space and port value must be > 0 and < 65535:"
            else
               echo "Enter new value for $parm: "
            fi
            read newValue
            if echo $newValue | grep ["="] > /dev/null
            then
                read -p "Sorry '=' is not allowed"
                configure_DTA
                break
            fi
            if [ `echo $parm | grep -c  "jasperEngineURL" ` -gt 0 ]; then
               replacement=$newValue
               var=$replacement
               formatURIs
            fi
            pattern=$parm
            if [ `echo $parm | grep -c  "jasperEngineURL" ` -gt 0 ]; then
               : # nothing to do as it should be set
            else
               replacement=$newValue
            fi

            propvalue=`sed '/^\#/d' $FILENAME | grep $parm | tail -n 1 | sed 's/^.*=//;s/^[[:space:]]*//;s/[[:space:]]*$//'`
            A="`echo | tr '\012' '\001' `"
            sed -i -e "s$A$pattern=$propvalue$A$pattern=$replacement$A" $FILENAME
            rm "$FILENAME-e"
            configure_DTA
            break
        ;;
        esac
    done    
}

configure_global_JasperEngineURL() {
   clear
   echo "Configure jasperEngineURL in all DTAs"
   echo "-------------------------------------"
   echo
   DTAList=`ls DTAs`
   if [ -z "$DTAList" ]; then
      select CHOICE in ${DTAList[*]} Back
         do
            case "$CHOICE" in
            Back)
               menuScreen
               break
               ;;
            esac
         done  
   else
   pattern="jasperEngineURL"
   echo "Enter new value - format: host1:port1  [host2:port2] ..."
   echo "Host:port pairs must be separated by at least one space and port value must be > 0 and < 65535:"
   read replacement
   if echo $replacement | grep ["="] > /dev/null
   then
       read -p "Sorry '=' is not allowed"
       configure_global_JasperEngineURL 
       break
   fi

   formatURIs

   echo "Set jasperEngineURL as $replacement in all DTAs? (y/n/q)"
   read choice
   if [ "$choice" == "n" ];then
      configure_global_JasperEngineURL
   else
      if [ "$choice" == "y" ]; then
         for f in $DTAList
            do 
               FILENAME="DTAs/$f/mule-app.properties"
               echo "Processing $FILENAME"
               propvalue=`sed '/^\#/d' $FILENAME | grep $pattern | tail -n 1 | sed 's/^.*=//;s/^[[:space:]]*//;s/[[:space:]]*$//'`
               A="`echo | tr '\012' '\001' `"
               sed -i -e "s$A$pattern=$propvalue$A$pattern=$replacement$A" $FILENAME
               if [ -e "$FILENAME-e" ]; then
                  rm "$FILENAME-e" 
               fi
            done
         echo "Hit any key to return to menu"
         read newChoice
         menuScreen
      else
         menuScreen
      fi
   fi
   fi
}

checkLicense()
{
   LicensePrefix=`echo "$CHOICE" | rev | cut -c 4- | rev`
   LicenseFilename="$LicensePrefix$LicenseSuffix"
   if [ -f DTAs/"$CHOICE"/"$LicenseFilename" ]; then
      ValidLicense='y'
   else
      ValidLicense='n'
  fi
}

formatURIs()
{
   var=$replacement
   set -- $var
   numPairs="$echo ${#@}"
   one=1
   ctr=$((numPairs-one))
   var=($replacement)
   replacement=$url_prefix
   for ((  i = 0 ;  i < $numPairs;  i++  ))
   do
      if [ $ctr -eq 0 ];then
         replacement=$replacement${var[0]}
         break
      fi
      if [ $i -eq 0 ]; then
         replacement=$replacement${var[$i]}$comma
      else if [ $i -lt $ctr ]; then
         replacement+=$tcp${var[$i]}$comma
      else
         replacement+=$tcp${var[$i]}
      fi
      fi
   done
   replacement=$replacement$url_suffix
}


menuScreen()
{
    clear
    PS3='Please enter your choice: '
    echo "DTA Management"
    echo "---------------"
    echo
    options=("Deploy DTA" "Undeploy DTA" "Configure DTA" "Configure Jasper Engine URL for all DTAs" "Quit")
    select opt in "${options[@]}"
    do
        case $opt in
            "Deploy DTA")
                deploy
                break
                ;;
            "Undeploy DTA")
                undeploy
                break
                ;;
            "Configure DTA")
                configure
                break
                ;;
            "Configure Jasper Engine URL for all DTAs")
                configure_global_JasperEngineURL
                break
                ;;
            "Quit")
                break
                ;;
            *) echo invalid option;;
        esac
    done
}

unzipFiles

if [ $# -eq 0 ]; then
   menuScreen
   clear
else
   clear
   if [ $1 = "-l" ]; then
     cd DTAs
     for file in *
        do
           if [ -x "$file" ]; then
              echo "$file"
           fi
        done 
     cd ..
   fi
   if [ $1 = "-ld" ]; then
      cd jsb-core/mule-standalone-3.4.0/apps/
      DTAList=`find . -maxdepth 1 -name "*-anchor.txt" | rev | cut -c 12- | rev | cut -c 3-`
      cd ../../../
      echo "$DTAList"
   fi
   if [ $1 = "-d" ]; then
      if [ -z $2 ]; then
         echo "Usage: $0 -d[eploy] {appName}" 
      else
         mv DTAs/"$2" jsb-core/mule-standalone-3.4.0/apps/"$2"
      fi
   fi
   if [ $1 = "-u" ];then
      if [ -z $2 ]; then
         echo "Usage: $0 -u[ndeploy] {appName}"
      else
         mv jsb-core/mule-standalone-3.4.0/apps/"$2" DTAs 
        rm jsb-core/mule-standalone-3.4.0/apps/"$2-anchor.txt"
      fi
   fi
fi
