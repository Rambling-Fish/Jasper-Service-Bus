#!/bin/sh

JASPER_ENGINE_CLASSPATH=jasperJars/*:3rdParty/*
JASPER_ENGINE_MAIN_CLASS=org.jasper.jCore.engine.JECore

$JAVA_HOME/bin/java -ea -classpath $JASPER_ENGINE_CLASSPATH $JASPER_ENGINE_MAIN_CLASS
