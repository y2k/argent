(ns argent.vendor.view_node
  (:require [argent.vendor.event :as event]))

(defn expand [node]
  (let [[tag attrs] node
        onresult (:onresult attrs)]
    (if (= tag :qr-image-button)
      [:button (assoc attrs :onclick
                      (fn []
                        (fn [w]
                          ((event/dispatch :qr:pick
                                           (fn [result] ((onresult result) w))) w))))]
      node)))
