#!/bin/sh

JASPER_ENGINE_CLASSPATH=jars/*:3rdParty/*
JASPER_ENGINE_PROPERTY_FILE=config/jCore-engine.properties
JASPER_ENGINE_LOG4J_xml=config/jCore-engine-log4j.xml
JASPER_ENGINE_MAIN_CLASS=org.jasper.jCore.engine.JECore


$JAVA_HOME/bin/java -ea -DjCore-engine-property-file=$JASPER_ENGINE_PROPERTY_FILE -DjCore-engine-log4j-xml=$JASPER_ENGINE_LOG4J_xml -classpath $JASPER_ENGINE_CLASSPATH $JASPER_ENGINE_MAIN_CLASS
