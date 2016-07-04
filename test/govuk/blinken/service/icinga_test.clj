(ns govuk.blinken.service.icinga-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [govuk.blinken.service.icinga :as icinga]
            [govuk.blinken.service :as service]))

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
    (let [i-service (icinga/create "http://foo" {})]
      (is (= (service/get-status i-service)
             {:hosts nil
              :alerts nil})))))
