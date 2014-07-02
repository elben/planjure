(ns planjure.components.canvas
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [planjure.plan :as plan]
            [planjure.appstate :as appstate]))

; weight is 0 to 9
(defn weight-to-hex-color [weight]
  "Convert a weight (0 to 9) to CSS hex color."
  (if (<= weight 0)
    "#ffffff"
    (let [part (.toString (int (+ (/ 100 weight) 50)) 16)  ;; convert to base 16
          normalized-part (if (= (count part) 1) (str "0" part) part)] ;; turn "e" to "0e"
      (str "#" normalized-part normalized-part normalized-part))))

(defn get-selected-tile-size
  []
  (let [tile-size-name (get-in @appstate/app-state [:world-size-config :selected-size])
        value (get-in @appstate/app-state [:world-size-config :options tile-size-name :tile-size])]
    value))

(defn draw-rect-tile 
  ([context row col color] (draw-rect-tile context row col color (get-selected-tile-size)))
  ([context row col color size]
   (let [y (* row (get-selected-tile-size))
         x (* col (get-selected-tile-size))]
     (set! (.-fillStyle context) color)
     (.fillRect context x y size size))))

(defn draw-circle
  ([context row col color] (draw-circle context row col color (get-selected-tile-size)))
  ([context row col color size]
   (let [radius (/ size 2)
         y (+ (* row size) radius)
         x (+ (* col size) radius)]
     (set! (.-fillStyle context) color)
     (.beginPath context)
     (.arc context x y radius 0 (* 2 Math/PI) false)
     (.closePath context)
     (set! (.-strokeStyle context) color)
     (.stroke context))))

(defn draw-start-finish-marker [context row col]
  (let [color "#ff0000"]
    (draw-rect-tile context row col color)))

(defn draw-path-market [context row col])

(defn draw-path [context path]
  (doseq [node path]
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
    ; clear canvas
    (set! (.-width canvas) (.-width canvas))

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
    (draw-path context (:path app-state))))


(defn world-canvas-component [app-state owner]
  (reify
    ; Lifecycles:
    ; http://facebook.github.io/react/docs/component-specs.html#lifecycle-methods

    om/IDidMount
    (did-mount [this]
      (refresh-world app-state owner "world-canvas-ref"))

    ; Invoked directly after rendering. What triggers a render? An update in
    ; the component's data. And since what we passed to this component was the
    ; global app-state, any changes there wil cause this component to render
    ; and update.
    ;
    ; TODO: change this to only update if the :path, :world, :setup changes.
    ; E.g. no need to update if :algo changes
    om/IDidUpdate
    (did-update [a b c]
      ;; a - this
      ;; b - previous properties (old app-state)
      ;; c - previous state (internal state data of component; not used in Om?)
      (refresh-world app-state owner "world-canvas-ref"))

    om/IRender
    (render [this]
      (dom/canvas #js {:id "world-canvas" :width (get-in @appstate/app-state [:world-size-config :width-px]) :height (get-in @appstate/app-state [:world-size-config :height-px]) :className "world-canvas" :ref "world-canvas-ref"}))))

