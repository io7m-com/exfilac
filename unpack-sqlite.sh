#!/bin/sh -ex

if [ $# -ne 1 ]
then
  echo "usage: version"
  exit 1
fi

VERSION="$1"
shift

SOURCE="https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/${VERSION}/sqlite-jdbc-${VERSION}.jar"

rm -rfv natives
mkdir -p natives
wget -c -O sqlite.jar "${SOURCE}"

unzip -d natives sqlite.jar org/sqlite/native/Linux-Android/x86_64/libsqlitejdbc.so
unzip -d natives sqlite.jar org/sqlite/native/Linux-Android/arm/libsqlitejdbc.so
unzip -d natives sqlite.jar org/sqlite/native/Linux-Android/x86/libsqlitejdbc.so
unzip -d natives sqlite.jar org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so

mkdir -p com.io7m.exfilac.main/src/main/jniLibs/x86_64/
mkdir -p com.io7m.exfilac.main/src/main/jniLibs/x86/
mkdir -p com.io7m.exfilac.main/src/main/jniLibs/armeabi/
mkdir -p com.io7m.exfilac.main/src/main/jniLibs/arm64-v8a/

mv natives/org/sqlite/native/Linux-Android/x86_64/libsqlitejdbc.so com.io7m.exfilac.main/src/main/jniLibs/x86_64/
mv natives/org/sqlite/native/Linux-Android/x86/libsqlitejdbc.so com.io7m.exfilac.main/src/main/jniLibs/x86/
mv natives/org/sqlite/native/Linux-Android/arm/libsqlitejdbc.so com.io7m.exfilac.main/src/main/jniLibs/armeabi/
mv natives/org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so com.io7m.exfilac.main/src/main/jniLibs/arm64-v8a/

