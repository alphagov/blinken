(ns govuk.blinken.service.sensu
  (:require [govuk.blinken.service.polling :as polling]))



(defn parse-hosts [hosts-json] hosts-json
  {:up (map :name hosts-json)
   :down []})



(def status-to-type {0 :ok 1 :warning 2 :critical 3 :unknown})

(defn- parse-alert [alerts alert]
  (let [status-keyword (status-to-type (:status alert))
        current-list (alerts status-keyword)]
    (assoc alerts status-keyword (conj current-list
                                       {:host (:client alert)
                                        :name (:check alert)
                                        :info (:output alert)}))))

(defn parse-alerts [alerts-json]
  (reduce parse-alert
          {:critical [] :warning [] :ok [] :unknown []}
          alerts-json))



(defn create [url options]
  (polling/create url {:alerts {:resource "/events"
                                :parse-fn parse-alerts}
                       :hosts {:resource "/clients"
                               :parse-fn parse-hosts}} options))


