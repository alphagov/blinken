(ns govuk.blinken.dashboard
  (:require [hiccup.core :as hiccup]
            [hiccup.page :as page]))


(defn host-status [status]
  (let [num-down (count (:down status))]
    [:div {:class "host-status"}
     [:h2 "Hosts"]
     [:div "Up: " [:span {:class "up"} (count (:up status))]]
     [:div "Down: " [:span {:class "down"} num-down]]
     (if (> num-down 0)
       [:ul {:class "down-hosts"}
        (for [down-host (:down status)]
          [:li down-host])])]))

(defn- list-alerts [alerts]
  (for [alert alerts]
    [:li {:class "alert"}
     [:div {:class "name"} (:name alert)]
     [:div {:class "host"} (:host alert)]
     [:div {:class "info"} (:info alert)]]))

(defn alerts [alerts]
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
        (list-alerts (:unknown alerts))])]))

(defn generate-structure [services]
  [:head [:title "Blinken"]]
  [:body (for [service services]
           [:div {:class "service"}
            [:h1 (:name service)]
            (host-status (:hosts service))
            (alerts (:alerts service))])])

(defn generate [services]
  (page/html5 (generate-structure services)))

