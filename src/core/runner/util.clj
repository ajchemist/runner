(ns runner.util
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as str]
   [clojure.stacktrace :as stacktrace]
   [clojure.edn :as edn]
   [clojure.data.json :as json]
   )
  (:import
   java.io.PushbackReader
   ))


(defn reduce-into
  [template & xs]
  (reduce
    (fn [ret coll] (into ret coll))
    template
    xs))


(defn strip-left-slash
  "Return just path(slashed) if path is absolute, otherwise return path(slashed) with parent."
  [s]
  (if (str/starts-with? s "/")
    (subs s 1)
    s))


(defn slash-absolutize
  "Return absoulute path(slashed)."
  [s]
  (if (str/starts-with? s "/")
    s
    (str "/" s)))


(defn slurp-edn-map
  "Read a readable source, slurp it, and read it as a map.

  Default `read-fn` is `edn/read-string`"
  ([source slurp-opts]
   (slurp-edn-map source slurp-opts edn/read-string))
  ([source slurp-opts read-string-fn]
   (let [ret (read-string-fn (apply slurp source (into [] cat slurp-opts)))]
     (if (map? ret)
       ret
       (throw
         (let [path (str source)]
           (ex-info (format "Expected edn map in: %s" path) {:path path})))))))


(defn read-edn-file
  "Return nil if errors occur"
  [edn-file]
  (try
    (with-open [rdr (jio/reader (jio/as-file edn-file))]
      (edn/read (PushbackReader. rdr)))
    (catch Exception e
      (stacktrace/print-stack-trace e)
      nil)))


(defn read-json-file
  "Return nil if errors occur"
  [json-file]
  (try
    (with-open [rdr (jio/reader (jio/as-file json-file))]
      (json/read rdr))
    (catch Exception e
      (stacktrace/print-stack-trace e)
      nil)))


(defn file-or-resource
  [root-dir path]
  (let [root (jio/as-file root-dir)]
    (if (and root (.isDirectory root))
      (let [file (jio/file root path)]
        (cond
          (.isFile file) file
          :else          (jio/resource path)))
      (jio/resource path))))
