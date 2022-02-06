(ns runner.integrant.config-source.file-or-resource
  (:require
   [clojure.java.io :as jio]
   [runner.environment :as env]
   [runner.integrant :as runner.ig]
   ))


(defrecord FileOrResourceConfigSource [file-or-resource]
  runner.ig/IConfigSource
  jio/IOFactory
  (make-reader [_ opts]
    ;; TODO: log trace level
    (println "FileOrResourceSource:" (str file-or-resource))
    (apply jio/reader file-or-resource opts)))


(defn config-source
  [^String path]
  (when-let [x (env/file-or-resource path)]
    (->FileOrResourceConfigSource x)))
