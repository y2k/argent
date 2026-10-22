(ns argent.vendor.event
  (:require [argent.vendor.effect :as fx]))

(defn register [env name handler]
  (assoc env name (fn [current-env arg] ((handler arg) current-env))))

(defn dispatch [key arg]
  (fn [env] (let [f (fx/summon env key)]
              (f env arg))))
