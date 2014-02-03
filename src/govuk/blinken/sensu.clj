(ns govuk.blinken.sensu
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


(defn parse-hosts [hosts-json] hosts-json
  {:up (map :name hosts-json)
   :down []})

(defn get-hosts [base-url options status-atom]
  (http/get (str base-url "/clients")
            options
            (partial handle-response status-atom :hosts parse-hosts)))



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

(defn get-alerts [base-url options status-atom]
  (http/get (str base-url "/events")
            options
            (partial handle-response status-atom :alerts parse-alerts)))



(deftype SensuService [url options status-atom poller-atom]
  protocols/Service
  (start [this] (let [poll-ms (get options :poll-ms 1000)
                      http-options (get options :http {})]
                  (println (str "Starting Sensu poller [ms:" poll-ms ", url:" url "]"))
                  (reset! poller-atom
                          (util/poll poll-ms
                                     (fn [status-atom]
                                       (get-hosts url http-options status-atom)
                                       (get-alerts url http-options status-atom))
                                     status-atom))))
  (get-status [this] @status-atom)
  (stop [this] (if-let [poller @poller-atom]
                 (do (util/cancel-poll poller)
                     (reset! poller-atom nil)
                     (println "Killed Sensu poller")))))

(defn create [url options]
  (let [status-atom (atom {:hosts {:up [] :down []}
                           :alerts {:critical [] :warning [] :ok [] :unknown []}})]
    (SensuService. url options status-atom (atom nil))))

