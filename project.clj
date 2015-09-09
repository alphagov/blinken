(defproject govuk/blinken "0.1.0-SNAPSHOT"
  :description "Dashboard to integrate multiple alert systems"
  :url "https://github.com/alphagov/blinken"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [docopt "0.6.1"]
                 [http-kit "2.1.19"]
                 [cheshire "5.5.0"]
                 [hiccup "1.0.5"]
                 [clj-yaml "0.4.0"]
                 [compojure "1.4.0"]
                 [lein-daemon "0.5.5"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/tools.logging "0.3.1"]]
  :daemon {:blinken {:ns govuk.blinken}}
  :plugins [[lein-daemon "0.5.4"]]
  :main govuk.blinken)
