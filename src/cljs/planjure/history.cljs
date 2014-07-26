(ns planjure.history
  (:require [planjure.appstate :as appstate]
            [om.core :as om]))

;; Reference:
;; https://github.com/jackschaedler/goya/blob/master/src/cljs/goya/timemachine.cljs

(def world-history (atom []))
(def world-future (atom []))

(defn push-world [world]
  (swap! world-history conj world)
  ;; When pushing to the history, reset the redo states.
  (reset! world-future []))

(defn move-stack [from-stack to-stack]
  "Push the current world state into to-stack. Pop off latest state (if any)
  from from-stack and put in current state."
  (if (> (count @from-stack) 0)
    (let [current-world (:world @appstate/app-state)
          prev-world (last @from-stack)]
      (swap! from-stack pop)
      (swap! to-stack conj current-world)
      (reset! appstate/app-state (assoc-in @appstate/app-state [:world] prev-world))
      prev-world)))

(defn undo []
  (move-stack world-history world-future))
    
(defn redo []
  (move-stack world-future world-history))

(defn reset []
  (reset! world-history [])
  (reset! world-future []))

(defn undoable []
  (> (count @world-history) 0))

(defn redoable []
  (> (count @world-future) 0))
