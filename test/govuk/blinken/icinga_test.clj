(ns govuk.blinken.icinga-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [govuk.blinken.icinga :as icinga]
            [govuk.blinken.protocols :as protocols]))

(def example-json (json/parse-string (slurp (io/resource "fixtures/icinga/hosts.json")) true))

(deftest test-parse-hosts
  (testing "converts hosts to simple map"
    (is (= (icinga/parse-hosts example-json)
           {:up ["monitoring.management" "puppetmaster-1.management"]
            :down []}))))

(deftest test-create
  (testing "it creates a service"
    (let [service (icinga/create "http://foo" {})]
      (is (= (protocols/get-status service)
             {:hosts {:up [] :down []}})))))


(deftest test-poll
  (let [count (atom 0)
        poller (icinga/poll 10 (fn [count] (swap! count inc)) count)]
    (Thread/sleep 500)
    (let [times (icinga/cancel-poll poller)]
      (is (= @count times)))))

