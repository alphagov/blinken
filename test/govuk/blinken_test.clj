(ns govuk.blinken-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [govuk.blinken :as blinken]))

(defn load-config [config-file]
  (blinken/load-config (io/resource config-file)
                       {"icinga" (fn [url options]
                                   {:type "icinga"
                                    :url url
                                    :options options})}))

(deftest test-load-config
  (testing "config file doesn't exist"
    (is (nil? (load-config "fixtures/laeurglaerugh"))))
  (testing "with spec config"
    (let [config (load-config "fixtures/config.yaml")
          groups (:groups config)]
      (is (= (count groups) 2))
      (let [group-another (groups "another")]
        (is (= (:name group-another) "another"))
        (is (= (count (:environments group-another)) 1)))
      (let [group-govuk (groups "govuk")]
        (is (= (:name group-govuk) "GOV.UK"))
        (let [prod ((:environments group-govuk) "prod")]
          (is (= {:name "Production"
                  :worker {:type "icinga"
                           :url "https://icinga.foo.production"
                           :options {:foo "bar"}}}
                 prod))
          (is (nil? (:type prod)))
          (is (nil? (:url prod)))
          (is (nil? (:options prod)))))))
  (testing "invalid type"
    (let [config (load-config "fixtures/config-with-invalid-type.yaml")]
      (is (= (count (:environments ((:groups config) "foo"))) 1))))
  (testing "no url"
    (let [config (load-config "fixtures/config-with-no-url.yaml")]
      (is (= (count (:environments ((:groups config) "foo"))) 0))))
  (testing "no envs in group should not add group"
    (let [config (load-config "fixtures/config-group-with-no-envs.yaml")]
      (is (= (-> config :groups count) 0)))))


