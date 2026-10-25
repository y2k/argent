(ns argent.vendor.view_node
  (:require [argent.vendor.effect :as fx]))

(defn expand [node]
  (let [[tag attrs] node
        onresult (:onresult attrs)]
    (if (= tag :qr-image-button)
      [:button (assoc attrs :onclick
                      (fn []
                        (fn [w]
                          ((fx/call :qr:pick
                                    (fn [result] ((onresult result) w))) w))))]
      node)))
