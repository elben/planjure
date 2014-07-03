(ns planjure.appstate
  (:require [planjure.plan :as plan]))

(def app-state
  (atom {:world (plan/random-world 20 20)
         :setup {:start [0 0] :finish [19 19]}
         :path []
         :algo :dijkstra
         :last-run-time 0
         :world-size-config
           {
            :width-px 400 :height-px 400
            :selected-size :small
            :options
              {
               :small  { :name :small :text "Small"  :size 20  :tile-size 20 }
               :medium { :name :medium :text "Medium" :size 40  :tile-size 10 }
               :large  { :name :large :text "Large"  :size 200 :tile-size 2 }
              }
           }
        }))
