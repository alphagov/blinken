(ns govuk.blinken.dashboard
  (:import [java.util Date]
           [java.text SimpleDateFormat])
  (:require [hiccup.core :as hiccup]
            [hiccup.page :as page]))

(defn host-status [status]
  (if status
    (let [num-down (count (:down status))]
      [:div {:class "host-status"}
       [:h3 "Hosts"]
       [:ul {:class "status-list"}
        [:li {:class "ok"} (count (:up status))]
        [:li {:class "critical"} num-down]]
       (if (pos? num-down)
         [:ul {:class "down-hosts"}
          (for [down-host (:down status)]
            [:li down-host])])])
    [:div {:class "host-status"}
     [:h2 "Hosts"] [:div "No data"]]))

(defn- list-alerts [alerts status-class]
  (let [max-i (dec (count alerts))]
    (map-indexed (fn [i alert]
                   [:tr {:class (str "alert " status-class)}
                    [:td {:class (str "status " status-class)}]
                    [:td {:class "host"} (:host alert)]
                    [:td {:class "name"} (:name alert)]
                    [:td {:class "info"} [:pre (:info alert)]]])
                 alerts)))

(defn alerts [alerts]
  (if alerts
    (let [num-ok (count (:ok alerts))
          num-warning (count (:warning alerts))
          num-critical (count (:critical alerts))
          num-unknown (count (:unknown alerts))]
      [:div {:class "alerts"}
       [:h3 "Alerts"]
       [:ul {:class "status-list"}
        [:li {:class "ok"} num-ok]
        [:li {:class "warning"} num-warning]
        [:li {:class "critical"} num-critical]
        [:li {:class "unknown"} num-unknown]]
       (if (pos? (+ num-warning num-critical num-unknown))
         [:table {:class "problem-alerts"}
          [:thead [:tr [:td {:class "status"}] [:td {:class "host"} "Host"] [:td {:class "name"} "Name"] [:td {:class "info"} "Info"]]]
          (list-alerts (:critical alerts) "critical")
          (list-alerts (:warning alerts) "warning")
          (list-alerts (:unknown alerts) "unknown")])])
    [:div {:class "alerts"}
     [:h2 "Alerts"]
     [:div "No data"]]))

(defn- format-timestamp [timestamp]
  (if timestamp
     (let [date (Date. timestamp)
           formatter (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")]
       (.format formatter date))
     "-"))

(defn environment-overview [environment]
  (let [critical-count (-> environment :alerts :critical count)
        warning-count (-> environment :alerts :warning count)
        error (-> environment :alerts :error)
        level (cond (-> environment :alerts not) :no-data
                    (-> environment :alerts :error) :no-data
                    (pos? critical-count) :critical
                    (pos? warning-count) :warning
                    :else :ok)]
    [:li {:class (str "environment-overview " (name level))}
     [:h2 error]
     [:a {:href (str "/" (:group-id environment) "/" (:id environment))}
      [:h3 (:name environment) [:small {:class "timestamp"} (format-timestamp (-> environment :timestamp :alerts))]]
      (if (not (or (= level :ok) (= level :no-data)))
        [:table
         (if (= level :critical) [:tr [:td {:class "label"} "Criticals"] [:td {:class "number critical"} critical-count]])
         [:tr [:td {:class "label"} "Warnings"] [:td {:class "number"} warning-count]]])]]))

(defn group-overview [group]
  [:ul {:class "group-overview"}
   [:li {:class "header"} [:h2 [:a {:href (str "/" (:id group))} (:name group)]]]
   (for [environment (:environments group)]
     (environment-overview environment))])

(defn groups-overview [groups]
  (map group-overview groups))

(defn environment-detail [environment]
  (let [error (-> environment :alerts :error)]
    [:div {:class "environment"}
     [:h2 (:name environment)]
     (if error
       [:h2 error]
       (do
         (host-status (:hosts environment))
         (alerts (:alerts environment))))]))

(defn environments-detail [environments]
  (for [environment environments]
    [:div {:class "environment-container"}
     (environment-detail environment)]))

(defn generate-structure [home-link? & body]
  [:html
   [:head
    [:title  "Blinken"]
    [:meta {:name "viewport" :content "width=device-width"}]
    [:meta {:http-equiv "refresh" :content "10"}]
    (page/include-css "/static/main.css")
    (page/include-css "/static/dashboard.css")
    (page/include-css "/static/detail.css")]
   [:body
    (if home-link? [:header [:a {:href "/"} "<< Return to dashboard"]])
    body]])

(defn generate [home-link? & body]
  (page/html5 (generate-structure home-link? body)))
