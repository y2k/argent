(ns argent.app
  (:require [argent.application :as application]
            [argent.vendor.effect :as fx]
            [argent.vendor.event_queue :as queue]
            [argent.vendor.event_pump :as event-pump]
            [argent.vendor.chat :as chat]
            [argent.vendor.qr_image :as qr]
            [argent.vendor.toast :as toast])
  (:import [android.app Activity]
           [android.os Bundle Handler Looper]
           [android.database.sqlite SQLiteDatabase]))

(defn run [^Activity activity]
  (eprintln "Argent app.start")
  ;; ponytail: short local operations run on main; move to a worker for slow effects.
  (let [^SQLiteDatabase db (queue/open activity)
        handler (Handler. (Looper/getMainLooper))
        application (atom nil)
        output (chat/start activity (fn [] (deref application)))
        qr-input (qr/start activity)
        pump (atom nil)
        wake (fn []
               (.post handler ^void:java.lang.Runnable
                      (fn [] ((deref pump)))))
        instance (application/create
                  (-> {:context activity}
                      (fx/register :qr:pick (fn [listener] ((:open qr-input) listener)))
                      (toast/register activity)
                      (fx/register :chat:append-message (:append-message output))
                      (fx/register :chat:render-message (:render-message output))
                      (fx/register :ui:dispatch
                                   (fn [event]
                                     (.post handler ^void:java.lang.Runnable
                                            (fn []
                                              (eprintln (str "Argent ui.handle.begin event=" event))
                                              ((fx/summon (deref application) :app:handle-event!) event)
                                              (eprintln (str "Argent ui.handle.end event=" event))
                                              ((fx/summon (deref application) :app:render!))
                                              (eprintln (str "Argent render.end source=ui event=" event))))
                                     (eprintln (str "Argent ui.enqueue event=" event))
                                     nil))
                      (fx/register :domain:dispatch
                                   (fn [event]
                                     (queue/push! db event)
                                     (eprintln (str "Argent queue.enqueue event=" event))
                                     (wake)
                                     nil))))]
    (reset! application instance)
    (reset! pump
            (fn []
              (event-pump/step
               {:peek (fn [] (queue/peek db))
                :handle (fx/summon instance :app:handle-event!)
                :ack (fn [id] (queue/ack! db id))
                :render (fx/summon instance :app:render!)
                :wake wake})))
    ((fx/summon instance :app:start!))
    (wake)
    {:stop (fn []
             ((:stop qr-input))
             (.removeCallbacksAndMessages handler nil)
             (.close db)
             (eprintln "Argent app.stop")
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
