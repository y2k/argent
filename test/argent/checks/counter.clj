(ns argent.checks.counter
  (:require [argent.counter :as counter]
            [argent.checks.support :as t]))

(defn run []
  (t/check "counter: routing and arithmetic" (counter/run))
  (t/check "counter: messages" (counter/messages))
  (t/check "counter: deferred" (counter/deferred))
  (t/check "counter: substituted" (counter/substituted))
  (t/check "counter: instance flow" (counter/instance-flow)))
