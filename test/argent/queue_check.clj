(ns argent.queue_check
  (:require [argent.vendor.event_queue :as queue]))

(defn check [ok]
  (if (not ok) (sneaky-throw (RuntimeException. "queue check failed"))))

(defn run [context]
  (let [original (cast android.database.sqlite.SQLiteDatabase (queue/open context))]
    (.execSQL original "DELETE FROM events")
    (queue/push! original :increment)
    (queue/push! original :decrement)
    (.close original))
  (let [db (cast android.database.sqlite.SQLiteDatabase (queue/open context))
        [id1 event1] (queue/peek db)]
    (check (= event1 :increment))
    (queue/ack! db id1)
    (let [[id2 event2] (queue/peek db)]
      (check (> id2 id1))
      (check (= event2 :decrement))
      (queue/ack! db id2))
    (check (= (queue/peek db) nil))
    (.close db))
  (android.util.Log/i "ArgentTest" "queue ok")
  nil)
