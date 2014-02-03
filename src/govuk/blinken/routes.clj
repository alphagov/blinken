(ns govuk.blinken.routes
  (:require [compojure.core :refer :all]
            [govuk.blinken.dashboard :as dashboard]
            [govuk.blinken.service :as service]))


(defn build [services]
  (routes
   (GET "/" []
        (dashboard/generate (map (fn [[key config]]
                                   (assoc (-> config :worker service/get-status)
                                     :name (:name config)))
                                 services)))))

