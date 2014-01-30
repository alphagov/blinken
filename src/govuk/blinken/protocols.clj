(ns govuk.blinken.protocols)

(defprotocol Service
  (get-status [this] "Get a map of hosts and alerts"))

