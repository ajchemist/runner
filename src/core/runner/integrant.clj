(ns runner.integrant
  (:require
   [clojure.java.io :as jio]
   [clojure.stacktrace :as stacktrace]
   [clojure.edn :as edn]
   [integrant.core :as ig]
   [runner.util :as util]
   [runner.environment :as env]
   [runner.merge :as merge]
   ))


(defprotocol IConfigSource)


;; * Config Source


(defn slurp-edn-map
  "Read a readable source, slurp it, and read it as a map.

  Default `read-fn` is `edn/read-string`"
  ([source slurp-opts]
   (slurp-edn-map source slurp-opts edn/read-string))
  ([source slurp-opts read-string-fn]
   (when (satisfies? IConfigSource source)
     (util/slurp-edn-map source slurp-opts read-string-fn))))


(defn slurp-system-map
  "Read the file-or-resource specified by the path-segments, slurp it, and read it as edn."
  [source slurp-opts]
  (slurp-edn-map source slurp-opts ig/read-string))


(defn merge-system-maps
  "The first map type return of slurp wins"
  [rules config sources]
  (merge/merge-maps
    rules
    (some
      (fn [src]
        (let [system-map (slurp-system-map src (:slurp-opts (meta src)))]
          (if (map? system-map)
            system-map
            nil)))
      sources)
    config))


(defn merge-system-maps-2
  "Generous merge-system-maps"
  [rules config sources]
  (merge/merge-maps
    rules
    (apply
      merge/merge-maps
      rules
      (map (fn [src] (slurp-system-map src (:slurp-opts (meta src)))) sources))
    config))


;; * Init


;; * Shutdown


(defn- halt-system!
  []
  (when-let [sys @env/*system]
    (ig/halt! sys)))


(defn install-system-shutdown-hook!
  []
  (.addShutdownHook
    (Runtime/getRuntime)
    (Thread.
      (fn
        []
        (try
          (halt-system!)
          (catch Throwable e
            (stacktrace/print-stack-trace e)))))))
