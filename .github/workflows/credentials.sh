#!/bin/sh

fatal()
{
  echo "credentials.sh: fatal: $1" 1>&2
  exit 1
}

error()
{
  echo "credentials.sh: error: $1" 1>&2
  exit 1
}

FAILED=0
if [ -z "${IO7M_ANDROID_KEYSTORE}" ]
then
  error "IO7M_ANDROID_KEYSTORE is not defined"
  FAILED=1
fi
if [ -z "${IO7M_ANDROID_KEYSTORE_PASSWORD}" ]
then
  error "IO7M_ANDROID_KEYSTORE_PASSWORD is not defined"
  FAILED=1
fi

if [ ${FAILED} -eq 1 ]
then
  fatal "One or more required variables are not defined."
fi

echo "${IO7M_ANDROID_KEYSTORE}" | base64 -d > io7m.keystore ||
  fatal "Could not decode keystore"

mkdir -p "${HOME}/.gradle" ||
  fatal "Could not create ${HOME}/.gradle"

(cat <<EOF
com.io7m.storePassword=${IO7M_ANDROID_KEYSTORE_PASSWORD}
com.io7m.keyPassword=${IO7M_ANDROID_KEYSTORE_PASSWORD}
com.io7m.keyAlias=io7m
EOF
) >> "${HOME}/.gradle/gradle.properties" ||
  fatal "Could not write keystore properties"
