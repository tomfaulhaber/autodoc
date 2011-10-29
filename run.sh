#!/bin/sh

file=$1

if [ $# -lt 2 ]; then
    commit=true
else
    commit=$2
fi

jar=`ls -t autodoc*-standalone.jar |head -1`

if [ -d params/$file ]
then
    java -jar $jar --param-dir=params/$file --commit?=$commit
else
    java -jar $jar --param-file=params/contrib.clj --param-key=$file --commit?=$commit
fi
