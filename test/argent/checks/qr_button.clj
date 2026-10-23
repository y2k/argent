(ns argent.checks.qr_button
  (:require [argent.vendor.view_node :as node]
            [argent.vendor.effect :as fx]
            [argent.vendor.event :as event]
            [argent.checks.support :as t]))

(defn run []
  (let [picked (atom [])
        received (atom [])
        env (-> {:context :current}
                (fx/register :qr:pick
                             (fn [listener]
                               (swap! picked (fn [xs] (conj xs listener)))))
                (event/register :received
                                (fn [value]
                                  (fn [w]
                                    (swap! received
                                           (fn [xs] (conj xs (assoc value :context (:context w)))))))))
        original [:qr-image-button
                  {:text "Read QR" :description "Scan QR"
                   :onresult (fn [result] (event/dispatch :received {:listener :first :result result}))}]
        updated [:qr-image-button
                 {:text "Read again" :description "Scan QR"
                  :onresult (fn [result] (event/dispatch :received {:listener :updated :result result}))}]
        first-button (node/expand original)
        next-button (node/expand updated)
        first-click ((get-in first-button [1 :onclick]))
        next-click ((get-in next-button [1 :onclick]))]
    (t/check "qr button: deferred pick" (= (count (deref picked)) 0))
    (t/check "qr button: deferred listener" (= (count (deref received)) 0))
    (t/check "qr button: expanded tag" (= (get first-button 0) :button))
    (t/check "qr button: text" (= (get-in first-button [1 :text]) "Read QR"))
    (t/check "qr button: updated text" (= (get-in next-button [1 :text]) "Read again"))
    (t/check "qr button: description" (= (get-in next-button [1 :description]) "Scan QR"))
    (first-click env)
    (next-click (assoc env :context :replacement))
    (t/check "qr button: picks" (= (count (deref picked)) 2))
    (t/check "qr button: pending results" (= (count (deref received)) 0))
    ((get (deref picked) 0) {:err nil :value "{\"id\":123}"})
    ((get (deref picked) 1) {:err :cancelled :value nil})
    (t/check "qr button: results" (= (count (deref received)) 2))
    (t/check "qr button: first listener" (= (get-in (deref received) [0 :listener]) :first))
    (t/check "qr button: first context" (= (get-in (deref received) [0 :context]) :current))
    (t/check "qr button: success" (= (get-in (deref received) [0 :result :err]) nil))
    (t/check "qr button: value" (= (get-in (deref received) [0 :result :value]) "{\"id\":123}"))
    (t/check "qr button: updated listener" (= (get-in (deref received) [1 :listener]) :updated))
    (t/check "qr button: updated context" (= (get-in (deref received) [1 :context]) :replacement))
    (t/check "qr button: cancelled" (= (get-in (deref received) [1 :result :err]) :cancelled))
    (t/check "qr button: cancelled value" (= (get-in (deref received) [1 :result :value]) nil)))
  (let [text (node/expand [:text {:text "7"}])]
    (t/check "qr button: plain tag" (= (get text 0) :text))
    (t/check "qr button: plain text" (= (get-in text [1 :text]) "7"))))
