(ns govuk.blinken.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [govuk.blinken.dashboard :as dashboard]
            [govuk.blinken.service :as service]))

(defn- get-environment-status [group-key key environment]
  (assoc (-> environment :worker service/get-status)
    :name (:name environment)
    :id key
    :group-id group-key))

(defn- get-group-status [key group]
  (assoc group
    :id key
    :environments (reverse (map (fn [[env-key environment]]
                                  (get-environment-status key env-key environment))
                                (:environments group)))))

(defn- get-groups-status [groups]
  (reverse (map (fn [[key group]] (get-group-status key group)) groups)))

(defn build [groups]
  (routes
   (route/resources "/static/")

   (GET "/" [] (dashboard/generate false
                (dashboard/groups-overview (get-groups-status groups))))
   (GET "/:group-id" [group-id]
        (if-let [group (groups group-id)]
          (dashboard/generate true (dashboard/environments-detail
                               (:environments (get-group-status group-id group))))
          {:status 404 :body "Group not found"}))

   (GET "/:group-id/:id" [group-id id]
        (if-let [group (groups group-id)]
          (if-let [environment ((:environments group) id)]
            (dashboard/generate true (dashboard/environment-detail
                                 (get-environment-status group-id id environment)))
            {:status 404 :body "Environment not found"})
          {:status 404 :body "Group not found"}))

   (route/not-found "Page not found")))
