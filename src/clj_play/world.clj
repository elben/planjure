(ns clj-play.world)

; (defprotocol World
;   (cost-at [world r c]
;       "Returns cost of (r, c) tile."))

(defn cost-at [world r c]
  (((world :map) c) r))

