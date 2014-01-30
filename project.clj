(defproject govuk/blinken "0.1.0-SNAPSHOT"
  :description "Dashboard to integrate multiple alert systems"
  :url "https://github.com/alphagov/blinken"
  :license {:name "Open Government License"
            :url "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/2/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [docopt "0.6.1"]
                 [http-kit "2.1.16"]
                 [cheshire "5.2.0"]
                 [hiccup "1.0.4"]
                 [clj-yaml "0.4.0"]]
  :main govuk.blinken)
