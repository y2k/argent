(ns build
  (:require [make :as make]
            [android-build :as android-build]))

(deps {:make "0.6.0" :android-build "0.3.0"})

(str
 (make/makefile
  {:target "java"
   :dirs [{:path "src"
           :build-dir ".build/bin/app/src/main/java"}]})
 (android-build/build-apk {:package-id "im.y2k.argent"}))
