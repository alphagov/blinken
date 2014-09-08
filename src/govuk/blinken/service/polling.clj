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



#_(defn- handle-response [status-atom status-keyword parse-fn response]
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


(defn get-all-and-parse [url http-options poller-options status-atom]
  (let [chs (for [[key path] (:resources poller-options)]
              (do
                (let [ch         (async/chan)
                      query-params (to-query-params {} ;(:query-params endpoint)
                                                    {"cachebuster" (System/currentTimeMillis)})
                      url (str url path query-params)]
                  (http/get url http-options #(async/put! ch %))
                  [key ch])))]
    (async/go-loop [chs chs results {}]
      (if (seq chs)
        (let [[[key ch] & rest] chs]
          (recur rest (assoc results key (json/parse-string (:body (async/<! ch)) true))))
        (let [parsed-results ((:parse-fn poller-options) results)]
          (reset! status-atom parsed-results))))))

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
