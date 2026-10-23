(ns argent.counter
  (:require [argent.application :as app]
            [argent.vendor.event :as event]
            [argent.vendor.effect :as fx]
            [argent.main :as main]
            [argent.checks.support :as t]))

(defn run []
  (let [state (atom (main/init))
        seen (atom [])
        w (main/main
           (-> {}
               (fx/register :state:update (fn [f] (swap! state f)))
               (fx/register :domain:dispatch
                            (fn [event] (swap! seen (fn [xs] (conj xs event)))))))]
    (((get-in (main/screen 0) [2 2 1 :onclick])) w)
    (((get-in (main/screen 0) [2 4 1 :onclick])) w)
    (let [unchanged (= (deref state) 0)]
      ((event/dispatch :domain:increment nil) w)
      (let [incremented (= (deref state) 1)]
        ((event/dispatch :domain:decrement nil) w)
        ((event/dispatch :domain:decrement nil) w)
        (and unchanged incremented (= (deref state) -1)
             (= (count (deref seen)) 2)
             (= (get (deref seen) 0) :domain:increment)
             (= (get (deref seen) 1) :domain:decrement))))))

(defn deferred []
  (let [state (atom 7)
        updates (atom 0)
        w (main/main
           (fx/register {} :state:update
                        (fn [f]
                          (swap! updates (fn [n] (+ n 1)))
                          (swap! state f))))
        increment (event/dispatch :domain:increment nil)
        untouched (and (= (deref state) 7)
                       (= (deref updates) 0))]
    (reset! state 20)
    (increment w)
    (and untouched (= (deref state) 21)
         (= (deref updates) 1))))

(defn messages []
  (let [seen (atom [])
        w (main/main
           (-> {}
               (event/register :ui:dispatch (fn [name] (event/dispatch name nil)))
               (fx/register :chat:new-message
                            (fn [arg] (swap! seen (fn [xs] (conj xs arg)))))))]
    ((event/dispatch :ui:start nil) w)
    (((get-in (main/screen 0) [3 1 :onclick])) w)
    (and (= (count (deref seen)) 2)
         (= (get (deref seen) 0) nil)
         (= (get (deref seen) 1) nil))))

(defn substituted []
  (let [original (atom 7)
        replacement (atom 40)
        w (main/main (fx/register {} :state:update (fn [f] (swap! original f))))
        other (-> (assoc w :state replacement)
                  (event/register :state:update
                                  (fn [f]
                                    (fn [current-world]
                                      (swap! (fx/summon current-world :state) f)))))
        result ((event/dispatch :domain:increment nil) other)]
    (and (= result 41) (= (deref replacement) 41) (= (deref original) 7))))

(defn instance-flow []
  (let [ui (atom nil)
        business (atom nil)
        messages (atom [])
        rendered (atom nil)
        renders (atom 0)
        world (atom nil)
        operations (-> {:context :test-context}
                       (fx/register :ui:dispatch (fn [event] (reset! ui event) nil))
                       (fx/register :domain:dispatch (fn [event] (reset! business event) nil))
                       (event/register :chat:append-message
                                       (fn [node]
                                         (fn [w]
                                           (reset! world w)
                                           (swap! messages (fn [xs] (conj xs node)))
                                           :appended)))
                       (fx/register :chat:render-message
                                    (fn [node]
                                      (swap! renders (fn [n] (+ n 1)))
                                      (reset! rendered node)
                                      nil)))
        instance (app/create operations)
        handle (fx/summon instance :app:handle-event!)
        render (fx/summon instance :app:render!)]
    (t/check "counter: no initial messages" (= (count (deref messages)) 0))
    (t/check "counter: no initial renders" (= (deref renders) 0))
    (t/check "counter: start result" (= ((fx/summon instance :app:start!)) nil))
    (t/check "counter: start appends" (= (count (deref messages)) 1))
    (t/check "counter: start does not render" (= (deref renders) 0))
    (t/check "counter: current context" (= (fx/summon (deref world) :context) :test-context))
    (t/check "counter: registered increment" (fx/summon (deref world) :domain:increment))
    (let [node (get (deref messages) 0)
          click (get-in node [2 2 1 :onclick])]
      (t/check "counter: initial text" (= (get-in node [2 3 1 :text]) "0"))
      ((click) (deref world)))
    (t/check "counter: no UI event" (= (deref ui) nil))
    (t/check "counter: business event" (= (deref business) :domain:increment))
    (t/check "counter: click does not render" (= (deref renders) 0))
    (render)
    (t/check "counter: first render" (= (deref renders) 1))
    (t/check "counter: before handle" (= (get-in (deref rendered) [2 3 1 :text]) "0"))
    (handle (deref business))
    (t/check "counter: handle does not render" (= (deref renders) 1))
    (t/check "counter: handle does not append" (= (count (deref messages)) 1))
    (t/check "counter: render still pending" (= (get-in (deref rendered) [2 3 1 :text]) "0"))
    (render)
    (t/check "counter: second render" (= (deref renders) 2))
    (t/check "counter: incremented text" (= (get-in (deref rendered) [2 3 1 :text]) "1"))
    (((get-in (deref rendered) [3 1 :onclick])) instance)
    (t/check "counter: new message UI event" (= (deref ui) :chat:new-message))
    (t/check "counter: new message result" (= (handle (deref ui)) nil))
    (t/check "counter: append does not render" (= (deref renders) 2))
    (t/check "counter: second message" (= (count (deref messages)) 2))
    (t/check "counter: old message text" (= (get-in (get (deref messages) 0) [2 3 1 :text]) "0"))
    (t/check "counter: new message text" (= (get-in (get (deref messages) 1) [2 3 1 :text]) "1"))
    (let [second-instance (app/create operations)]
      ((fx/summon second-instance :app:start!))
      (t/check "counter: isolated instance" (= (get-in (get (deref messages) 2) [2 3 1 :text]) "0")))
    (render)
    (t/check "counter: third render" (= (deref renders) 3))
    (t/check "counter: three messages" (= (count (deref messages)) 3))
    (= (get-in (deref rendered) [2 3 1 :text]) "1")))
