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
          groups (:groups config)]
      (is (= (count groups) 2))
      (let [group-another (groups "another")]
        (is group-another)
        (is (= (:name group-another) "another"))
        (is (= (count (:environments group-another)) 1)))
      (let [group-govuk (groups "govuk")]
        (is group-govuk)
        (is (= (:name group-govuk) "GOV.UK"))
        (let [prod ((:environments group-govuk) "prod")]
          (is (= (:name prod) "Production"))
          (is (nil? (:type prod)))
          (is (nil? (:url prod)))
          (is (nil? (:options prod)))
          (is (:worker prod))
          (is (= (-> prod :worker :type) "icinga"))
          (is (= (-> prod :worker :url) "https://icinga.foo.production"))
          (is (= (-> prod :worker :options)
                 {:foo "bar"}))))))

  (testing "config file doesn't exist"
    (is (nil? (blinken/load-config (io/resource "fixtures/laeurglaerugh")
                                   type-to-worker-fn))))

  (testing "invalid type"
    (let [config (blinken/load-config (io/resource "fixtures/config-with-invalid-type.yaml")
                                      type-to-worker-fn)]
      (is (= (count (:environments ((:groups config) "foo"))) 1))))

  (testing "no url"
    (let [config (blinken/load-config (io/resource "fixtures/config-with-no-url.yaml")
                                      type-to-worker-fn)]
      (is (= (count (:environments ((:groups config) "foo"))) 0))))

  (testing "no envs in group should not add group"
    (let [config (blinken/load-config (io/resource "fixtures/config-group-with-no-envs.yaml")
                                      type-to-worker-fn)]
      (is (= (-> config :groups count) 0)))))


