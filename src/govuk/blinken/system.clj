(ns govuk.blinken.system
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as httpkit]
            [govuk.blinken.routes :as routes]
            [govuk.blinken.service :as service]
            [govuk.blinken.service.sensu :as sensu]
            [govuk.blinken.service.icinga :as icinga]))

(defrecord WebServer [port routes-fn server-instance services]
  component/Lifecycle
  (start [web-server]
    (if server-instance
      web-server
      (let [new-instance (httpkit/run-server (routes-fn services)
                                             {:port port})]
        (log/infof "Started web server on http://localhost:%s" port)
        (assoc web-server :server-instance new-instance))))

  (stop [web-server]
    (if server-instance
      (do (server-instance)
          (log/infof "Stopped web server on http://localhost:%s" port)
          (assoc web-server :server-instance nil))
      web-server)))

(defn- new-web-server [port routes-fn]
  (map->WebServer {:port port :routes-fn routes-fn}))

(defn- environments [config]
  (apply merge (map (fn [[group-key group-config]]
                      (reduce (fn [dict [environment-key environment-config]]
                                (assoc dict (str group-key "/" environment-key)
                                       environment-config))
                              {} (:environments group-config)))
                    (:groups config))))

(defn- map-environments [environments-map map-fn]
  (reduce (fn [dict [env-key env-config]]
            (assoc dict env-key (map-fn env-config)))
          {} environments-map))

(defn- new-service [config]
  (case (:type config)
    "sensu" (sensu/create (:url config) (:options config))
    "icinga" (icinga/create (:url config) (:options config))
    (throw (IllegalArgumentException. (str "Invalid type of service: " (:type config))))))

(defrecord Services [environments-map service-instances]
  component/Lifecycle
  (start [services]
    (if service-instances
      services
      (let [new-service-instances (map-environments environments-map new-service)]
        (doseq [[key instance] new-service-instances] (component/start instance))
        (assoc services :service-instances new-service-instances))))
  
  (stop [services]
    (if service-instances
      (do (doseq [instance service-instances] (component/stop instance))
          (assoc services :service-instances nil))
      services))

  service/Service
  (get-status [services]
    (map-environments service-instances service/get-status)))

(defn- new-services [config]
  (map->Services {:environments-map (environments config)}))

(defn new-system [port config]
  (component/system-map
   :services (new-services config)
   :web (component/using
         (new-web-server port #(routes/build (:groups config) %))
         [:services])))
