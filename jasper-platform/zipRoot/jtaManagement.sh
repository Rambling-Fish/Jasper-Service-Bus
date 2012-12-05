#!/bin/bash

unzipFiles()
{
    cd JTAs
    find . -maxdepth 1 -name "*.zip" | while read filename; do unzip -o -d "`echo $filename | rev | cut -c 5- | rev | cut -c 3-`" "$filename"; done;
    rm *.zip
    cd ..
}

deploy()
{
    clear
    PS3='Please select JTA to deploy: '
    echo "JTA deployment"
    echo "---------------"
    echo
    JTAList=`ls JTAs`
    
    select CHOICE in ${JTAList[*]} Back
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
            mv JTAs/"$CHOICE" jsb-core/mule-standalone-3.3.0/apps/"$CHOICE" 
            deploy
            break
        ;;
        esac
    done    
}

undeploy()
{
    clear
    PS3='Please select JTA to undeploy: '
    echo "JTA undeployment"
    echo "-----------------"
    echo
    cd jsb-core/mule-standalone-3.3.0/apps/
    JTAList=`find . -maxdepth 1 -name "*-anchor.txt" | rev | cut -c 12- | rev | cut -c 3-`
    cd ../../../
    
    select CHOICE in ${JTAList[*]} Back
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
            cp -r jsb-core/mule-standalone-3.3.0/apps/"$CHOICE" JTAs/"$CHOICE"
            rm jsb-core/mule-standalone-3.3.0/apps/"$CHOICE"-anchor.txt
            undeploy
            break
        ;;
        esac
    done    
}

configure()
{
    clear
    PS3='Please select JTA to configure: '
    echo "JTA configuration"
    echo "------------------"
    echo
    JTAList=`ls JTAs`
    
    select CHOICE in ${JTAList[*]} Back
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
            configure_JTA
            break
        ;;
        esac
    done    
}

configure_JTA()
{
    clear
    PS3='Please select property to configure: '
    
    echo "$CHOICE configuration"
    echo
    echo "Current configuration"
    echo "---------------------"
    FILENAME="JTAs/$CHOICE/mule-app.properties"
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
            echo "Enter new value for $parm: "
            read newValue
            pattern=$parm
            replacement=$newValue
            propvalue=`sed '/^\#/d' $FILENAME | grep $parm | tail -n 1 | sed 's/^.*=//;s/^[[:space:]]*//;s/[[:space:]]*$//'`
            A="`echo | tr '\012' '\001' `"
            sed -i -e "s$A$pattern=$propvalue$A$pattern=$replacement$A" $FILENAME
            rm "$FILENAME-e"
            configure_JTA
            break
        ;;
        esac
    done    
}

configure_global_JasperEngineURL() {
   clear
   echo "Configure jasperEngineURL in all JTAs"
   echo "-------------------------------------"
   echo
   JTAList=`ls JTAs`
   if [ -z "$JTAList" ]; then
      select CHOICE in ${JTAList[*]} Back
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
   echo "Enter new value (format: tcp://host:port): "
   read replacement
   echo "Set jasperEngineURL as $replacement in all JTAs? (y/n/q)"
   read choice
   if [ "$choice" == "n" ];then
      configure_global_JasperEngineURL
   else
      if [ "$choice" == "y" ]; then
         for f in $JTAList
            do 
               FILENAME="JTAs/$f/mule-app.properties"
               echo "Processing $FILENAME"
               propvalue=`sed '/^\#/d' $FILENAME | grep $pattern | tail -n 1 | sed 's/^.*=//;s/^[[:space:]]*//;s/[[:space:]]*$//'`
               A="`echo | tr '\012' '\001' `"
               sed -i -e "s$A$pattern=$propvalue$A$pattern=$replacement$A" $FILENAME
               rm "$FILENAME-e" 
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

menuScreen()
{
    clear
    PS3='Please enter your choice: '
    echo "JTA Management"
    echo "---------------"
    echo
    options=("Deploy JTA" "Undeploy JTA" "Configure JTA" "Configure Jasper Engine URL for all JTAs" "Quit")
    select opt in "${options[@]}"
    do
        case $opt in
            "Deploy JTA")
                deploy
                break
                ;;
            "Undeploy JTA")
                undeploy
                break
                ;;
            "Configure JTA")
                configure
                break
                ;;
            "Configure Jasper Engine URL for all JTAs")
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
menuScreen
clear


