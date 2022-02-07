(ns runner.example.web.server.main
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.java.io :as jio]
   [clojure.tools.cli :as cli]
   [integrant.core :as ig]
   [user.ring.alpha.model.integrant :as ig.ring]
   [runner.environment :as runner.env]
   [runner.integrant :as runner.ig]
   [runner.integrant.config-source.file-or-resource :as runner.ig.file-or-resource]
   )
  (:import
   java.io.File
   ))


(defn- resolve-path
  "Return just path if path is absolute, otherwise return path with parent."
  [path parent]
  (let [path (str path)]
    (if (str/starts-with? path File/separator)
      path
      (.getPath (jio/file parent path)))))


(def cli-options
  [[nil "--system-config-edn-path" "config.edn file or resource path"
    :default "config/server.edn"
    :validate [string?]]
   [nil "--volume-dir VOLUME_DIR" "volume directory"
    :default "volume"
    :validate-fn [string?]]
   [nil "--log-dir LOG_DIR" "log file directory"
    :default-fn (fn [{:keys [volume-dir]}] (.getPath (jio/file volume-dir "log")))
    :validate-fn [string?]]
   [nil "--web-dir WEB_DIR" "web content root directory"
    :default-fn (fn [{:keys [volume-dir]}] (.getPath (jio/file volume-dir "web")))
    :validate-fn [string?]]
   [nil "--server-render-root SERVER_RENDER_ROOT" "relative server render root path to `volume-dir`"
    :default "server-render"
    :validate-fn [string?]]
   [nil "--file-public-root FILE_PUBLIC_ROOT" "relative path to `web-dir`"
    :default "public"
    :validate-fn [string?]]
   [nil "--resource-public-root RESOURCE_PUBLIC_ROOT" "resource public root"
    :default "public"
    :validate-fn [string?]]
   [nil "--body-script-modules-file BODY_SCRIPT_MODULES_FILE" "BODY_SCRIPT_MODULE_FILE"
    :default "body_script_modules.edn"
    :validate-fn [string?]]
   [nil "--shadow-cljs-manifest-file SHADOW_CLJS_MANIFEST_FILE" "SHADOW_CLJS_MANIFEST_FILE"
    :default "manifest.json"
    :validate-fn [string?]]
   [nil "--webpack-asset-manifest-file WEBPACK_ASSET_MANIFEST_FILE" "WEBPACK_ASSET_MANIFEST_FILE"
    :default "b/assets.json"
    :validate-fn [string?]]])


(defn direnv
  "Create direnv"
  [{:keys [volume-dir web-dir log-dir
           server-render-root
           file-public-root
           resource-public-root
           body-script-modules-file
           shadow-cljs-manifest-file
           webpack-asset-manifest-file]
    :as   _options}]
  (let [file-public-root   (resolve-path file-public-root web-dir)
        server-render-root (resolve-path server-render-root volume-dir)]
    (as-> {:system/volume-dir                         volume-dir
           :system/web-dir                            web-dir
           :system/log-dir                            log-dir
           :server-render/root-dir                    server-render-root
           :web/file-public-root                      file-public-root
           :web/resource-public-root                  resource-public-root
           :server-render/webpack-asset-manifest-file (resolve-path webpack-asset-manifest-file file-public-root)
           :server-render/body-script-modules-file    (resolve-path body-script-modules-file server-render-root)
           :shadow-cljs/manifest-file                 (resolve-path shadow-cljs-manifest-file server-render-root)}
      $
      (do
        (run! #(.mkdirs (jio/file (get $ %)))
              [:system/log-dir
               :server-render/root-dir
               :web/file-public-root])
        (run! #(jio/make-parents (jio/file (get $ %)))
              [:server-render/webpack-asset-manifest-file
               :server-render/body-script-modules-file])
        $))))


;;


(derive :example/http-server ::ig.ring/jetty-server)


(defn system-map
  [rules {:keys [profile system-config-edn-file-or-resource-path] :as cli-opts}]
  {:post [(map? %)]}
  (runner.ig/merge-system-maps
    rules
    (merge
      (direnv cli-opts)
      {:system/profile profile})
    [(runner.ig.file-or-resource/config-source system-config-edn-file-or-resource-path)]))


(defn -main
  [& xs]
  (try
    (let [{{:keys [volume-dir] :as cli-opts} :options} (cli/parse-opts xs cli-options)]
      (println "[runner.environment] profile:" runner.env/*profile*)
      (prn cli-opts)
      (runner.env/install-system-exit-signal-handler!)
      (runner.ig/install-system-shutdown-hook!)
      (binding [runner.env/*volume-directory* volume-dir]
        (as-> (-> (system-map nil cli-opts)
                (ig/prep)
                (ig/init [:module/core]))
          $
          (reset! runner.env/*system $))))
    (catch Throwable e
      (.printStackTrace e)
      (shutdown-agents))))
