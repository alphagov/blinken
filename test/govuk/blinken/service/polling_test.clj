(ns govuk.blinken.service.polling-test
  (:require [clojure.test :refer :all]
            [govuk.blinken.service.polling :refer :all]))

(deftest test-poll
  (let [count (atom 0)
        poller (poll 10 (fn [count] (swap! count inc)) count)]
    (Thread/sleep 500)
    (let [times (cancel-poll poller)]
      (is (= @count times)))))

(deftest test-handle-response
  (testing "handling response from monitoring APIs"
    (testing "receiving a 200 response changes the status-atom and returns the status-atom value"
      (let [status-atom    (atom {})
            status-keyword :alerts
            parse-fn       (fn [x] x)
            response       {:body "{\"foo\": \"bar\"}"
                            :error nil
                            :status 200}]
        (is (= {:alerts {:foo "bar"}}
               (handle-response status-atom status-keyword parse-fn response)))
        (is (= {:alerts {:foo "bar"}}
               @status-atom))))
    (testing "receiving an error shouldn't change the status-atom and returns nil"
      (let [status-atom    (atom {})
            status-keyword :alerts
            parse-fn       (fn [x] x)
            response       {:body "{\"foo\": \"bar\"}"
                            :error true
                            :status 500}]
        (is (= nil
               (handle-response status-atom status-keyword parse-fn response)))
        (is (= {}
               @status-atom))))
    (testing "received a No Content (204) response"
      (let [status-atom    (atom {})
            status-keyword :alerts
            parse-fn       (fn [x] x)
            response       {:body "{\"foo\": \"bar\"}"
                            :error nil
                            :status 204}]
        (is (= nil
               (handle-response status-atom status-keyword parse-fn response)))
        (is (= {}
               @status-atom))))))

(deftest test-to-query-params
  (is (= (to-query-params "/" {:a "b"} {"c" "d"} {"e" 43587345})
         "/?a=b&c=d&e=43587345")))
