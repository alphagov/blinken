(ns govuk.blinken
  (:require [docopt.core :as dc]
            [docopt.match :as dm]))


(def usage "Blinken

A dashboard that aggregates multiple alert sources

Usage:
  blinken -h | --help
  blinken -v | --version
")

(def version "Blinken 0.0.1-SNAPSHOT")

(defn -main  [& args]
  (let [arg-map (dm/match-argv (dc/parse usage) args)]
    (cond
     (or (nil? arg-map)
         (arg-map "--help")
         (arg-map "-h"))
     (println usage)
         
     (or (arg-map "--version")
         (arg-map "-v"))
     (println version)

     :else
     (println "Running..."))))
