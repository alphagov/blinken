(ns govuk.blinken
  (:require [docopt.core :as dc]
            [docopt.match :as dm]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [govuk.blinken.service.icinga :as icinga]
            [govuk.blinken.service.sensu :as sensu]
            [clj-yaml.core :as yaml]
            [org.httpkit.server :as httpkit]
            [govuk.blinken.service :as service]
            [govuk.blinken.system :refer [new-system]]
            [govuk.blinken.routes :as routes]))

(def valid-types #{"icinga" "sensu"})

(defn- create-environments [environments-config]
  (reduce (fn [environments [key config]]
            (if (and (:type config) (:url config))
              (if (valid-types (:type config))
                (assoc environments (name key) {:name (get config :name (name key))
                                                :type (:type config)
                                                :url (:url config)
                                                :options (:options config)})
                (do (log/warn "Invalid type for environment " (name key))
                    environments))
              (do (log/warn "Please provide both a type and url for" (name key))
                  environments)))
          {} environments-config))

(defn- create-groups [groups-config]
  (reduce (fn [groups [key config]]
            (let [environments (create-environments (:environments config))]
              (if (empty? environments)
                (do (log/warn "No environments for group" (name key))
                  groups)
                (assoc groups (name key) {:name (get config :name (name key))
                                          :environments environments}))))
          {} groups-config))

(defn load-config [path]
  (if-let [file (io/as-file path)]
    (if (.exists (io/as-file file))
      (let [raw (yaml/parse-string (slurp file))]
        (update-in raw [:groups] create-groups)))))


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
       (if-let [config (load-config config-path)]
         (let [system (new-system port config)]
           (component/start system))
         (log/error "Config file does not exist:" config-path))))))
