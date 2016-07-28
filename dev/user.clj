(ns user
  (:require [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :as ns-repl]
            [reloaded.repl :refer [system init start stop go reset clear]]
            [govuk.blinken :refer [load-config]]
            [govuk.blinken.system :refer [new-system]]
            [govuk.blinken.service :as service]))

(reloaded.repl/set-init!
 #(new-system 8080 (load-config "./config.yaml")))

(defn test-all [] (run-all-tests #"^govuk.blinken.*-test$"))

(defn reset-and [f]
  (clear)
  (ns-repl/refresh :after (symbol "user" f)))

(defn reset-all []
  (ns-repl/clear)
  (reset))
