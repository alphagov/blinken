(ns govuk.blinken.service.polling-test
  (:require [clojure.test :refer :all]
            [govuk.blinken.service.polling :refer :all]))


(deftest test-poll
  (let [count (atom 0)
        poller (poll 10 (fn [count] (swap! count inc)) count)]
    (Thread/sleep 500)
    (let [times (cancel-poll poller)]
      (is (= @count times)))))


(deftest test-to-query-params
  (let [query-params (to-query-params {:a "b"} {"c" "d"} {"e" 43587345})]
    (is (= query-params
           "?e=43587345&c=d&a=b"))))

(deftest failed-request
  (let [status-atom (atom {:alerts ["a" "b" "c"]})]
    (handle-response status-atom :alerts identity {:error "some error"})
    (is (nil? (:alerts @status-atom)))))

(deftest non-200-request
  (let [status-atom (atom {:alerts ["a" "b" "c"]})]
    (handle-response status-atom :alerts identity {:status 404})
    (is (nil? (:alerts @status-atom)))))

(deftest good-request
  (let [status-atom (atom {:alerts ["a" "b" "c"]})]
    (handle-response status-atom :alerts identity {:status 200 :body "[\"d\",\"e\"]"})
    (is (= (count (:alerts @status-atom)) 2))))


