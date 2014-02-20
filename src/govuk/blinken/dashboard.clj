(ns govuk.blinken.dashboard
  (:require [hiccup.core :as hiccup]
            [hiccup.page :as page]))


(defn host-status [status]
  (if status
    (let [num-down (count (:down status))]
      [:div {:class "host-status"}
       [:h3 "Hosts"]
       [:ul {:class "status-list"}
        [:li "Up: " [:span {:class "ok"} (count (:up status))]]
        [:li "Down: " [:span {:class "critical"} num-down]]]
       (if (> num-down 0)
         [:ul {:class "down-hosts"}
          (for [down-host (:down status)]
            [:li down-host])])])
    [:div {:class "host-status"}
     [:h2 "Hosts"] [:div "No data"]]))

(defn- list-alerts [alerts status-class]
  (let [max-i (- (count alerts) 1)]
    (map-indexed (fn [i alert]
                   [:tr {:class (str "alert " (if (= i max-i) status-class))}
                    [:td {:class (str "status " status-class)}]
                    [:td {:class "name"} (:name alert)]
                    [:td {:class "host"} (:host alert)]
                    [:td {:class "info"} (:info alert)]])
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
        [:li "Ok: " [:span {:class "ok"} num-ok]]
        [:li "Warning: " [:span {:class "warning"} num-warning]]
        [:li "Critical: " [:span {:class "critical"} num-critical]]
        [:li "Unknown: " [:span {:class "unknown"} num-unknown]]]
       (if (> (+ num-warning num-critical num-unknown) 0)
         [:table {:class "problem-alerts"}
          [:thead [:tr [:td] [:td "Host"] [:td "Name"] [:td "Info"]]]
          (list-alerts (:critical alerts) "critical")
          (list-alerts (:warning alerts) "warning")
          (list-alerts (:unknown alerts) "unknown")])])
    [:div {:class "alerts"}
     [:h2 "Alerts"]
     [:div "No data"]]))

(defn environment-overview [environment]
  (let [critical-count (-> environment :alerts :critical count)
        warning-count (-> environment :alerts :warning count)
        level (cond (-> environment :alerts not) :no-data
                    (> critical-count 0) :critical
                    (> warning-count 0) :warning
                    :else :ok)]
    [:a {:href (str "/" (:group-id environment) "/" (:id environment))
         :class (str "environment-overview " (name level))} 
     [:h3 (:name environment)]
     (if (not (or (= level :ok) (= level :no-data)))
       [:div {:class "count"}
        (if (= level :critical) [:div {:class "critical"} critical-count])
        [:div {:class "warning"} warning-count]])]))

(defn group-overview [group]
  [:div {:class "group-overview"}
   [:h2 [:a {:href (str "/" (:id group))} (:name group)]]
   (for [environment (:environments group)]
     (environment-overview environment))])

(defn groups-overview [groups]
  (map group-overview groups))

(defn environment-detail [environment]
  [:div {:class "environment"}
   (host-status (:hosts environment))
   (alerts (:alerts environment))])

(defn environments-detail [environments]
  (for [environment environments]
    [:div {:class "environment-container"}
     [:h2 (:name environment)]
     (environment-detail environment)]))

(defn generate-structure [title & body]
  [:html
   [:head
    [:title (str title " - Blinken")]
    [:meta {:http-equiv "refresh" :content "10"}]
    (page/include-css "/static/main.css")]
   [:body
    [:header
     [:a {:href "/"} "Home"]
     [:h1 title]]
    body]])

(defn generate [title & body]
  (page/html5 (generate-structure title body)))

