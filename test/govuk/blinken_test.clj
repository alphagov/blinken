(ns govuk.blinken-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [govuk.blinken :as blinken]))


(defn get-service-by-name [services name]
  (first (filter #(= (:name %) name) services)))

(defn service-with-name? [services name]
  (-> (get-service-by-name services name) nil? not))


(deftest test-load-config
  (testing "with spec config"
    (let [config (blinken/load-config (io/resource "fixtures/config.yaml"))
          services (:services config)]
      (is (= (count services) 2))
      (is (service-with-name? services "GOV.UK Production"))
      (is (service-with-name? services "GOV.UK Staging"))
      (let [prod-service (get-service-by-name services "GOV.UK Production")]
        (is (nil? (:type prod-service)))
        (is (nil? (:url prod-service)))
        (is (nil? (:options prod-service)))
        (is (:worker prod-service)))))

  (testing "config file doesn't exist"
    (is (nil? (blinken/load-config (io/resource "fixtures/laeurglaerugh")))))

  (testing "invalid type"
    (let [config (blinken/load-config (io/resource "fixtures/config-with-invalid-type.yaml"))]
      (is (= (count (:services config)) 1))))

  (testing "no url"
    (let [config (blinken/load-config (io/resource "fixtures/config-with-no-url.yaml"))]
      (is (= (count (:services config)) 0)))))


