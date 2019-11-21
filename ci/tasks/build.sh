#!/bin/sh
set -e -x
VERSION=`cat version/number`
mkdir -p build

cd repo
if [[ -d $PWD/maven && ! -d $HOME/.m2 ]]; then
  ln -s "$PWD/maven" "$HOME/.m2"
fi

cd repo/$MODULE
../mvnw versions:set -DnewVersion=$VERSION && \
../mvnw -B package && \
cp target/*.jar ../../build
