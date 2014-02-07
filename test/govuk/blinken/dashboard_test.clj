(ns govuk.blinken.dashboard-test
  (:require [clojure.test :refer :all]
            [govuk.blinken.dashboard :as dashboard]))


(defn has-class? [html class]
  (let [flat-html (flatten html)]
    (some #(= (:class %) class) flat-html)))

(defn has-content? [html content]
  (let [flat-html (flatten html)]
    (some #(= % content) flat-html)))

(defn- children
  ([element] (children element false))
  ([element with-values?]
     (if (vector? element)
       (let [position (if (map? (second element)) 2 1)]
         (vec (filter #(and (not (nil? %))
                            (or with-values? (vector? %))) (nthrest element position))))
       nil)))

(defn element-has-content? [html class content]
  (let [results (clojure.walk/prewalk (fn [element]
                                        (if (and (map? (second element))
                                                 (= (:class (second element)) class)
                                                 (some #(= % content)
                                                       (flatten (children element true))))
                                          :found
                                          (children element))) html)]
    (some #(= % :found) (flatten results))))

(defn num-elements-with-class [html class]
  (let [flat-html (flatten html)]
    (count (filter #(= (:class %) class) flat-html))))


(deftest test-host-status
  (testing "host status widget with just up"
    (let [html (dashboard/host-status {:up ["foo" "bar"] :down []})]
      (is (has-class? html "ok"))
      (is (element-has-content? html "ok" 2))
      (is (has-class? html "critical"))
      (is (element-has-content? html "critical" 0))
      (is (not (has-class? html "down-hosts")))
      (is (not (has-content? html "foo")))))

  (testing "host status widget with some down"
    (let [html (dashboard/host-status {:up [] :down ["foo"]})]
      (is (has-class? html "ok"))
      (is (element-has-content? html "ok" 0))
      (is (has-class? html "critical"))
      (is (element-has-content? html "critical" 1))
      (is (has-class? html "down-hosts"))
      (is (has-content? html "foo"))))

  (testing "no host data"
    (let [html (dashboard/host-status nil)]
      (is (has-class? html "host-status"))
      (is (has-content? html "Hosts"))
      (is (has-content? html "No data")))))

(deftest test-alerts
  (testing "only ok alerts"
    (let [html (dashboard/alerts {:ok ["doesnt matter"] :warning []
                                  :critical [] :unknown []})]
      (is (has-class? html "ok"))
      (is (element-has-content? html "ok" 1))
      (is (has-class? html "warning"))
      (is (element-has-content? html "warning" 0))
      (is (has-class? html "critical"))
      (is (element-has-content? html "critical" 0))
      (is (has-class? html "unknown"))
      (is (element-has-content? html "unknown" 0))
      (is (not (has-class? html "problem-alerts")))))
  
  (testing "with problem alerts"
    (let [html (dashboard/alerts {:ok []
                                  :warning [{:host "h1" :name "n1" :info "i1"}]
                                  :critical [{:host "h2" :name "n2" :info "i2"}]
                                  :unknown [{:host "h3" :name "n3" :info "i3"}]})]
      (is (element-has-content? html "ok" 0))
      (is (element-has-content? html "warning" 1))
      (is (element-has-content? html "critical" 1))
      (is (element-has-content? html "unknown" 1))
      (doseq [i (range 1 3)]
        (do (is (has-content? html (str "h" i)))
            (is (has-content? html (str "n" i)))
            (is (has-content? html (str "i" i)))))))

  (testing "no alert data"
    (let [html (dashboard/alerts nil)]
      (is (has-class? html "alerts"))
      (is (has-content? html "Alerts"))
      (is (has-content? html "No data"))
      (is (not (has-class? html "problem-alerts"))))))

(deftest test-environment-overview
  (testing "general layout"
    (let [html (dashboard/environment-overview {:name "Some Environment!"
                                                :alerts {:critical [1 2 3]
                                                         :warning []}})]
      (is (has-class? html "environment-overview critical"))
      (is (element-has-content? html "critical" 3))
      (is (element-has-content? html "warning" 0))
      (is (has-content? html "Some Environment!"))))

  (testing "warning overview"
    (let [html (dashboard/environment-overview {:name "Some Environment!"
                                                :alerts {:critical []
                                                         :warning [1 2 3]}})]
      (is (has-class? html "environment-overview warning"))
      (is (not (has-class? html "critical")))
      (is (element-has-content? html "warning" 3))))

  (testing "ok overview"
    (let [html (dashboard/environment-overview {:name "Some Environment!"
                                                :alerts {:critical []
                                                         :warning []}})]
      (is (has-class? html "environment-overview ok"))
      (is (not (has-class? html "critical")))
      (is (not (has-class? html "warning")))))

  (testing "no data overview"
    (let [html (dashboard/environment-overview {:name "Some Environment!"
                                                :alerts nil})]
      (is (has-class? html "environment-overview no-data"))
      (is (not (has-class? html "count"))))))

(deftest test-generate-structure
  (testing "has correct number of environments"
    (let [environments [{:name "Production" :hosts {:up [] :down []}}
                        {:name "Staging" :hosts {:up [] :down []}}]
          environments-html (dashboard/environments-detail environments)
          html (dashboard/generate-structure "List of environments" environments-html)]
      (is (has-content? html "Home"))
      (is (has-content? html "List of environments"))
      (is (= (num-elements-with-class html "environment") 2))
      (is (= (num-elements-with-class html "host-status") 2)))))


