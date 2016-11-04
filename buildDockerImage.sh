#!/bin/bash

GRADLE_USER_HOME=cache ./gradlew buildDeb -x test 

if [ ! -z "$1" ]
	then
		dockertag="$1"
	else
		dockertag=latest
fi

docker build -t jmonsinjon/spinnaker-orca:"$dockertag" .