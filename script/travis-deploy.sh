#!/bin/bash
set -ev

VERSION=`perl -npe "s/version in ThisBuild\s+:=\s+\"(.*)\"/\1/" version.sbt | sed -e "/^$/d"`

# Deploy a snapshot version only for master branch and jdk8
if [[ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ]]; then 
  if [[ "$TRAVIS_PULL_REQUEST" == "false" ]] && [[ "$VERSION" == *SNAPSHOT ]]; then
    ./sbt "; test; publish";
  else
    ./sbt test;
  fi;
else
  ./sbt test;
fi;
