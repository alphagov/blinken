(ns govuk.blinken.icinga
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [govuk.blinken.protocols :as protocols]))


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
            (partial handle-hosts-response status-atom)))

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

(deftype IcingaService [url options status-atom poller-atom]
  protocols/Service
  (start [this] (let [poll-ms (get options :poll-ms 1000)]
                  (println (str "Starting Icinga poller [ms:" poll-ms ", url:" url "]"))
                  (reset! poller-atom
                          (poll poll-ms
                                (fn [status-atom]
                                  (get-hosts url status-atom))
                                status-atom))))
  (get-status [this] @status-atom)
  (stop [this] (if-let [poller @poller-atom]
                 (do (cancel-poll poller)
                     (reset! poller-atom nil)
                     (println "Killed Icinga poller")))))


(defn create [url options]
  (let [status-atom (atom {:hosts {:up [] :down []}})]
    (IcingaService. url options status-atom (atom nil))))

