(ns argent.vendor.qr_image
  (:import [android.app Activity]
           [android.content Context]
           [com.google.android.gms.common.moduleinstall ModuleInstall ModuleInstallRequest ModuleInstallResponse ModuleInstallStatusUpdate InstallStatusListener]
           [com.google.android.gms.tasks OnSuccessListener OnCanceledListener OnFailureListener]
           [com.google.mlkit.vision.codescanner GmsBarcodeScanning GmsBarcodeScannerOptions]
           [com.google.mlkit.vision.barcode.common Barcode]))

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

(defn- when-ready [ready installed]
  (if installed (ready)))

(defn- camera [^Context context]
  (let [options (.build (.enableAutoZoom (.setBarcodeFormats (GmsBarcodeScannerOptions.Builder.) 256)))
        scanner (GmsBarcodeScanning/getClient context options)
        modules (ModuleInstall/getClient context)
        install-listener (atom nil)]
    {:prepare
     (fn [ready]
       (let [^InstallStatusListener listener
             ^void:InstallStatusListener
             (fn [^ModuleInstallStatusUpdate update]
               ; STATE_COMPLETED = 4; acceptance of the request is not readiness.
               (when-ready ready (= (.getInstallState update) 4)))
             install (.build (.setListener (.addApi (ModuleInstallRequest/newBuilder) scanner) listener))]
         (reset! install-listener listener)
         (.addOnSuccessListener (.installModules modules install)
                                ^void:OnSuccessListener
                                (fn [^ModuleInstallResponse response]
                                  (when-ready ready (.areModulesAlreadyInstalled response))))))
     :scan
     (fn [result]
       (let [task (.startScan scanner)]
         (.addOnSuccessListener task ^void:OnSuccessListener
                                (fn [^Barcode barcode]
                                  (result {:err nil :value (.getRawValue barcode)})))
         (.addOnCanceledListener task ^void:OnCanceledListener
                                 (fn [] (result {:err :cancelled :value nil})))
         (.addOnFailureListener task ^void:OnFailureListener
                                (fn [error]
                                  (eprintln (str "Argent qr.error error="
                                                 (re-replace (str error) (re-pattern "[\r\n]") " ")))
                                  (result {:err :scan-failed :value nil})))))
     :close
     (fn []
       (let [ignored (if-let [listener (deref install-listener)]
                       (.unregisterListener modules (cast InstallStatusListener listener)))]
         (reset! install-listener nil)
         nil))}))

(defn start [^Activity activity]
  (let [current (atom nil)]
    {:open (fn [listener]
             (let [operation (request (camera activity) listener)]
               (reset! current operation)
               ((:start operation))))
     :stop (fn []
             (let [ignored (if-let [operation (deref current)] ((:stop operation)))]
               (reset! current nil)
               nil))}))
