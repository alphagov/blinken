(ns govuk.blinken.service.icinga
  (:require [govuk.blinken.service.polling :as polling]))



(defn- parse-host [hosts host]
  (let [status (:status host)
        status-keyword (if (= status "UP") :up :down)
        current-list (hosts status-keyword)]
    (assoc hosts status-keyword (conj current-list (:host_name host)))))

(defn parse-hosts [hosts-json]
  (reduce parse-host
          {:up [] :down []}
          (-> hosts-json :status :host_status)))



(defn- parse-alert [alerts alert]
  (let [status (:status alert)
        status-keyword (cond (= status "OK") :ok
                             (= status "CRITICAL") :critical
                             (= status "WARNING") :warning
                             :else :unknown)
        current-list (alerts status-keyword)]
    (assoc alerts status-keyword (conj current-list
                                       {:host (:host_name alert)
                                        :name (:service_description alert)
                                        :info (:status_information alert)}))))

(defn parse-alerts [alerts-json]
  (reduce parse-alert
          {:critical [] :warning [] :ok [] :unknown []}
          (-> alerts-json :status :service_status)))


(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))


(defn create [url options]
  (polling/create url {:alerts (deep-merge {:resource "/cgi-bin/icinga/status.cgi"
                                            :query-params {:servicestatustypes 20
                                                           "jsonoutput" 1}
                                            :parse-fn parse-alerts}
                                           (get options :alerts {}))
                       :hosts (deep-merge {:resource "/cgi-bin/icinga/status.cgi"
                                           :query-params {"style" "hostdetail"
                                                          "jsonoutput" 1}
                                           :parse-fn parse-hosts}
                                          (get options :hosts {}))} options))



