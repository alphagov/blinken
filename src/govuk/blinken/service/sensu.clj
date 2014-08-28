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


(defn matches-filter-param? [filter-params alert key]
  (re-matches (re-pattern (filter-params key)) (alert key)))

(defn matches-filter-params? [filter-params alert]
  (every? (partial matches-filter-param? filter-params alert) (keys filter-params)))

(defn filter-alerts [filter-params alerts]
  (filter #(matches-filter-params? filter-params %) alerts))

(defn parse-alerts [filter-params, alerts-json]
  (reduce parse-alert
          {:critical [] :warning [] :ok [] :unknown []}
          (filter-alerts filter-params alerts-json)))

(defn create [url options]
  (let [filter-params (-> options :alerts :filter-params)]
    (polling/create url {:alerts {:resource "/events"
                                  :parse-fn (partial parse-alerts filter-params)}
                         :hosts {:resource "/clients"
                                 :parse-fn parse-hosts}} options)))