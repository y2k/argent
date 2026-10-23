(ns argent.vendor.event_pump)

(defn step [{:peek peek :handle handle :ack ack :render render :wake wake}]
  (if-let [item (peek)]
    (let [[id event] item]
      (eprintln (str "Argent queue.handle.begin id=" id " event=" event))
      (handle event)
      (eprintln (str "Argent queue.handle.end id=" id " event=" event))
      (ack id)
      (eprintln (str "Argent queue.ack id=" id " event=" event))
      (render)
      (eprintln (str "Argent render.end source=domain id=" id " event=" event))
      (wake))))
