(ns argent.vendor.effect)

(defn register [env name f]
  (assoc env name (fn [_env arg] (f arg))))

(defn summon [env key]
  (get env key))

(defn pure [value]
  (fn [_env] value))

(defn next [effect f]
  (fn [env]
    (let [value (effect env)
          next-effect (f value)]
      (next-effect env))))
