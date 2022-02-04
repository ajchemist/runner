(ns runner.shadow-cljs
  (:require
   [clojure.java.io :as jio]
   [clojure.edn :as edn]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [weavejester.dependency :as dep]
   )
  (:import
   java.io.PushbackReader
   ))


(set! *warn-on-reflection* true)


;; * build state


;; ** modules





;; * manifest file


;; ** edn


(defn read-manifest-edn-file
  {:deprecated true}
  ([input]
   (read-manifest-edn-file input {:eof nil} nil))
  ([input read-opts]
   (read-manifest-edn-file input read-opts nil))
  ([input read-opts slurp-opts]
   (edn/read-string read-opts (apply slurp input (into [] cat slurp-opts)))))


(defn read-manifest-edn
  [reader]
  (edn/read (PushbackReader. reader)))


;; ** json


(defn read-manifest-json
  [reader]
  (json/read reader :key-fn keyword))


;; ** transitive-modules


(defn dependency-graph
  [modules-config]
  (reduce
    (fn [g {:keys [module-id depends-on]}]
      (reduce #(dep/depend %1 module-id %2) g depends-on))
    (dep/graph)
    modules-config))


(defn transitive-modules
  [modules-config]
  (let [deps-graph (dependency-graph modules-config)]
    (reduce
      (fn [ret {:keys [module-id] :as module}]
        (let [transitive-deps (dep/transitive-dependencies deps-graph module-id)]
          (assoc ret module-id
            (conj
              (into []
                (filter (fn [{:keys [module-id]}] (contains? transitive-deps module-id)))
                modules-config)
              module))))
      {}
      modules-config)))


;;


(set! *warn-on-reflection* false)
