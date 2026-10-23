(ns argent.checks.runner
  (:require [argent.checks.registration :as registration]
            [argent.checks.qr_request :as qr]
            [argent.checks.qr_example :as example]
            [argent.checks.qr_button :as button]
            [argent.checks.counter :as counter]
            [argent.checks.event_pump :as pump]
            [argent.checks.logging :as logging]))

(raw-code "public static void main(String[] args) throws Exception { run(); }")

(defn run []
  (registration/run)
  (println "registration ok")
  (qr/run)
  (println "qr request ok")
  (example/run)
  (println "qr example ok")
  (button/run)
  (println "qr button ok")
  (counter/run)
  (println "counter ok")
  (pump/run)
  (println "event pump ok")
  (logging/run)
  (println "logging ok")
  (println "unit tests passed"))
