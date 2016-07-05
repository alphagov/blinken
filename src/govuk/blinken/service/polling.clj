(ns govuk.blinken.service.polling
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [govuk.blinken.service :as service]))

(defn poll [ms func & args]
  (let [control (async/chan)
        times (atom 0)
        out (async/go (loop [[v ch] (async/alts! [(async/timeout 0) control])]
                        (if (= ch control)
                          @times
                          (do (swap! times inc)
                              (apply func args)
                              (recur (async/alts! [(async/timeout ms) control]))))))]
    {:control control :out out}))

(defn cancel-poll [chans]
  (async/>!! (:control chans) :cancel)
  (async/<!! (:out chans)))

(defn- store! [status-atom timestamp key val]
  (swap! status-atom (fn [status]
                       (let [timestamp-for-keys (get status :timestamp {})
                             last-timestamp (get timestamp-for-keys key 0)]
                         (if (> timestamp last-timestamp)
                           (assoc status key val :timestamp (assoc timestamp-for-keys key timestamp))
                           status)))))

(defn handle-response [status-atom timestamp key parse-fn response]
  (cond (:error response)
        (do (log/error "Request error:" response)
            (store! status-atom timestamp key nil))

        (= (:status response) 200)
        (store! status-atom timestamp key
                (parse-fn (json/parse-string (:body response) true)))

        :else
        (do (log/error "Unknown request error:" response)
            (store! status-atom timestamp key nil))))

(defn to-query-params [& hashes]
  (let [all-params (reverse (apply merge hashes))]
    (str "?" (clojure.string/join "&"
                                  (map (fn [[key val]]
                                         (str (http/url-encode (name key)) "="
                                              (http/url-encode val)))
                                       all-params)))))

(defn- get-and-parse [base-url endpoint options status-atom status-key]
  (let [timestamp (System/currentTimeMillis)
        query-params (to-query-params (:query-params endpoint)
                                      {"cachebuster" (System/currentTimeMillis)})
        url (str base-url (:resource endpoint) query-params)]
    (http/get url options (partial handle-response status-atom
                                   timestamp status-key (:parse-fn endpoint)))))

(defn- request-status [url poller-options http-options]
  (fn [status-atom]
    (get-and-parse url (:alerts poller-options)
                   http-options status-atom :alerts)
    (get-and-parse url (:hosts poller-options)
                   http-options status-atom :hosts)))


(defrecord PollingService [url poller-options user-options status-atom poller]
  component/Lifecycle
  (start [polling-service]
    (if poller
      polling-service
      (let [poll-ms (get user-options :poll-ms 1000)
            http-options (get user-options :http {})
            new-poller (poll poll-ms (request-status url poller-options http-options) status-atom)]
        (log/infof "Starting poller [ms: %d, url: %s]" poll-ms url)
        (assoc polling-service :poller new-poller))))

  (stop [polling-service]
    (if poller
      (do (cancel-poll poller)
          (log/infof "Killed poller [url: %s]" url)
          (assoc polling-service :poller nil))))
  service/Service
  (get-status [this] @status-atom))

(defn create [url poller-options user-options]
  (map->PollingService {:url url
                        :poller-options poller-options
                        :user-options user-options
                        :status-atom (atom {:hosts  nil
                                            :alerts nil})
                        :poller nil}))
