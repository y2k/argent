(ns argent.vendor.toast
  (:require [argent.vendor.effect :as fx])
  (:import [android.app Activity]
           [android.widget Toast]))

(defn show [text]
  (fn [env]
    ((fx/summon env :ui:toast) env text)))

(defn register [env ^Activity activity]
  (fx/register env :ui:toast
               (fn [text]
                 (.show (Toast/makeText activity (cast String text) 0))
                 nil)))
