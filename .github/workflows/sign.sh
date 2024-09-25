#!/bin/sh

fatal()
{
  echo "sign.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "sign.sh: info: $1" 1>&2
}

if [ $# -ne 1 ]
then
  fatal "usage: apk"
fi

APK_FILE="$1"
shift

FAILED=0
if [ -z "${IO7M_ANDROID_SIGNING_KEY}" ]
then
  error "IO7M_ANDROID_SIGNING_KEY is not defined"
  FAILED=1
fi
if [ -z "${IO7M_ANDROID_KEY_ALIAS}" ]
then
  error "IO7M_ANDROID_KEY_ALIAS is not defined"
  FAILED=1
fi
if [ -z "${IO7M_ANDROID_KEY_PASSWORD}" ]
then
  error "IO7M_ANDROID_KEY_PASSWORD is not defined"
  FAILED=1
fi

if [ ${FAILED} -eq 1 ]
then
  fatal "One or more required variables are not defined."
fi

echo "${IO7M_ANDROID_SIGNING_KEY}" | base64 -d > com.io7m.jks ||
  fatal "Could not unpack keystore"

info "Signing ${APK_FILE} and writing to signed.apk"

export PATH="${PATH}:/usr/local/lib/android/sdk/build-tools/33.0.0/"

exec apksigner \
  sign \
  --ks com.io7m.jks \
  --ks-key-alias "${IO7M_ANDROID_KEY_ALIAS}" \
  --ks-pass pass:"${IO7M_ANDROID_KEY_PASSWORD}" \
  --out signed.apk \
  --key-pass pass:"${IO7M_ANDROID_KEY_PASSWORD}" \
  "${APK_FILE}"
