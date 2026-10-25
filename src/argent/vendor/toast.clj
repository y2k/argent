(ns argent.vendor.toast
  (:require [argent.vendor.effect :as fx])
  (:import [android.content Context]
           [android.widget Toast]))

(defn show [text]
  (fx/call :ui:toast text))

(defn register [env ^Context context]
  (fx/register env :ui:toast
               (fn [text]
                 (.show (Toast/makeText context (cast String text) 0))
                 nil)))
