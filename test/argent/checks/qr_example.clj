(ns argent.checks.qr_example
  (:require [argent.main :as main]
            [argent.vendor.effect :as fx]
            [argent.checks.support :as t]))

(defn run []
  (let [seen (atom [])
        state (atom 7)
        env (-> {}
                (fx/register :ui:toast (fn [text] (swap! seen (fn [xs] (conj xs text)))))
                (fx/register :state:update (fn [f] (swap! state f))))
        node (get (main/screen (deref state)) 4)]
    (t/check "qr example: node" (= (get node 0) :qr-image-button))
    (((get-in node [1 :onresult]) {:err nil :value "{\"id\":123}"}) env)
    (((get-in node [1 :onresult]) {:err :cancelled :value nil}) env)
    (((get-in node [1 :onresult]) {:err :scan-failed :value nil}) env)
    (t/check "qr example: toast count" (= (count (deref seen)) 3))
    (t/check "qr example: original JSON" (= (get (deref seen) 0) "{\"id\":123}"))
    (t/check "qr example: cancelled" (= (get (deref seen) 1) "Сканирование отменено"))
    (t/check "qr example: failed" (= (get (deref seen) 2) "Не удалось завершить сканирование"))
    (t/check "qr example: unchanged state" (= (deref state) 7))))
