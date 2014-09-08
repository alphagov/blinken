(ns govuk.blinken.service.polling
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [govuk.blinken.service :as service]))



(defn poll [ms func & args]
  (let [control (async/chan)
        times (atom 0)
        out (async/go-loop [[v ch] (async/alts! [(async/timeout 0) control])]
              (if (= ch control)
                @times
                (do (swap! times inc)
                    (apply func args)
                    (recur (async/alts! [(async/timeout ms) control])))))]
    {:control control :out out}))

(defn cancel-poll [chans]
  (async/>!! (:control chans) :cancel)
  (async/<!! (:out chans)))



(defn- handle-response [status-atom status-keyword parse-fn response]
  (cond (:error response)
        (log/error "Request error:" response)

        (= (:status response) 200)
        (swap! status-atom assoc status-keyword
               (parse-fn (json/parse-string (:body response) true)))

        :else
        (log/error "Unknown request error:" response)))

(defn to-query-params [& hashes]
  (let [all-params (reverse (apply merge hashes))]
    (str "?" (clojure.string/join "&"
                                  (map (fn [[key val]]
                                         (str (http/url-encode (name key)) "="
                                              (http/url-encode val)))
                                       all-params)))))

(defn- get-and-parse [base-url endpoint options status-atom status-key]
  (let [query-params (to-query-params (:query-params endpoint)
                                      {"cachebuster" (System/currentTimeMillis)})
        url (str base-url (:resource endpoint) query-params)]
    (http/get url options (partial handle-response status-atom
                                   status-key (:parse-fn endpoint)))))

(defn get-all-and-parse [url http-options poller-options status-atom]
  (doseq [[k v] poller-options]
    (get-and-parse url v http-options status-atom k)))

(deftype PollingService [url poller-options user-options status-atom poller-atom]
  service/Service
  (start [this] (let [poll-ms (get user-options :poll-ms 1000)
                      http-options (get user-options :http {})]
                  (log/info (str "Starting poller [ms:" poll-ms ", url:" url "]"))
                  (reset! poller-atom
                          (poll poll-ms
                                (partial get-all-and-parse url http-options poller-options)
                                status-atom))))
  (get-status [this] @status-atom)
  (stop [this] (if-let [poller @poller-atom]
                 (do (cancel-poll poller)
                     (reset! poller-atom nil)
                     (log/info "Killed poller")))))

(defn create [url poller-options user-options]
  (let [status-atom (atom {})]
    (PollingService. url poller-options user-options status-atom (atom nil))))
