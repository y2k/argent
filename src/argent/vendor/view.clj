(ns argent.vendor.view
  (:require [argent.vendor.view_node :as node]
            [argent.vendor.effect :as fx])
  (:import [android.content Context]
           [android.view View ViewGroup]
           [android.widget LinearLayout Button TextView]
           [com.google.android.flexbox FlexboxLayout]))

(defn dp [^Context context ^int value]
  (cast int (android.util.TypedValue/applyDimension
             1 value (.getDisplayMetrics (.getResources context)))))

(defn disable-children! [^ViewGroup parent ^int index]
  (if (< index (.getChildCount parent))
    (let [child (.getChildAt parent index)]
      (.setEnabled child false)
      (if (instance? ViewGroup child)
        (disable-children! child 0))
      (disable-children! parent (+ index 1)))))

(defn- create-button [w {:text title :onclick onclick :compact compact :description description}]
  (let [^Context context (fx/summon w :context)
        btn (Button. context)
        ^int gap (dp context 8)
        params (FlexboxLayout.LayoutParams. -2 -2)]
    (.setMargins params 0 0 gap gap)
    (.setLayoutParams btn params)
    (.setText btn (cast String title))
    (if description
      (.setContentDescription btn (cast String description)))
    (if compact
      (let [^int size (dp context 48)]
        (.setMinWidth btn size)
        (.setMinimumWidth btn size)
        (.setMinimumHeight btn size)))
    (.setOnClickListener btn
                         ^void:View.OnClickListener
                         (fn [_e]
                           ((onclick) w)))
    btn))

(defn- create-text [w {:text ^String title :color color}]
  (let [^Context context (fx/summon w :context)
        text (TextView. context)]
    (.setText text title)
    (if color
      (.setTextColor text (android.graphics.Color/parseColor (cast String color))))
    (.setTextSize text 28)
    (.setPadding text 0 0 0 (cast int (dp context 8)))
    text))

(defn- create-divider [w]
  (let [^Context context (fx/summon w :context)
        divider (View. context)
        ^int gap (dp context 8)
        params (LinearLayout.LayoutParams. -1 (cast int (dp context 1)))]
    (.setMargins params 0 gap 0 gap)
    (.setLayoutParams divider params)
    (.setBackgroundColor divider (.getCurrentTextColor (TextView. context)))
    (.setAlpha (.getBackground divider) 48)
    divider))

(defn- add-children [w ^ViewGroup layout children]
  (reduce (fn [result child]
            (let [f (create child)]
              (.addView layout (cast View (f w)))
              result))
          layout
          children))

(defn- create-layout [w ^int orientation children]
  (let [^Context context (fx/summon w :context)
        layout (LinearLayout. context)]
    (.setOrientation layout orientation)
    (.setPadding layout 0 0 0 (cast int (dp context 8)))
    (add-children w layout children)))

(defn- create-row [w children]
  (let [^Context context (fx/summon w :context)
        layout (FlexboxLayout. context)]
    ;; ly2k has no Java static-field expression: 0 is ROW and 1 is WRAP.
    (.setFlexDirection layout 0)
    (.setFlexWrap layout 1)
    (add-children w layout children)))

(defn create [node]
  (fn [w]
    (let [expanded (node/expand node)
          [tag attrs] expanded
          children (drop 2 expanded)]
      (case tag
        :button (create-button w attrs)
        :text (create-text w attrs)
        :divider (create-divider w)
        :column (create-layout w 1 children)
        :row (create-row w children)
        (sneaky-throw (RuntimeException. (str "unknown view: " tag)))))))

;; Fixed-shape screens: keep buttons attached while another tap is in flight.
(defn update! [w ^View widget node]
  (let [[tag attrs] (node/expand node)]
    (case tag
      :text (.setText (cast TextView widget) (cast String (:text attrs)))
      :button (let [^Button button widget
                    onclick (:onclick attrs)]
                (.setText button (cast String (:text attrs)))
                (.setOnClickListener button
                                     ^void:View.OnClickListener
                                     (fn [_e] ((onclick) w))))
      :divider nil
      (reduce (fn [index child]
                (update! w (.getChildAt (cast ViewGroup widget) (cast int index)) child)
                (+ index 1))
              0
              (drop 2 node))))
  nil)
