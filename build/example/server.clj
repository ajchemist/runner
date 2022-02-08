(ns build.example.server
  (:refer-clojure :exclude [compile])
  (:require
   [clojure.string :as str]
   [clojure.tools.build.api :as build]
   ))


(def basis (build/create-basis {:project "deps.edn" :aliases [:provided :example]}))
(def class-dir "target/classes")
(def uber-file "target/server.jar")


(defn clean
  [_]
  (build/delete {:path "target"}))


(defn prep
  [_]
  (build/copy-dir
    {:src-dirs   ["src/core" "src/example"]
     :target-dir class-dir
     :ignores    #{#".*\.clj[cs]?"}}))


(defn compile
  [_]
  (build/compile-clj
    {:basis        basis
     :src-dirs     ["src/core" "src/example"]
     :class-dir    class-dir
     :compile-opts {:elide-meta     [:doc :added]
                    :direct-linking true}
     :ns-compile '#{runner.example.web.server.main}}))


(defn uber
  [_]
  (build/uber
    {:basis     basis
     :class-dir class-dir
     :uber-file uber-file
     :main      'runner.example.web.server.main}))


(defn all
  [_]
  (do
    (clean nil)
    (prep nil)
    (compile nil)
    (uber nil)))
