(ns argent.vendor.event_queue
  (:import [android.content Context ContentValues]
           [android.database.sqlite SQLiteDatabase]))

(defn open [^Context context]
  (let [db (.openOrCreateDatabase context "events.db" 0 nil)]
    (.execSQL db "CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT NOT NULL)")
    db))

(defn push! [^SQLiteDatabase db ^String event]
  (let [values (ContentValues.)]
    (.put values "event" event)
    (.insertOrThrow db "events" nil values))
  nil)

(defn peek [^SQLiteDatabase db]
  (let [cursor (.rawQuery db "SELECT id, event FROM events ORDER BY id LIMIT 1" nil)
        result (if (.moveToFirst cursor)
                 [(.getLong cursor 0) (.getString cursor 1)]
                 nil)]
    (.close cursor)
    result))

(defn ack! [^SQLiteDatabase db id]
  (.execSQL db (str "DELETE FROM events WHERE id = " id))
  nil)
