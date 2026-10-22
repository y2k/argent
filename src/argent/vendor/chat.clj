(ns argent.vendor.chat
  (:require [argent.vendor.view :as view])
  (:import [android.app Activity]
           [android.view View]
           [android.widget LinearLayout ScrollView]))

(defn start [^Activity activity]
  (let [scroll (ScrollView. activity)
        root (LinearLayout. activity)
        ^int padding (view/dp activity 16)]
    (.setOrientation root 1)
    (.setGravity root 80) ; Gravity.BOTTOM
    (.setPadding root padding padding padding padding)
    (.setFillViewport scroll true)
    (.setFitsSystemWindows scroll true)
    (.addView scroll root)
    (.setContentView activity scroll)
    {:append-message (fn [node]
                       (fn [w]
                         (let [f (view/create node)]
                           (view/disable-children! root 0)
                           (.addView root (cast View (f w)))
                           (if (> (.getChildCount root) 15)
                             (.removeViewAt root 0))
                           (.post scroll
                                  ^void:java.lang.Runnable
                                  (fn [] (.scrollTo scroll 0 (.getBottom root)))))
                         nil))
     :render-message (fn [node]
                       (fn [w]
                         (view/update! w
                                       (.getChildAt root (cast int (- (.getChildCount root) 1)))
                                       node)
                         nil))}))
