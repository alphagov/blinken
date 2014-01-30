(ns govuk.blinken.dashboard
  (:require [hiccup.core :as hiccup]))


(defn host-status [status]
  (let [num-down (count (:down status))]
    [:div {:class "host-status"}
     [:div {:class "up"} (count (:up status))]
     [:div {:class "down"} num-down]
     (if (> num-down 0)
       [:ul {:class "down-hosts"}
        (for [down-host (:down status)]
          [:li down-host])])]))


