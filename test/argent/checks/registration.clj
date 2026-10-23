(ns argent.checks.registration
  (:require [argent.vendor.effect :as fx]
            [argent.vendor.event :as event]
            [argent.checks.support :as t]))

(defn composition []
  (let [state (atom 7)
        factories (atom 0)
        env (-> {}
                (fx/register :state:update (fn [f] (swap! state f)))
                (event/register :add
                                (fn [amount]
                                  (swap! factories (fn [n] (+ n 1)))
                                  (event/dispatch :state:update
                                                  (fn [value] (+ value amount))))))
        call (event/dispatch :add 3)]
    (t/check "registration: deferred factory" (= (deref factories) 0))
    (t/check "registration: deferred state" (= (deref state) 7))
    (t/check "registration: result" (= (call env) 10))
    (t/check "registration: state" (= (deref state) 10))
    (t/check "registration: factory count" (= (deref factories) 1))
    (let [replacement (atom 40)
          other (fx/register env :state:update (fn [f] (swap! replacement f)))]
      (t/check "registration: replacement result" (= (call other) 43))
      (t/check "registration: replacement state" (= (deref replacement) 43))
      (t/check "registration: original state" (= (deref state) 10))))
  nil)

(defn action []
  (let [seen (atom nil)
        invoked (atom false)
        env (fx/register {} :save
                         (fn [value]
                           (reset! seen value)
                           (fn [] (reset! invoked true) value)))
        call (event/dispatch :save 23)]
    (t/check "registration: deferred action" (= (deref seen) nil))
    (let [result (call env)]
      (t/check "registration: argument" (= (deref seen) 23))
      (t/check "registration: function not invoked" (= (deref invoked) false))
      (t/check "registration: function result" (= (result) 23))
      (t/check "registration: explicit invocation" (= (deref invoked) true))))
  nil)

(defn run []
  (composition)
  (action))
