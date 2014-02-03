(ns govuk.blinken.icinga
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [govuk.blinken.protocols :as protocols]
            [govuk.blinken.util :as util]))

(defn- handle-response [status-atom status-keyword parse-fn response]
  (cond (:error response)
        (println "Request error:" response)

        (= (:status response) 200)
        (swap! status-atom assoc status-keyword
               (parse-fn (json/parse-string (:body response) true)))

        :else
        (println "Unknown request error:" response)))



(defn- parse-host [hosts host]
  (let [status (:status host)
        status-keyword (if (= status "UP") :up :down)
        current-list (hosts status-keyword)]
    (assoc hosts status-keyword (conj current-list (:host_name host)))))

(defn parse-hosts [hosts-json]
  (reduce parse-host
          {:up [] :down []}
          (-> hosts-json :status :host_status)))

(defn get-hosts [base-url status-atom]
  (http/get (str base-url "/cgi-bin/icinga/status.cgi?style=hostdetail&jsonoutput")
            {:insecure? true}
            (partial handle-response status-atom :hosts parse-hosts)))



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

(defn get-alerts [base-url status-atom]
  (http/get (str base-url "/cgi-bin/icinga/status.cgi?jsonoutput")
            {:insecure? true}
            (partial handle-response status-atom :alerts parse-alerts)))



(deftype IcingaService [url options status-atom poller-atom]
  protocols/Service
  (start [this] (let [poll-ms (get options :poll-ms 1000)]
                  (println (str "Starting Icinga poller [ms:" poll-ms ", url:" url "]"))
                  (reset! poller-atom
                          (util/poll poll-ms
                                     (fn [status-atom]
                                       (get-hosts url status-atom)
                                       (get-alerts url status-atom))
                                     status-atom))))
  (get-status [this] @status-atom)
  (stop [this] (if-let [poller @poller-atom]
                 (do (util/cancel-poll poller)
                     (reset! poller-atom nil)
                     (println "Killed Icinga poller")))))


(defn create [url options]
  (let [status-atom (atom {:hosts  {:up [] :down []}
                           :alerts {:critical [] :warning [] :ok [] :unknown []}})]
    (IcingaService. url options status-atom (atom nil))))

