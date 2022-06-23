(ns runner.integrant.preset.plain
  (:require
   [integrant.core :as ig]
   ))


(defmethod ig/init-key :default [_ o] o)
(defmethod ig/halt-key! :default [_ _])
