#!/bin/bash

unzipFiles()
{
    cd jApps
    find . -maxdepth 1 -name "*.zip" | while read filename; do unzip -o -d "`echo $filename | rev | cut -c 5- | rev | cut -c 3-`" "$filename"; done;
    rm *.zip
    cd ..
}

deploy()
{
    clear
    PS3='Please select jApp to deploy: '
    echo "jApp deployment"
    echo "---------------"
    echo
    jAppList=`ls jApps`
    
    select CHOICE in ${jAppList[*]} Back
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
            mv jApps/"$CHOICE" jCore/mule-standalone-3.3.0/apps/"$CHOICE" 
            deploy
            break
        ;;
        esac
    done    
}

undeploy()
{
    clear
    PS3='Please select jApp to undeploy: '
    echo "jApp undeployment"
    echo "-----------------"
    echo
    cd jCore/mule-standalone-3.3.0/apps/
    jAppList=`find . -maxdepth 1 -name "*-anchor.txt" | rev | cut -c 12- | rev | cut -c 3-`
    cd ../../../
    
    select CHOICE in ${jAppList[*]} Back
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
            cp -r jCore/mule-standalone-3.3.0/apps/"$CHOICE" jApps/"$CHOICE"
            rm jCore/mule-standalone-3.3.0/apps/"$CHOICE"-anchor.txt
            undeploy
            break
        ;;
        esac
    done    
}

configure()
{
    clear
    PS3='Please select jApp to configure: '
    echo "jApp configuration"
    echo "------------------"
    echo
    jAppList=`ls jApps`
    
    select CHOICE in ${jAppList[*]} Back
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
            configure_jApp
            break
        ;;
        esac
    done    
}

configure_jApp()
{
    clear
    PS3='Please select property to configure: '
    
    echo "$CHOICE configuration"
    echo
    echo "Current configuration"
    echo "---------------------"
    FILENAME="jApps/$CHOICE/mule-app.properties"
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
            configure_jApp
            break
        ;;
        esac
    done    
}

menuScreen()
{
    clear
    PS3='Please enter your choice: '
    echo "jApp Management"
    echo "---------------"
    echo
    options=("Deploy jApp" "Undeploy jApp" "Configure jApp" "Quit")
    select opt in "${options[@]}"
    do
        case $opt in
            "Deploy jApp")
                deploy
                break
                ;;
            "Undeploy jApp")
                undeploy
                break
                ;;
            "Configure jApp")
                configure
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


