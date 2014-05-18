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
         :path []}))

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


(defn draw-tile [context row col color]
  (let [y (* row 20)
        x (* col 20)]
    (set! (.-fillStyle context) color)
    (.fillRect context x y 20 20)))

(defn draw-start-finish-node [canvas row col]
  (let [context (.getContext canvas "2d")
        color "#ff0000"]
    (draw-tile context row col color)))


;; canvas - DOM canvas
;; row -
;; col
;; weight - 0 to 9
(defn draw-node [canvas row col weight]
  (let [context (.getContext canvas "2d")
        color (weight-to-hex-color weight)]
    (draw-tile context row col color)))

(defn refresh-world [app-state owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        context (.getContext canvas "2d")
        world (:world app-state)
        setup (:setup app-state)]
    ; draw world
    (dotimes [row (count world)]
      (dotimes [col (count (nth world row))]
        (draw-node canvas row col (nth (nth world row) col))))

    ;draw start/finish
    (let [[start-row start-col] (get-in app-state [:setup :start])
          [finish-row finish-col] (get-in app-state [:setup :finish])]
      (draw-start-finish-node canvas start-row start-col)
      (draw-start-finish-node canvas finish-row finish-col))

    ; draw path (if exists)
    ;; TODO
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
