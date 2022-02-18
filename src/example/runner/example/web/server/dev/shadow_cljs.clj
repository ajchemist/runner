(ns runner.example.web.server.dev.shadow-cljs
  (:require
   [clojure.tools.cli :as cli]
   [integrant.core :as ig]
   [reitit.core :as reitit]
   [reitit.ring]
   [runner.example.web.server.main :as main]
   ))


(def ring-handler-0
  (-> (main/system-map nil (:options (cli/parse-opts ["--volume-dir" "volumes/dev"] main/cli-options)))
    (ig/prep)
    (ig/init [:example/ring-handler])
    (ig/find-derived-1 :example/ring-handler)
    (val)))


(def ring-handler #'ring-handler-0)


(comment
  (ring-handler
    {:uri            "/"
     :request-method :get})
  )
