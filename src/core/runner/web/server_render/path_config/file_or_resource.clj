(ns runner.web.server-render.path-config.file-or-resource
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as str]
   [reitit.core :as reitit]
   [user.ring.alpha :as user.ring]
   [runner.util :as util]
   ))


(defn file-or-resource
  [sr-root-dir path]
  (let [sr-root (jio/as-file sr-root-dir)]
    (if (and sr-root (.isDirectory sr-root))
      (let [file (jio/file sr-root-dir path)]
        (cond
          (.isFile file) file
          :else          (jio/resource path)))
      (jio/resource path))))


(defn- path-config-file-or-resource
  [{::reitit/keys [match]}]
  (let [{:keys [path]}   match
        config-file-path (util/strip-left-slash path)
        config-file-path (if (str/ends-with? config-file-path "/")
                           (str config-file-path "index.edn")
                           (str config-file-path ".index.edn"))]
    (file-or-resource
      (get-in match [:data :server-render/root-dir])
      config-file-path)))


(defn path-config-map
  [request]
  (if-let [file (path-config-file-or-resource request)]
    (if (.exists (jio/as-file file))
      (let [edn (util/read-edn-file file)]
        (if (map? edn)
          edn
          {}))
      {})
    {}))


(defn wrap-extended-request-params
  [handler]
  (user.ring/wrap-transform-request handler
    (fn [{:as request}]
      (merge (path-config-map request) request))))
