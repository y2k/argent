(ns argent.main
  (:require [argent.vendor.event :as event]))

(defn init [] 0)

(defn update-state [f]
  (event/dispatch :state:update f))

(defn main [w]
  (-> w
      (event/register :ui:start
                      (fn [_arg] (event/dispatch :chat:new-message nil)))
      (event/register :domain:increment
                      (fn [_arg] (update-state (fn [state] (+ state 1)))))
      (event/register :domain:decrement
                      (fn [_arg] (update-state (fn [state] (- state 1)))))))

(defn screen [state]
  [:column {}
   [:row {}
    [:button {:text "+1" :description "Increment"
              :onclick (fn [] (event/dispatch :domain:dispatch :domain:increment))}]
    [:text {:text (str state)}]
    [:button {:text "-1" :description "Decrement"
              :onclick (fn [] (event/dispatch :domain:dispatch :domain:decrement))}]]
   [:button {:text "Новое сообщение" :description "New message"
             :onclick (fn [] (event/dispatch :ui:dispatch :chat:new-message))}]
   [:qr-image-button {:text "Сканировать QR" :description "Scan QR"
                      :onresult (fn [{:err err :value value}]
                                  (event/dispatch :ui:toast
                                                  (case err
                                                    :cancelled "Сканирование отменено"
                                                    :scan-failed "Не удалось завершить сканирование"
                                                    value)))}]])
