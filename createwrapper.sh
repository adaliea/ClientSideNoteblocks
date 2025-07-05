#!/bin/bash

if [ -z "$1" ]; then
version='8.12'
else
version=$1
fi

rm -rf "$version"
mkdir -p "$version"

mkdir -p gradle/wrapper
pushd gradle/wrapper
wget https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
cat >gradle-wrapper.properties <<EOL
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-${version}-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOL
popd

pushd $version

echo -e "1\n1\n\n" | java -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain init

popd