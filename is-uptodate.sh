#!/bin/bash

if [ $# -ne 1 ]
then
    echo "Usage: is-uptodate.sh src-directory"
    exit 1
fi

srcdir=$1

cd $srcdir

commit=`git log -1 | awk '/^commit/ {print $2;}'`
lastcommit=`cat ../last-seen-revision`

if [ "$commit" = "$lastcommit" ]
then
    echo yes
else
    echo no
fi

exit 0
