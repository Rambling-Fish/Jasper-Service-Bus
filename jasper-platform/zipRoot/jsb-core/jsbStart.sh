#!/bin/bash

source config/jmx_properties.sh

JSB_CLASSPATH=jars/*:3rdParty/*
JSB_PROPERTY_FILE=config/jsb.properties
JSB_KEYSTORE=keystore/
JSB_LOG4J_XML=config/jsb-log4j.xml
JSB_MAIN_CLASS=org.jasper.jCore.engine.JECore
JSB_JMXREMOTE_PASSWORD_FILE=config/jsb.jmxremote.password

java -ea -Djsb-property-file=$JSB_PROPERTY_FILE -Djsb-keystore=$JSB_KEYSTORE -Djsb-log4j-xml=$JSB_LOG4J_XML -Djava.rmi.server.hostname=$JSB_JMXREMOTE_HOST -Dcom.sun.management.jmxremote.port=$JSB_JMXREMOTE_PORT -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.password.file=$JSB_JMXREMOTE_PASSWORD_FILE -classpath $JSB_CLASSPATH $JSB_MAIN_CLASS
