(ns govuk.blinken
  (:require [docopt.core :as dc]
            [docopt.match :as dm]
            [clojure.java.io :as io]
            [govuk.blinken.service.icinga :as icinga]
            [govuk.blinken.service.sensu :as sensu]
            [clj-yaml.core :as yaml]
            [org.httpkit.server :as httpkit]
            [govuk.blinken.service :as service]
            [govuk.blinken.routes :as routes]))


(def type-to-worker-fn {"icinga" icinga/create
                        "sensu" sensu/create})

(defn- create-services [services-config type-to-worker-fn]
  (reduce (fn [services [key config]]
            (if (and (:type config) (:url config))
              (if-let [worker-fn (type-to-worker-fn (:type config))]
                (assoc services (name key) {:name (get config :name (name key))
                                            :worker (worker-fn (:url config)
                                                               (:options config))})
                (do (println "Invalid type for service " (name key))
                    services))
              (do (println "Please provide both a type and url for" (name key))
                  services)))
          {} services-config))

(defn load-config [path type-to-worker-fn]
  (if-let [file (io/as-file path)]
    (if (.exists (io/as-file file))
      (let [raw (yaml/parse-string (slurp file))]
        (assoc raw :services (create-services (:services raw) type-to-worker-fn))))))


(def usage "Blinken

A dashboard that aggregates multiple alert sources

Usage:
  blinken [options] <config-path>
  blinken -h | --help
  blinken -v | --version

Options:
  --port=<port>  Port for web server. [default:8080]
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
     (let [config-path (arg-map "<config-path>")
           port (Integer/parseInt (arg-map "--port"))]
       (if-let [config (load-config config-path type-to-worker-fn)]
         (do (doseq [[key config] (:services config)]
               (service/start (:worker config)))
             (httpkit/run-server (routes/build (:services config))
                                 {:port port})
             (println "Started web server on" port))
         (println "Config file does not exist:" config-path))))))
