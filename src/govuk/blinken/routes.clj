(ns govuk.blinken.routes
  (:require [compojure.core :refer :all]
            [govuk.blinken.dashboard :as dashboard]
            [govuk.blinken.protocols :as protocols]))


(defn build [services]
  (routes
   (GET "/" []
        (dashboard/generate (map #(assoc (-> % :worker protocols/get-status)
                                    :name (:name %))
                                 services)))))

