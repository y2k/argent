#!/bin/sh
set -eu
serial=${1:?Usage: sh test/qr_component.sh DEVICE_SERIAL}
make build
docker run --rm \
  -v "$PWD/.build/cache:/root/.gradle" \
  -v "$PWD/.build/bin:/target:ro" \
  -v "$PWD/test/android:/checks:ro" \
  -v "$PWD/.build/apk:/output_apk" \
  -e APP_ID=im.y2k.argent \
  --entrypoint sh \
  y2khub/cljdroid@sha256:f7164ab4817da71aa0c6699ae0b62149f9c550f4fac5f01633bc1e9a372de646 \
  -c 'rm -rf /app/app/src/main/java /app/app/src/main/kotlin
      cp -a /target/. /app/
      mkdir -p /app/app/src/androidTest/java/argent
      cp /checks/*.java /app/app/src/androidTest/java/argent/
      cd /app
      ./gradlew assembleDebugAndroidTest --no-daemon --no-watch-fs -q
      cp app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk /output_apk/'
adb -s "$serial" install -r .build/apk/app-debug.apk
adb -s "$serial" install -r .build/apk/app-debug-androidTest.apk
adb -s "$serial" shell am instrument -w -r \
  -e class argent.QrButtonTest im.y2k.argent.test/androidx.test.runner.AndroidJUnitRunner \
  > .build/qr-component-result.txt
cat .build/qr-component-result.txt
grep -q 'OK (1 test)' .build/qr-component-result.txt
