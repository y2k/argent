(ns argent.app
  (:require [argent.main :as main]
            [argent.vendor.event :as event]
            [argent.vendor.effect :as fx]
            [argent.vendor.event_queue :as queue]
            [argent.vendor.chat :as chat]
            [argent.vendor.qr_image :as qr])
  (:import [android.app Activity]
           [android.os Bundle Handler Looper]
           [android.database.sqlite SQLiteDatabase]
           [android.widget Toast]))

(defn create [env]
  (let [state (atom (main/init))
        app-env (main/main
                 (-> env
                     (fx/register :state:update (fn [f] (swap! state f)))
                     (event/register :chat:new-message
                                     (fn [_arg]
                                       (fx/next (event/dispatch :state:update (fn [value] value))
                                                (fn [current-state]
                                                  (fx/next (event/dispatch :chat:append-message
                                                                           (main/screen current-state))
                                                           (fn [_result] (fx/pure nil)))))))))]
    (merge app-env
           {:app:start! (fn [] ((event/dispatch :ui:start nil) app-env))
            :app:handle-event! (fn [event] ((event/dispatch event nil) app-env))
            :app:render! (fn []
                           ((fx/next (event/dispatch :state:update (fn [value] value))
                                     (fn [current-state]
                                       (event/dispatch :chat:render-message (main/screen current-state))))
                            app-env)
                           nil)})))

(defn run [^Activity activity]
  ;; ponytail: short local operations run on main; move to a worker for slow effects.
  (let [^SQLiteDatabase db (queue/open activity)
        handler (Handler. (Looper/getMainLooper))
        output (chat/start activity)
        qr-input (qr/start activity)
        application (atom nil)
        pump (atom nil)
        wake (fn []
               (.post handler ^void:java.lang.Runnable
                      (fn [] ((deref pump)))))
        instance (create
                  (-> {:context activity}
                      (fx/register :qr:pick (fn [listener] ((:open qr-input) listener)))
                      (fx/register :ui:toast
                                   (fn [text]
                                     (.show (Toast/makeText activity (cast String text) 0))
                                     nil))
                      (event/register :chat:append-message (:append-message output))
                      (event/register :chat:render-message (:render-message output))
                      (fx/register :ui:dispatch
                                   (fn [event]
                                     (.post handler ^void:java.lang.Runnable
                                            (fn []
                                              ((fx/summon (deref application) :app:handle-event!) event)
                                              ((fx/summon (deref application) :app:render!))))
                                     nil))
                      (fx/register :domain:dispatch
                                   (fn [event]
                                     (queue/push! db event)
                                     (wake)
                                     nil))))]
    (reset! application instance)
    (reset! pump
            (fn []
              (if-let [item (queue/peek db)]
                (let [[id event] item]
                  ((fx/summon instance :app:handle-event!) event)
                  (queue/ack! db id)
                  ((fx/summon instance :app:render!))
                  (wake)))))
    ((fx/summon instance :app:start!))
    (wake)
    {:stop (fn []
             ((:stop qr-input))
             (.removeCallbacksAndMessages handler nil)
             (.close db)
             nil)}))

(gen-class
 :name MainActivity
 :extends Activity
 :methods [[^override onCreate [Bundle] void]
           [^override onDestroy [] void]])

(defn -onCreate [^MainActivity self savedInstanceState]
  (.setTag (.getDecorView (.getWindow self)) (run self))
  nil)

(defn -onDestroy [^MainActivity self]
  (let [instance (.getTag (.getDecorView (.getWindow self)))]
    ((:stop instance))))
