(ns runner.web.server-render
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.java.io :as jio]
   [reitit.core :as reitit]
   [rum.core :as rum]
   [user.ring.alpha :as user.ring]
   [runner.util :as util]
   ))


;;


(set! *warn-on-reflection* true)


;;


(s/def :html/title string?)
(s/def :html/author string?)
(s/def :html/description string?)
(s/def :html/lang string?)
(s/def :html/root-uri string?)
(s/def :html/reply-to string?)
(s/def :html/viewport string?)
(s/def :html/robots string?)


(s/def :request/html-params
  (s/keys
    :opt [:html/title :html/author :html/description :html/lang
          :html/root-uri :html/reply-to :html/viewport :html/robots]))


;; * IO


(defn file-or-resource
  ([request]
   (file-or-resource
     request
     (get-in request [::reitit/match :path])))
  ([{:keys [::reitit/match]} path]
   (file-or-resource
     (get-in match [:data :server-render/root-dir])
     path)))


;; * Ring


(defn wrap-html-request
  "Ring middleware to transform `request` with html params."
  ([handler target-key component]
   (wrap-html-request handler target-key (fnil conj []) component))
  ([handler target-key update-fn component]
   (user.ring/wrap-transform-request handler
     (fn [request]
       (-> request (update target-key update-fn (if (fn? component) (component request) component)))))))


;; * webpack


(defn read-webpack-manifest-json
  [json-file]
  (util/read-json-file json-file))


(defn find-webpack-asset-path
  [match asset-name]
  (get-in match [:data :server-render/webpack-asset-manifest asset-name]))


;; ** ring-middleware


(defn wrap-webpack-asset-stylesheets
  [handler entries]
  (wrap-html-request
    handler
    :html/stylesheets
    (fnil into [])
    (fn [{:keys [::reitit/match]}]
      (into []
        (comp
          (map (fn [asset-name] (find-webpack-asset-path match asset-name)))
          (map (fn [path] [:link {:rel "stylesheet" :href path}])))
        entries))))


;; * js


(defn transitive-body-script-modules
  [transitive-modules]
  (reduce-kv
    (fn [m module-id modules]
      (assoc m module-id
        (into []
          (map (fn [{:keys [output-name]}] [:script {:src (util/slash-absolutize output-name)}]) )
          modules)))
    {}
    transitive-modules))


(defn read-body-script-modules
  [edn-file]
  (let [edn (util/read-edn-file edn-file)]
    (if (map? edn)
      edn
      nil)))


;; ** ring-middleware


(defn wrap-body-scripts
  [handler module-id]
  (wrap-html-request
    handler
    :html/body-scripts
    (fnil into [])
    (fn [{:keys [::reitit/match]}]
      (get-in match [:data :server-render/body-script-modules module-id]))))


;; * html


;; ** predicates


(defn html-component?
  [x]
  (and (vector? x) (= (first x) :html)))


;; ** render


(defn render-static-html
  [component]
  (str "<!DOCTYPE html>" (rum/render-static-markup component)))


(defn html-head
  [{{:keys [path]} ::reitit/match

    :keys [
           :html/root-uri
           :html/title
           :html/author
           :html/description
           :html/reply-to
           :html/viewport
           :html/robots
           :html/head-components
           :html/stylesheets
           :html/scripts
           :anti-forgery-token ; ring.middleware.anti-forgery
           ]

    :as request}]
  (util/reduce-into
    [:head
     (when title [:title title])
     (when author [:meta {:name "author" :content author}])
     (when description [:meta {:name "description" :content description}])
     (when reply-to [:meta {:name "reply-to" :content reply-to}])
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content (or viewport "width=device-width, initial-scale=1")}]
     [:meta {:name "robots" :content (or robots "index,follow")}]
     [:meta {:property "og:type" :content (or (:og/type request) "website")}]
     (when-let [og-locale (:og/locale request)] [:meta {:property "og:locale" :content og-locale}])
     (when-let [og-site-name (or (:og/site-name request) title)] [:meta {:property "og:site_name" :content og-site-name}])
     (when-let [og-title (or (:og/title request) title)] [:meta {:property "og:title" :content og-title}])
     (when-let [og-description (or (:og/description request) description)] [:meta {:property "og:description" :content og-description}])
     (when-let [og-image (:og/image request)] [:meta {:property "og:image" :content og-image}])
     (when path [:meta {:property "og:url" :content (str root-uri path)}])
     (when anti-forgery-token [:meta {:name "csrf-token" :content anti-forgery-token}])]
    head-components
    stylesheets
    scripts))


(defn html-component
  [{:keys [:html/lang :html/contents :html/body-scripts] :as request}]
  [:html
   (cond-> {}
     lang (assoc :lang lang))
   (html-head request)
   (util/reduce-into
     [:body]
     contents
     body-scripts)])


;; ** ring-response


(defn html-component-response
  [request]
  {:status  200
   :headers { "Content-Type" "text/html; charset=utf-8" }
   :body    (html-component request)})


(defn html-response
  [request]
  {:status  200
   :headers { "Content-Type" "text/html; charset=utf-8" }
   :body    (render-static-html (html-component request))})


;; ** ring-middleware


(defn wrap-render-html
  [handler]
  (user.ring/wrap-transform-response handler
    (fn [{:keys [body] :as resp}]
      (if (html-component? body)
        (-> resp
          (update :status #(or % 200))
          (update-in [:headers "Content-Type"] #(or % "text/html; charset=utf-8"))
          (update :body render-static-html))
        resp))))


;;


(set! *warn-on-reflection* false)
