(ns govuk.blinken.dashboard-test
  (:require [clojure.test :refer :all]
            [govuk.blinken.dashboard :as dashboard]))


(defn has-class? [html class]
  (let [flat-html (flatten html)]
    (some #(= (:class %) class) flat-html)))

(defn has-content? [html content]
  (let [flat-html (flatten html)]
    (some #(= % content) flat-html)))

(defn- children [element]
  (if (vector? element)
    (let [position (if (map? (second element)) 2 1)]
      (vec (filter #(and (not (nil? %))
                         (vector? %)) (nthrest element position))))
    nil))

(defn element-has-content? [html class content]
  (let [results (clojure.walk/prewalk (fn [element]
                                        (if (and (map? (second element))
                                                 (= (:class (second element)) class)
                                                 (= (nth element 2) content))
                                          :found
                                          (children element))) html)]
    (some #(= % :found) results)))

(deftest test-host-status
  (testing "host status widget with just up"
    (let [html (dashboard/host-status {:up ["foo" "bar"] :down []})]
      (is (has-class? html "up"))
      (is (element-has-content? html "up" 2))
      (is (has-class? html "down"))
      (is (element-has-content? html "down" 0))
      (is (not (has-class? html "down-hosts")))
      (is (not (has-content? html "foo")))))

  (testing "host status widget with some down"
    (let [html (dashboard/host-status {:up [] :down ["foo"]})]
      (is (has-class? html "up"))
      (is (element-has-content? html "up" 0))
      (is (has-class? html "down"))
      (is (element-has-content? html "down" 1))
      (is (has-class? html "down-hosts"))
      (is (has-content? html "foo")))))


