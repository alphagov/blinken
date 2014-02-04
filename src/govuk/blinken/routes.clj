(ns govuk.blinken.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [govuk.blinken.dashboard :as dashboard]
            [govuk.blinken.service :as service]))

(defn- get-status
  ([services]
     (reverse (map (fn [[key config]]
                     (assoc (-> config :worker service/get-status)
                       :name (:name config)
                       :id key))
                   services)))
  ([key services]
     (if-let [config (services key)]
       (assoc (-> config :worker service/get-status)
         :name (:name config)
         :id key))))

(defn build [services]
  (routes
   (GET "/" [] (dashboard/generate "Dashboard"
                (dashboard/services-overview (get-status services))))
   (GET "/:id" [id]
        (if-let [status (get-status id services)]
          (dashboard/generate (:name status) (dashboard/service-detail status))
          {:status 404 :body "Service not found"}))

   (route/resources "/static/")
   (route/not-found "Page not found")))

