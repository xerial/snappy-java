#!/bin/sh

# Compute sbt-dynver-compatible version number
BUILD_TIME=`date '+%Y%m%d-%H%M'`
DYN_VER=`git describe --long --tags --abbrev=8 --match "v[0-9]*" --always --dirty="-${BUILD_TIME}"`
GIT_DIST=`echo ${DYN_VER} | sed -re "s/v([^-]*)-([0-9]+)-g(.*)/\2/g"`
GIT_TAG=`git describe --tags --dirty`
RELEASE_VERSION=`echo ${DYN_VER} | sed -re "s/v([^-]*)-([0-9]+)-g(.*)/\1/g"`
SNAPSHOT_VERSION=`echo ${DYN_VER} | sed -re "s/v([^-]*)-([0-9]+)-g(.*)/\1-\2-\3/g"`-SNAPSHOT

if [ ${GIT_DIST} -eq 0 ]; then
  if [ ${GIT_TAG} == *"-dirty" ]; then
    VERSION=${SNAPSHOT_VERSION}
  else
    VERSION=${RELEASE_VERSION}
  fi
else
  VERSION=${SNAPSHOT_VERSION}
fi

echo ${VERSION}
