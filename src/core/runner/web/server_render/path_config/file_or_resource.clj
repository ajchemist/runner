(ns runner.web.server-render.path-config.file-or-resource
  (:require
   [clojure.java.io :as jio]
   [clojure.string :as str]
   [reitit.core :as reitit]
   [user.ring.alpha :as user.ring]
   [runner.util :as util]
   [runner.web.server-render :as server-render]
   ))


(defn- path-config-file-or-resource
  [{::reitit/keys [match] :as request}]
  (let [path             (get-in request [::reitit/match :path])
        config-file-path (util/strip-left-slash path)
        config-file-path (if (str/ends-with? config-file-path "/")
                           (str config-file-path "index.edn")
                           (str config-file-path ".index.edn"))]
    (server-render/file-or-resource
      request
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
