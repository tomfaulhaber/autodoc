#!/bin/bash

contribs="data.json java.classpath java.jdbc tools.logging tools.logging tools.macros tools.nrepl"
projects="clojure incanter ${contribs}"

if [ $# -lt 1 ]; then 
    commit=true
else
    commit=$1
fi

for project in $projects
do
    ./run.sh $project $commit
done
