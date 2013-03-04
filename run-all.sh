#!/bin/bash

projects="clojure incanter"
contribs="algo.generic algo.monads core.cache core.incubator core.match core.memoize core.unify data.codec data.csv data.finger-tree data.json data.priority-map data.xml data.zip java.classpath java.data java.jdbc java.jmx math.combinatorics math.numeric-tower test.generative tools.cli tools.logging tools.macro tools.namespace tools.trace tools.nrepl"

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
