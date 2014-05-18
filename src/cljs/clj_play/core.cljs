(ns clj-play.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clj-play.plan :as plan]))

(enable-console-print!)

(def app-state
  (atom {:world
         [[1 1 1 1 1]
          [1 1 1 1 1]
          [9 9 9 1 1]
          [1 9 9 1 9]
          [1 1 1 1 9]]
         :setup {:start [4 0] :finish [0 1]}
         :path [[4 0] [4 1]]}))

(println "hello")

;(def world-canvas-elem (. js/document (getElementById "world-canvas")))

;(defn clear-world-canvas []
;  (set! (.-width world-canvas-elem) (.-width world-canvas-elem)))


;; Each tile is 20x20
;; World, for now, is 200 x 200 (10 tiles x 10 tiles)

; weight is 0 to 9
(defn weight-to-hex-color [weight]
  "Convert a weight (0 to 9) to CSS hex color."
  (if (<= weight 0)
    "#ffffff"
    (let [part (.toString (int (+ (/ 100 weight) 50)) 16)  ;; convert to base 16
          normalized-part (if (= (count part) 1) (str "0" part) part)] ;; turn "e" to "0e"
      (str "#" normalized-part normalized-part normalized-part))))


(defn draw-rect-tile 
  ([context row col color] (draw-rect-tile context row col color 20))
  ([context row col color size]
   (let [y (* row 20)
         x (* col 20)]
     (set! (.-fillStyle context) color)
     (.fillRect context x y size size))))

(defn draw-circle
  ([context row col color] (draw-circle context row col color 10))
  ([context row col color size]
   (let [y (+ (* row 20) 10)
         x (+ (* col 20) 10)]
     (set! (.-fillStyle context) color)
     (.beginPath context)
     (.arc context x y size 0 (* 2 Math/PI) false)
     (.closePath context)
     (set! (.-strokeStyle context) color)
     (.stroke context))))

(defn draw-start-finish-marker [context row col]
  (let [color "#ff0000"]
    (draw-rect-tile context row col color)))

(defn draw-path-market [context row col])

(defn draw-path [context path]
  (doseq [node path]
    (println node)
    (draw-circle context (nth node 0) (nth node 1) "#00ff00")))

;; context - Canvas context
;; row -
;; col
;; weight - 0 to 9
(defn draw-tile [context row col weight]
  (let [color (weight-to-hex-color weight)]
    (draw-rect-tile context row col color)))

(defn refresh-world [app-state owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        context (.getContext canvas "2d")
        world (:world app-state)
        setup (:setup app-state)]
    ; draw world
    (dotimes [row (count world)]
      (dotimes [col (count (nth world row))]
        (draw-tile context row col (nth (nth world row) col))))

    ;draw start/finish
    (let [[start-row start-col] (get-in app-state [:setup :start])
          [finish-row finish-col] (get-in app-state [:setup :finish])]
      (draw-start-finish-marker context start-row start-col)
      (draw-start-finish-marker context finish-row finish-col))

    ; draw path (if exists)
    (draw-path context (:path app-state))
    ))


(defn world-canvas-component [app-state owner]
  (reify
    om/IDidMount
    (did-mount [this] (refresh-world app-state owner "world-canvas-ref"))

    om/IRender
    (render [this]
      (dom/canvas #js {:id "world-canvas" :width 200 :height 200 :className "world-canvas" :ref "world-canvas-ref"}))))


(om/root world-canvas-component app-state
         {:target (. js/document (getElementById "world"))})
