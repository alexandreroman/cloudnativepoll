#!/bin/sh
set -e -x
VERSION=`cat version/number`
mkdir -p prerelease
cp build/*.jar prerelease
echo "$APP $VERSION" > prerelease/release && \
echo "$CF_MANIFEST" > prerelease/manifest.yml
