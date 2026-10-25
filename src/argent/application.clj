(ns argent.application
  (:require [argent.main :as main]
            [argent.vendor.event :as event]
            [argent.vendor.effect :as fx]))

(defn create [env]
  (let [state (atom (main/init))
        app-env (main/main
                 (-> env
                     (fx/register :state:update (fn [f] (swap! state f)))
                     (event/register :chat:new-message
                                     (fn [_arg]
                                       (fx/next (fx/call :state:update (fn [value] value))
                                                (fn [current-state]
                                                  (fx/next (fx/call :chat:append-message
                                                                    (main/screen current-state))
                                                           (fn [_result] (fx/pure nil)))))))))]
    (merge app-env
           {:app:start! (fn [] ((event/dispatch :ui:start nil) app-env))
            :app:handle-event! (fn [event] ((event/dispatch event nil) app-env))
            :app:render! (fn []
                           ((fx/next (fx/call :state:update (fn [value] value))
                                     (fn [current-state]
                                       (fx/call :chat:render-message (main/screen current-state))))
                            app-env)
                           nil)})))
