(ns clj-play.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [clj-play.plan :as plan]))

(enable-console-print!)

(def plan-chan (chan))

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
    ;; Lifecycles:
    ;; http://facebook.github.io/react/docs/component-specs.html#lifecycle-methods

    om/IWillMount
    (will-mount [this]
      (go (loop []
        (let [trigger (<! plan-chan)]
          (println trigger)
          (println "path before")
          (println (:path @app-state))
          ;; Need to use @ here because of cursors. I don't understand, but get
          ;; this error:  Cannot manipulate cursor outside of render phase, only
          ;; om.core/transact!, om.core/update!, and cljs.core/deref operations
          ;; allowed. Explained here:
          ;; https://github.com/swannodette/om/wiki/Basic-Tutorial#debugging-om-components
          (om/update! app-state :path (plan/dfs (:world @app-state) (:setup @app-state)))
          (println "path after")
          (println (:path @app-state))
          (recur)))))

    om/IDidMount
    (did-mount [this]
      (refresh-world app-state owner "world-canvas-ref"))

    ;; Invoked directly after rendering. What triggers a render? An update in
    ;; the component's data. And since what we passed to this component was the
    ;; global app-state, any changes there wil cause this component to render
    ;; and update.
    om/IDidUpdate
    (did-update [a b c]
      ;; a - this
      ;; b - previous properties (old app-state)
      ;; c - previous state (internal state data of component; not used in Om?)
      (println "change!")
      (println a)
      (println b)
      (println c)
      (refresh-world app-state owner "world-canvas-ref"))

    om/IRender
    (render [this]
      (dom/canvas #js {:id "world-canvas" :width 200 :height 200 :className "world-canvas" :ref "world-canvas-ref"}))))

(defn toolbar-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_] {:plan-chan plan-chan})

    om/IDidMount
    (did-mount [this] nil)

    om/IRender
    (render [this]
      ;; This uses core.async to communicate between components (namely, on
      ;; click, update the path state).
      ;; https://github.com/swannodette/om/wiki/Basic-Tutorial#intercomponent-communication
      ;; This grabs plan-chan channel, and puts "plan!" in the channel. The
      ;; world canvas component listens to this global channel to plan new paths.
      (dom/button #js {:onClick #(put! (om/get-state owner :plan-chan) "plan!")} "Plan Path"))))

(om/root world-canvas-component app-state
         {:target (. js/document (getElementById "world"))})

(om/root toolbar-component app-state
         {:target (. js/document (getElementById "toolbar"))})
