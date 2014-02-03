(ns govuk.blinken.routes
  (:require [compojure.core :refer :all]
            [govuk.blinken.dashboard :as dashboard]
            [govuk.blinken.service :as service]))


(defn build [services]
  (routes
   (GET "/" []
        (dashboard/generate (map #(assoc (-> % :worker service/get-status)
                                    :name (:name %))
                                 services)))))

