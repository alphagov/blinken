(ns govuk.blinken.dashboard
  (:require [hiccup.core :as hiccup]
            [hiccup.page :as page]))


(defn host-status [status]
  (if status
    (let [num-down (count (:down status))]
      [:div {:class "host-status"}
       [:h2 "Hosts"]
       [:div "Up: " [:span {:class "up"} (count (:up status))]]
       [:div "Down: " [:span {:class "down"} num-down]]
       (if (> num-down 0)
         [:ul {:class "down-hosts"}
          (for [down-host (:down status)]
            [:li down-host])])])
    [:div {:class "host-status"}
     [:h2 "Hosts"] [:div "No data"]]))

(defn- list-alerts [alerts]
  (for [alert alerts]
    [:li {:class "alert"}
     [:div {:class "name"} (:name alert)]
     [:div {:class "host"} (:host alert)]
     [:div {:class "info"} (:info alert)]]))

(defn alerts [alerts]  
  (if alerts
    (let [num-ok (count (:ok alerts))
          num-warning (count (:warning alerts))
          num-critical (count (:critical alerts))
          num-unknown (count (:unknown alerts))]
      [:div {:class "alerts"}
       [:h2 "Alerts"]
       [:div "Ok: " [:span {:class "ok"} num-ok]]
       [:div "Warning: " [:span {:class "warning"} num-warning]]
       [:div "Critical: " [:span {:class "critical"} num-critical]]
       [:div "Unknown: " [:span {:class "unknown"} num-unknown]]
       (if (> (+ num-warning num-critical num-unknown) 0)
         [:ul {:class "problem-alerts"}
          (list-alerts (:critical alerts))
          (list-alerts (:warning alerts))
          (list-alerts (:unknown alerts))])])
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
    (page/include-css "/static/main.css")]
   [:body
    [:header
     [:a {:href "/"} "Home"]
     [:h1 title]]
    body]])

(defn generate [title & body]
  (page/html5 (generate-structure title body)))

