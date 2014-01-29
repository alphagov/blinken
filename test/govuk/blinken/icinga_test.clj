(ns govuk.blinken.icinga-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [govuk.blinken.icinga :as icinga]))

(def example-json (json/parse-string (slurp (io/resource "fixtures/icinga/hosts.json")) true))

(deftest test-parse-hosts
  (testing "converts hosts to simple map"
    (is (= (icinga/parse-hosts example-json)
           {:up ["monitoring.management" "puppetmaster-1.management"]
            :down []}))))

