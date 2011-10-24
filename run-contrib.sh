#!/bin/sh

project=$1

if [ $# -lt 2 ]; then 
    commit=true
else
    commit=$2
fi

java -jar autodoc-standalone.jar --param-file=params/contrib.clj --param-key=$project --commit?=$commit
