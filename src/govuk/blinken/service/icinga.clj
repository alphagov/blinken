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



(defn create [url options]
  (polling/create url {:alerts {:resource "/cgi-bin/icinga/status.cgi?jsonoutput"
                                :parse-fn parse-alerts}
                       :hosts {:resource "/cgi-bin/icinga/status.cgi?style=hostdetail&jsonoutput"
                               :parse-fn parse-hosts}} options))



