(ns govuk.blinken.util
  (:require [clojure.core.async :as async]))



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



