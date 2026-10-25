(ns argent.vendor.effect)

(defn register [env name f]
  (assoc env name f))

(defn summon [env key]
  (get env key))

(defn call [key arg]
  (fn [env]
    (let [f (summon env key)]
      (eprintln (str "Argent op.begin name=" key))
      (let [result (f arg)]
        (eprintln (str "Argent op.end name=" key))
        result))))

(defn pure [value]
  (fn [_env] value))

(defn next [effect f]
  (fn [env]
    (let [value (effect env)
          next-effect (f value)]
      (next-effect env))))
