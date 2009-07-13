#!/bin/bash

if [ $# -ne 1 ]
then
    echo "Usage: update-last.sh src-directory"
    exit 1
fi

srcdir=$1

cd $srcdir

commit=`git log -1 | awk '/^commit/ {print $2;}'`
lastcommit=`cat ../last-seen-revision`

echo $commit > ../last-seen-revision

exit 0
