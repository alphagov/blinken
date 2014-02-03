(ns govuk.blinken.service)

(defprotocol Service
  (start [this] "Start pulling data")
  (get-status [this] "Get a map of hosts and alerts")
  (stop [this] "Stop pulling data"))

