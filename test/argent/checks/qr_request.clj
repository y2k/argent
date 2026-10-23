(ns argent.checks.qr_request
  (:require [argent.vendor.qr_request :as qr]
            [argent.checks.support :as t]))

(defn delivery []
  (let [ready (atom nil)
        decoded (atom nil)
        scans (atom 0)
        seen (atom [])
        closed (atom 0)
        operation (qr/request
                   {:prepare (fn [next] (reset! ready next))
                    :scan (fn [next]
                            (swap! scans (fn [n] (+ n 1)))
                            (reset! decoded next))
                    :close (fn [] (swap! closed (fn [n] (+ n 1))))}
                   (fn [result]
                     (t/check "qr: close before listener" (= (deref closed) 1))
                     (swap! seen (fn [xs] (conj xs result)))))]
    ((:start operation))
    (t/check "qr: wait for preparation" (= (deref scans) 0))
    ((deref ready))
    ((deref ready))
    (t/check "qr: scan once" (= (deref scans) 1))
    (t/check "qr: wait for result" (= (count (deref seen)) 0))
    ((deref decoded) {:err nil :value "https://example.com/item/123"})
    ((deref decoded) {:err :cancelled :value nil})
    (t/check "qr: deliver once" (= (count (deref seen)) 1))
    (t/check "qr: success" (= (get-in (deref seen) [0 :err]) nil))
    (t/check "qr: original URL" (= (get-in (deref seen) [0 :value]) "https://example.com/item/123"))
    ((:stop operation))
    (t/check "qr: close once" (= (deref closed) 1))))

(defn results []
  (let [seen (atom [])
        closed (atom 0)
        run-scan (fn [value]
                   (let [operation (qr/request
                                    {:prepare (fn [next] (next))
                                     :scan (fn [next] (next value) (next value))
                                     :close (fn [] (swap! closed (fn [n] (+ n 1))))}
                                    (fn [result] (swap! seen (fn [xs] (conj xs result)))))]
                     ((:start operation))
                     ((:stop operation))))]
    (run-scan {:err :cancelled :value nil})
    (t/check "qr: cancel once" (= (count (deref seen)) 1))
    (t/check "qr: result shape" (= (count (get (deref seen) 0)) 2))
    (t/check "qr: cancel status" (= (get-in (deref seen) [0 :err]) :cancelled))
    (t/check "qr: cancel value" (= (get-in (deref seen) [0 :value]) nil))
    (run-scan {:err nil :value "{\"id\":123}"})
    (t/check "qr: restart delivery" (= (count (deref seen)) 2))
    (t/check "qr: restart success" (= (get-in (deref seen) [1 :err]) nil))
    (t/check "qr: original JSON" (= (get-in (deref seen) [1 :value]) "{\"id\":123}"))
    (run-scan {:err :scan-failed :value nil})
    (t/check "qr: error once" (= (count (deref seen)) 3))
    (t/check "qr: error status" (= (get-in (deref seen) [2 :err]) :scan-failed))
    (t/check "qr: error value" (= (get-in (deref seen) [2 :value]) nil))
    (t/check "qr: close each request" (= (deref closed) 3))))

(defn stopped []
  (run! (fn [start-scan]
          (let [ready (atom nil)
                decoded (atom nil)
                scans (atom 0)
                closed (atom 0)
                operation (qr/request
                           {:prepare (fn [next] (reset! ready next))
                            :scan (fn [next]
                                    (swap! scans (fn [n] (+ n 1)))
                                    (reset! decoded next))
                            :close (fn [] (swap! closed (fn [n] (+ n 1))))}
                           (fn [_result] (t/check "qr: no late delivery" false)))]
            ((:start operation))
            (if start-scan ((deref ready)))
            ((:stop operation))
            ((deref ready))
            (if start-scan
              (do
                ((deref decoded) {:err nil :value "late"})
                ((deref decoded) {:err :cancelled :value nil})
                ((deref decoded) {:err :scan-failed :value nil})))
            (t/check "qr: no late scan" (= (deref scans) (if start-scan 1 0)))
            (t/check "qr: stopped once" (= (deref closed) 1))))
        [false true]))

(defn run []
  (delivery)
  (results)
  (stopped))
