(ns manifest (:require [xml :as xml]))

(deps {:xml "0.3.0"})

(xml/to-string
 [:manifest {:xmlns:android "http://schemas.android.com/apk/res/android"}
  [:application {:android:label "Argent"
                 :android:theme "@android:style/Theme.Material.Light.NoActionBar"}
   [:activity {:android:name "argent.app$MainActivity"
               :android:exported "true"}
    [:intent-filter {}
     [:action {:android:name "android.intent.action.MAIN"}]
     [:category {:android:name "android.intent.category.LAUNCHER"}]]]]])
