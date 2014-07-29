(ns planjure.appstate
  (:require [planjure.plan :as plan]))

(def app-state
  (atom {:world (plan/random-world 20 20)
         :setup {:start [0 0] :finish [19 19]}
         :path []
         :algo :dijkstra
         :last-run-time 0
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

         :brush-size :size1
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
         :mouse-pos [0 0]
        }))
