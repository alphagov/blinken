(defproject govuk/blinken "0.1.0-SNAPSHOT"
  :description "Dashboard to integrate multiple alert systems"
  :url "https://github.com/alphagov/blinken"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}

  :min-lein-version "2.0.0"

  :plugins [[lein-ancient "0.6.7"]
            [jonase/eastwood "0.2.1"]
            [lein-bikeshed "0.2.0"]
            [lein-kibit "0.1.2"]
            [lein-daemon "0.5.4"]]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [reloaded.repl "0.1.0"]
                 [docopt "0.6.1"]
                 [http-kit "2.1.19"]
                 [cheshire "5.5.0"]
                 [hiccup "1.0.5"]
                 [clj-yaml "0.4.0"]
                 [compojure "1.4.0"]
                 [lein-daemon "0.5.5"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]]

  :aliases {"checkall" ["do" ["check"] ["kibit"] ["eastwood"] ["bikeshed"]]}

  :daemon {:blinken {:ns govuk.blinken}}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [leiningen #= (leiningen.core.main/leiningen-version)]
                                  [im.chit/vinyasa "0.4.1"]]
                   :source-paths ["dev"]}})
