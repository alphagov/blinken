(ns govuk.blinken.icinga-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [govuk.blinken.icinga :as icinga]
            [govuk.blinken.protocols :as protocols]))

(def example-hosts-json
  (json/parse-string (slurp (io/resource "fixtures/icinga/hosts.json")) true))
(def example-alerts-json
  (json/parse-string (slurp (io/resource "fixtures/icinga/alerts.json")) true))

(deftest test-parse-hosts
  (testing "converts hosts to simple map"
    (is (= (icinga/parse-hosts example-hosts-json)
           {:up ["monitoring.management" "puppetmaster-1.management"]
            :down []}))))

(deftest test-parse-alerts
  (testing "converts alerts to simple map"
    (is (= (icinga/parse-alerts example-alerts-json)
           {:critical [{:host "whitehall-mysql-slave-2.backend.production"
                        :name "unable to ssh"
                        :info "SSH OK - OpenSSH_5.9p1 Debian-5ubuntu1.1 (protocol 2.0)"}]
            :warning  []
            :ok       [{:host "apt-1.management.production"
                        :name "high disk time"
                        :info "OK: value=0.0"}]
            :unknown  []}))))

(deftest test-create
  (testing "it creates a service"
    (let [service (icinga/create "http://foo" {})]
      (is (= (protocols/get-status service)
             {:hosts {:up [] :down []}
              :alerts {:critical [] :warning [] :ok [] :unknown []}})))))


(deftest test-poll
  (let [count (atom 0)
        poller (icinga/poll 10 (fn [count] (swap! count inc)) count)]
    (Thread/sleep 500)
    (let [times (icinga/cancel-poll poller)]
      (is (= @count times)))))

