(ns build-test
  (:require [make :as make]))

(deps {:make "0.6.0"})

(make/makefile
 {:target "java"
  :dirs [{:path "src" :build-dir ".build/unit/src"}
         {:path "test" :build-dir ".build/unit/src"}]})
