(ns govuk.blinken.util-test
  (:require [clojure.test :refer :all]
            [govuk.blinken.util :refer :all]))


(deftest test-poll
  (let [count (atom 0)
        poller (poll 10 (fn [count] (swap! count inc)) count)]
    (Thread/sleep 500)
    (let [times (cancel-poll poller)]
      (is (= @count times)))))


