#!/bin/bash

# Do the raw setup for a new clojure-contirb library in the 1.3 world

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` repo-name"
  exit 1
fi

repo=$1

# Create the new directory
cd /home/tom/src/clj/autodoc-work-area/
mkdir $repo
cd $repo

# Clone the github repo into two separate directories
hub clone clojure/$repo src
git clone git@github.com:clojure/$repo.git autodoc

# Now turn the autodoc directory into a new gh-pages branch
cd autodoc/
git symbolic-ref HEAD refs/heads/gh-pages
rm .git/index
git clean -fdx
