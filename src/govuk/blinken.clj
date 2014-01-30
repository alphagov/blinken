(ns govuk.blinken
  (:require [docopt.core :as dc]
            [docopt.match :as dm]
            [clojure.java.io :as io]
            [govuk.blinken.icinga :as icinga]
            [clj-yaml.core :as yaml]
            [govuk.blinken.protocols :as protocols]))


(def type-to-worker-fn {"icinga" icinga/create})

(defn- create-services [services-config]
  (filter #(-> % nil? not)
          (map (fn [[key config]]
                 (let [service-name (name key)]
                   (if (and (:type config) (:url config))
                     (if-let [worker-fn (type-to-worker-fn (:type config))]
                       (assoc {} :name service-name
                              :worker (worker-fn (:url config) (:options config)))
                       (println "Invalid type for service " service-name))
                     (println "Please provide both a type and url for" service-name))))
               services-config)))

(defn load-config [path]
  (if-let [file (io/as-file path)]
    (if (.exists (io/as-file file))
      (let [raw (yaml/parse-string (slurp file))]
        (assoc raw :services (create-services (:services raw)))))))


(def usage "Blinken

A dashboard that aggregates multiple alert sources

Usage:
  blinken <config-path>
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
     (let [config-path (arg-map "<config-path>")]
       (if-let [config (load-config config-path)]
         (println config)
         (println "Config file does not exist:" config-path))))))
