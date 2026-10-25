(ns argent.vendor.event
  (:require [argent.vendor.effect :as fx]))

(defn register [env name handler]
  (assoc env name handler))

(defn dispatch [key arg]
  (fn [env] (let [f (fx/summon env key)]
              (eprintln (str "Argent op.begin name=" key))
              (let [result ((f arg) env)]
                (eprintln (str "Argent op.end name=" key))
                result))))
