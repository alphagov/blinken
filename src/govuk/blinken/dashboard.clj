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

(defn generate-structure [services]
  [:head [:title "Blinken"]]
  [:body (for [service services]
           [:div {:class "service"}
            [:h1 (:name service)]
            (host-status (:hosts service))])])

(defn generate [services]
  (page/html5 (generate-structure services)))

