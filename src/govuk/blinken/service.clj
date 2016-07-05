(ns govuk.blinken.service)

(defprotocol Service
  (get-status [this] "Get a map of hosts and alerts"))

