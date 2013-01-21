#!/bin/sh

JSB_CLASSPATH=jars/*:3rdParty/*
JSB_PROPERTY_FILE=config/jsb.properties
JSB_KEYSTORE=keystore/
JSB_LOG4J_XML=config/jsb-log4j.xml
JSB_MAIN_CLASS=org.jasper.jCore.engine.JECore


java -ea -Djsb-property-file=$JSB_PROPERTY_FILE -Djsb-keystore=$JSB_KEYSTORE -Djsb-log4j-xml=$JSB_LOG4J_XML -Djava.rmi.server.hostname=0.0.0.0 -Dcom.sun.management.jmxremote.port=1098 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -classpath $JSB_CLASSPATH $JSB_MAIN_CLASS
