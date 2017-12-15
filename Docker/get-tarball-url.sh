#!/bin/sh

REPO=$1
TARGET=$2

if [ -z $TARGET ] ; then
    echo $(curl -s https://api.github.com/repos/$REPO/releases/latest | grep tarball_url | cut -d\" -f4)
else
    echo "https://api.github.com/repos/$REPO/tarball/$TARGET"
fi
