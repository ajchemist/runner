(ns runner.web.server-render.integrant
  (:require
   [runner.util :as util]
   [runner.web.server-render :as server-render]
   [integrant.core :as ig]
   [hawk.core :as hawk]
   ))


(defmethod ig/init-key ::auto-update-reference
  [_ {:keys [file-path read-fn watch-opts]}]
  (server-render/auto-update-reference file-path read-fn watch-opts))


(defmethod ig/halt-key! ::auto-update-reference
  [_ a]
  (when-let [w (:hawk/watch (meta a))]
    (println "[hawk/stop!]:" (:file-path (meta a)))
    (hawk/stop! w)))


(derive ::webpack-asset-manifest-reference ::auto-update-reference)
(derive ::body-script-modules-reference ::auto-update-reference)


(defmethod ig/prep-key ::webpack-asset-manifest-reference
  [_ config]
  (assoc config :read-fn util/read-json-file))


(defmethod ig/prep-key ::body-script-modules-reference
  [_ config]
  (assoc config :read-fn util/read-edn-file))
