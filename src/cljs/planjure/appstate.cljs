(ns planjure.appstate
  (:require [om.core :as om :include-macros true]
            [planjure.plan :as plan]
            [cljs.core.async :refer [put! chan]]))

(def app-state
  (atom {:world (plan/random-world 20 20)
         :setup {:start [0 0] :finish [19 19]}
         :replan false ;; Replan on world change?
         :path []
         :draw-visited false ;; Mark visited nodes on canvas?
         :visited []
         :algo :dijkstra
         :last-run-time 0
         :last-cost 0 ;; Cost of last plan
         :canvas { :width 400 :height 400 }

         :world-size :small
         :world-size-options
           {
            :small  { :text "Small"  :size 20  :tile-size-px 20 }
            :medium { :text "Medium" :size 40  :tile-size-px 10 }
            :large  { :text "Large"  :size 100 :tile-size-px 4 }
           }

         :brush :brush
         :brush-options
           {
            :brush { :text "Brush" }
            :eraser { :text "Eraser" }
           }

         :brush-size :size3
         :brush-size-options
           {
            :size1 { :text "1" :matrix [[1]] }
            :size2 { :text "2" :matrix [[0 1 0]
                                        [1 1 1]
                                        [0 1 0]] }
            :size3 { :text "3" :matrix [[0 1 1 1 0]
                                        [1 1 2 1 1]
                                        [1 2 2 2 1]
                                        [1 1 2 1 1]
                                        [0 1 1 1 0]] }
           }

         :mouse-drawing false
         :mouse-moving-setup { :start false :finish false }
         :mouse-pos [0 0]
        }))

(def plan-chan (chan))

(defn update-world-state!
  "Update world state given app-state cursor or atom."
  [app-state new-world]
  (if (om/cursor? app-state)
    (om/update! app-state :world new-world)
    (swap! app-state assoc :world new-world)))

