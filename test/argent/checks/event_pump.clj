(ns argent.checks.event_pump
  (:require [argent.vendor.event_pump :as pump]
            [argent.application :as app]
            [argent.vendor.effect :as fx]
            [argent.checks.support :as t]))

(defn order []
  (let [calls (atom [])
        record (fn [value] (swap! calls (fn [xs] (conj xs value))))
        instance (app/create
                  (fx/register {} :chat:render-message
                               (fn [node]
                                 (record (str "render:" (get-in node [2 3 1 :text]))))))
        handle (fx/summon instance :app:handle-event!)
        render (fx/summon instance :app:render!)]
    (pump/step {:peek (fn [] (record :peek) [42 :domain:increment])
                :handle (fn [event]
                          (record event)
                          (t/check "pump: handle result" (= (handle event) 1)))
                :ack (fn [id] (record (str "ack:" id)))
                :render render
                :wake (fn [] (record :wake))})
    (t/check "pump: call count" (= (count (deref calls)) 5))
    (t/check "pump: peek first" (= (get (deref calls) 0) :peek))
    (t/check "pump: handle second" (= (get (deref calls) 1) :domain:increment))
    (t/check "pump: ack third" (= (get (deref calls) 2) "ack:42"))
    (t/check "pump: render fourth" (= (get (deref calls) 3) "render:1"))
    (t/check "pump: wake last" (= (get (deref calls) 4) :wake))))

(defn single-step []
  (let [pending (atom [[7 :domain:increment] [12 :domain:increment] [18 :domain:decrement]])
        calls (atom [])
        record (fn [value] (swap! calls (fn [xs] (conj xs value))))
        io {:peek (fn [] (record :peek) (get (deref pending) 0))
            :handle (fn [event] (record event))
            :ack (fn [id]
                   (record id)
                   (swap! pending (fn [items] (drop 1 items))))
            :render (fn [] (record :render))
            :wake (fn [] (record :wake))}]
    (run! (fn [[id event remaining]]
            (reset! calls [])
            (pump/step io)
            (t/check "pump: one item per step" (= (count (deref pending)) remaining))
            (t/check "pump: step call count" (= (count (deref calls)) 5))
            (t/check "pump: step peek" (= (get (deref calls) 0) :peek))
            (t/check "pump: step event" (= (get (deref calls) 1) event))
            (t/check "pump: step id" (= (get (deref calls) 2) id))
            (t/check "pump: step render" (= (get (deref calls) 3) :render))
            (t/check "pump: step wake" (= (get (deref calls) 4) :wake)))
          [[7 :domain:increment 2] [12 :domain:increment 1] [18 :domain:decrement 0]])
    (reset! calls [])
    (pump/step io)
    (t/check "pump: empty queue calls" (= (count (deref calls)) 1))
    (t/check "pump: empty queue only peeks" (= (get (deref calls) 0) :peek))))

(defn run []
  (order)
  (single-step))
