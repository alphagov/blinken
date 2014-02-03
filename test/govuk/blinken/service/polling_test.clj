(ns govuk.blinken.service.polling-test
  (:require [clojure.test :refer :all]
            [govuk.blinken.service.polling :refer :all]))


(deftest test-poll
  (let [count (atom 0)
        poller (poll 10 (fn [count] (swap! count inc)) count)]
    (Thread/sleep 500)
    (let [times (cancel-poll poller)]
      (is (= @count times)))))


