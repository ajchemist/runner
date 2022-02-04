(ns hooks
  (:require
   [clojure.java.io :as jio]
   )
  (:import
   java.time.Instant
   ))


(defn print-build-state
  {:shadow.build/stage :flush}
  [state]
  (print (str ";; " (Instant/now)))
  (newline)
  (prn (keys state))
  (flush)
  state)


(defn spit-build-state
  {:shadow.build/stage :flush}
  [state]
  (with-open [w (jio/writer (jio/file "target/build_state.edn"))]
    (binding [*out* w]
      (print (str ";; " (Instant/now)))
      (newline)
      (pr {:shadow.build/build-id       (:shadow.build/build-id state)
           :shadow.build/mode           (:shadow.build/mode state)
           :shadow.build/stage          (:shadow.build/stage state)
           :shadow.build/config         (:shadow.build/config state)
           :shadow.build.modules/config (:shadow.build.modules/config state)})
      (flush)))
  state)
