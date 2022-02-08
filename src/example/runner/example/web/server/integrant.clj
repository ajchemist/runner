(ns runner.example.web.server.integrant
  (:require
   [integrant.core :as ig]
   [runner.environment :as runner.env]
   [runner.web.server-render :as server-render]
   [runner.web.server-render.path-config.file-or-resource :as server-render.path-config.file-or-resource]
   [ring.middleware.defaults]
   [reitit.core :as reitit]
   [reitit.ring]
   [user.ring.alpha :as user.ring]
   [user.ring.alpha.model.integrant :as ig.ring]
   ))


(defn raw-routes
  [{:as _options}]
  [""
   ["/"
    {:middleware [[server-render/wrap-render-html]
                  [server-render.path-config.file-or-resource/wrap-extended-request-params]]
     :get        (fn [request]
            (server-render/html-component-response
              (assoc request
                :html/contents [[:p "Hello, World!"]])))}]])


(defmethod ig/init-key :example/ring-handler
  [_ {:keys [:system/volume-dir
             :system/web-dir
             :system/log-dir

             :web/file-public-root
             :web/resource-public-root

             :conn]
      :as options}]
  (reitit.ring/ring-handler
    (reitit.ring/router
      (raw-routes
        {:conn                     conn
         :web/file-public-root     file-public-root
         :web/resource-public-root resource-public-root})
      {:data {
              ;; static (init-time)
              :system/volume-dir volume-dir
              :system/web-dir    web-dir
              :system/log-dir    log-dir

              :server-render/root-dir               (:server-render/root-dir options)
              :server-render/body-script-modules    (server-render/read-body-script-modules (:server-render/body-script-modules-file options))
              :server-render/webpack-asset-manifest (server-render/read-webpack-manifest-json (:server-render/webpack-asset-manifest-file options))
              }})
    (reitit.ring/routes
      #_(reitit.ring/create-file-handler {})
      #_(reitit.ring/create-resource-handler {})
      (reitit.ring/create-default-handler))
    {:middleware [(fn [handler]
                    (ring.middleware.defaults/wrap-defaults
                      handler
                      (-> ring.middleware.defaults/site-defaults
                        (assoc-in [:static :files] file-public-root)
                        (assoc-in [:static :resources] resource-public-root))))
                  [user.ring/wrap-transform-request
                   (fn [request]
                     (update request :html/root-uri #(or % (:html/root-uri options))))]
                  (runner.env/not-in-profiles
                    #{:dev}
                    [user.ring/wrap-transform-response
                     (fn [resp]
                       (update-in resp
                         [:headers "Content-Security-Policy"]
                         #(or % "default-src 'self' https://fonts.googleapis.com/ https://fonts.gstatic.com/ 'unsafe-inline';")))])]}))


(defmethod ig/halt-key! :example/ring-handler
  [_ _])


(derive :example/http-server ::ig.ring/jetty-server)
