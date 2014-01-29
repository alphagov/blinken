(ns govuk.blinken.icinga
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]))


(defn- parse-host [hosts host]
  (let [status (:status host)
        status-keyword (if (= status "UP") :up :down)
        current-list (hosts status-keyword)]
    (assoc hosts status-keyword (conj current-list (:host_name host)))))

(defn parse-hosts [hosts-json]
  (reduce parse-host
          {:up [] :down []}
          (-> hosts-json :status :host_status)))


(defn- handle-hosts-response [status-atom response]
  (cond (:error response)
        (println "Request error:" response)

        (= (:status response) 200)
        (swap! status-atom assoc :hosts
               (parse-hosts (json/parse-string (:body response) true)))

        :else
        (println "Unknown request error:" response)))

(defn get-hosts [base-url status-atom]
  (http/get (str base-url "/cgi-bin/icinga/status.cgi?style=hostdetail&jsonoutput")
            {:insecure? true}
            (apply handle-hosts-response status-atom)))

