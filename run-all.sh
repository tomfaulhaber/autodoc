#!/bin/bash

jar=`ls -t target/autodoc*-standalone.jar |head -1`

projects="clojure incanter"
contribs=$(java -jar $jar --param-file=params/contrib.clj list-keys | tail -n +2)

if [ $# -ge 1 ]; then
    if [ "$1" == "contrib" ]; then
        shift
        projects=""
    fi
fi

if [ $# -lt 1 ]; then 
    commit=true
else
    commit=$1
fi

for project in $projects
do
    echo
    echo "================= Project $project ======================"
    echo
    ./run.sh $project $commit
done

for contrib in $contribs
do
    echo
    echo "================= Project $contrib ======================"
    echo
    ./run.sh $contrib $commit
done
