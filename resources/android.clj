(ns android
  (:require [android-build :as android]))

(deps {:android-build "0.3.0"})

(android/dependencies ["com.google.android.flexbox:flexbox:3.0.0"
                       "com.google.android.gms:play-services-code-scanner:16.1.0"
                       "com.google.android.gms:play-services-base:18.5.0"])
