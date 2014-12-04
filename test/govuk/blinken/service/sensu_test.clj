(ns govuk.blinken.service.sensu-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [govuk.blinken.service.sensu :as sensu]
            [govuk.blinken.service :as service]))


(def example-hosts-json
  (json/parse-string (slurp (io/resource "fixtures/sensu/hosts.json")) true))
(def example-alerts-json
  (json/parse-string (slurp (io/resource "fixtures/sensu/alerts.json")) true))

(deftest test-parse-hosts
  (testing "converts hosts to simple map"
    (is (= (sensu/parse-hosts example-hosts-json)
           {:up ["dns-2.prod" "monitoring-1.prod"]
            :down []}))))

(deftest test-parse-alerts
  (testing "converts alerts to simple map"
    (is (= (sensu/parse-alerts {} example-alerts-json)
           {:critical [{:host "proxy-1.prod"
                        :name "proxy-1_vhost_check"
                        :info "CheckHTTP CRITICAL: Connection error: Connection refused - connect(2)\n"}
                       {:host "proxy-1.prod"
                        :name "proxy-1_application_errors"
                        :info "CheckLog CRITICAL: 0 warnings, 2 criticals for pattern \"level\":\"ERROR\"\n"}]
            :warning  [{:host "proxy-1.prod"
                        :name "nginx_service_check"
                        :info "CheckCMDStatus CRITICAL: /etc/init.d/nginx status exited with 3\n"}
                       {:host "proxy-2.prod"
                        :name "proxy_application_errors"
                        :info "CheckLog CRITICAL: 0 warnings, 1 criticals for pattern \"level\":\"ERROR\"\n"}]
            :ok       []
            :unknown  []}))))

(deftest test-filter-alerts
  (testing "filters relevant alerts"
    (is (= (sensu/parse-alerts {:client {:name "proxy-2.*" :bind "127.0.0.1"}} example-alerts-json)
           {:critical []
            :warning  [{:host "proxy-2.prod"
                        :name "proxy_application_errors"
                        :info "CheckLog CRITICAL: 0 warnings, 1 criticals for pattern \"level\":\"ERROR\"\n"}]
            :ok       []
            :unknown  []}))))

(deftest test-filter-alerts2
  (testing "filters relevant alerts (multiple hashes)"
    (is (= (sensu/parse-alerts {:client {:address "10.0.0.1"} :check {:output ".*CRITICAL.*\n"}} example-alerts-json)
           {:critical [{:host "proxy-1.prod"
                        :name "proxy-1_vhost_check"
                        :info "CheckHTTP CRITICAL: Connection error: Connection refused - connect(2)\n"}
                       {:host "proxy-1.prod"
                        :name "proxy-1_application_errors"
                        :info "CheckLog CRITICAL: 0 warnings, 2 criticals for pattern \"level\":\"ERROR\"\n"}]
            :warning  [{:host "proxy-1.prod"
                        :name "nginx_service_check"
                        :info "CheckCMDStatus CRITICAL: /etc/init.d/nginx status exited with 3\n"}]
            :ok       []
            :unknown  []}))))

(deftest test-filter-alerts3
  (testing "filters relevant alerts (match numbers)"
    (is (= (sensu/parse-alerts {:id "a-b-c-d-e" :client {:address "10.0.0.1"} :check {:issued "1414.*"}} example-alerts-json)
           {:critical [{:host "proxy-1.prod"
                        :name "proxy-1_vhost_check"
                        :info "CheckHTTP CRITICAL: Connection error: Connection refused - connect(2)\n"}]
            :warning  []
            :ok       []
            :unknown  []}))))

(deftest test-create
  (testing "it creates a service"
    (let [s-service (sensu/create "http://foo" {})]
      (is (= (service/get-status s-service)
             {:hosts nil
              :alerts nil})))))



