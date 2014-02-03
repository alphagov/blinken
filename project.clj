(defproject govuk/blinken "0.1.0-SNAPSHOT"
  :description "Dashboard to integrate multiple alert systems"
  :url "https://github.com/alphagov/blinken"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [docopt "0.6.1"]
                 [http-kit "2.1.16"]
                 [cheshire "5.2.0"]
                 [hiccup "1.0.4"]
                 [clj-yaml "0.4.0"]
                 [compojure "1.1.6"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :main govuk.blinken)
