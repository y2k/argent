(ns argent.checks.logging
  (:require [argent.vendor.effect :as fx]
            [argent.vendor.event :as event]
            [argent.vendor.event_pump :as pump]
            [argent.checks.event_pump :as pump-check]
            [argent.checks.qr_request :as qr-check]
            [argent.checks.support :as t]))

(defn operations []
  (let [actual (t/capture
                (fn []
                  (let [env (-> {}
                                (fx/register :inner (fn [arg] arg))
                                (event/register :outer (fn [arg] (event/dispatch :inner arg))))
                        call (event/dispatch :outer 42)]
                    (eprintln "before-execution")
                    (t/check "logging: operation result" (= (call env) 42))
                    (eprintln "after-execution"))))]
    (t/check "logging: deferred and nested operations"
             (= (re-replace actual (re-pattern "\r\n") "\n")
                (str "before-execution\n"
                     "Argent op.begin name=outer\n"
                     "Argent op.begin name=inner\n"
                     "Argent op.end name=inner\n"
                     "Argent op.end name=outer\n"
                     "after-execution\n")))))

(defn queue []
  (let [actual (re-replace
                (re-replace (t/capture (fn [] (pump-check/run))) (re-pattern "\r\n") "\n")
                (re-pattern "Argent op\\.[^\n]*\n") "")
        expected (reduce
                  (fn [text [id event]]
                    (str text
                         "Argent queue.handle.begin id=" id " event=" event "\n"
                         "Argent queue.handle.end id=" id " event=" event "\n"
                         "Argent queue.ack id=" id " event=" event "\n"
                         "Argent render.end source=domain id=" id " event=" event "\n"))
                  ""
                  [[42 :domain:increment] [7 :domain:increment]
                   [12 :domain:increment] [18 :domain:decrement]])]
    (t/check "logging: queue order and ids" (= actual expected)))
  (t/check "logging: empty queue is silent"
           (= (t/capture (fn [] (pump/step {:peek (fn [] nil)}))) "")))

(defn qr []
  (let [actual (re-replace (t/capture (fn [] (qr-check/run))) (re-pattern "\r\n") "\n")
        expected (str
                  (reduce (fn [text status]
                            (str text "Argent qr.prepare\nArgent qr.ready\nArgent qr.scan.start\n"
                                 "Argent qr.result status=" status "\nArgent qr.stop\n"))
                          "" [:success :cancelled :success :scan-failed])
                  "Argent qr.prepare\nArgent qr.stop\n"
                  "Argent qr.prepare\nArgent qr.ready\nArgent qr.scan.start\nArgent qr.stop\n")]
    (t/check "logging: QR lifecycle" (= actual expected))))

(defn run []
  (operations)
  (queue)
  (qr))
