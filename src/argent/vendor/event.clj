(ns argent.vendor.event
  (:require [argent.vendor.effect :as fx]))

(defn register [env name handler]
  (assoc env name (fn [current-env arg] ((handler arg) current-env))))

(defn dispatch [key arg]
  (fn [env] (let [f (fx/summon env key)]
              (eprintln (str "Argent op.begin name=" key))
              (let [result (f env arg)]
                (eprintln (str "Argent op.end name=" key))
                result))))
