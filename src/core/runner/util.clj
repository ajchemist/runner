(ns runner.util
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
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
