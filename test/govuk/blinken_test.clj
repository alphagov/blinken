(ns govuk.blinken-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [govuk.blinken :as blinken]))


(def type-to-worker-fn {"icinga" (fn [url options] {:type "icinga"
                                                   :url url
                                                   :options options})})

(deftest test-load-config
  (testing "with spec config"
    (let [config (blinken/load-config (io/resource "fixtures/config.yaml")
                                      type-to-worker-fn)
          services (:services config)]
      (is (= (count services) 2))
      (is (services "govuk-prod"))
      (is (services "govuk-staging"))
      (is (= (-> "govuk-staging" services :name)
             "govuk-staging"))
      (let [prod-service (services "govuk-prod")]
        (is (= (:name prod-service) "GOV.UK Production"))
        (is (nil? (:type prod-service)))
        (is (nil? (:url prod-service)))
        (is (nil? (:options prod-service)))
        (is (:worker prod-service))
        (is (= (-> prod-service :worker :type) "icinga"))
        (is (= (-> prod-service :worker :url) "https://icinga.foo.production"))
        (is (= (-> prod-service :worker :options)
               {:foo "bar"})))))

  (testing "config file doesn't exist"
    (is (nil? (blinken/load-config (io/resource "fixtures/laeurglaerugh")
                                   type-to-worker-fn))))

  (testing "invalid type"
    (let [config (blinken/load-config (io/resource "fixtures/config-with-invalid-type.yaml")
                                      type-to-worker-fn)]
      (is (= (count (:services config)) 1))))

  (testing "no url"
    (let [config (blinken/load-config (io/resource "fixtures/config-with-no-url.yaml")
                                      type-to-worker-fn)]
      (is (= (count (:services config)) 0)))))


