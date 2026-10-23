(ns argent.vendor.qr_request)

; All request callbacks run on the UI thread, including module readiness.
(defn request [io listener]
  (let [phase (atom :waiting)
        result (atom listener)
        stop (fn []
               (if (not= (deref phase) :done)
                 (do
                   (reset! phase :done)
                   (reset! result nil)
                   ((:close io))
                   (eprintln "Argent qr.stop"))))]
    {:start (fn []
              (eprintln "Argent qr.prepare")
              ((:prepare io)
               (fn []
                 (if (= (deref phase) :waiting)
                   (do
                     (reset! phase :scanning)
                     (eprintln "Argent qr.ready")
                     (eprintln "Argent qr.scan.start")
                     ((:scan io)
                      (fn [value]
                        (if (= (deref phase) :scanning)
                          (let [deliver (deref result)]
                            (eprintln (str "Argent qr.result status=" (or (:err value) :success)))
                            (stop)
                            (deliver value))))))))))
     :stop stop}))
