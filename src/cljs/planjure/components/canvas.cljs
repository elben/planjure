(ns planjure.components.canvas
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]]
            [planjure.utils :as utils]
            [planjure.components.toolbar :as toolbar]
            [planjure.appstate :as appstate]
            [planjure.history :as history]))

(def update-world-time (atom 0))
(def canvas-redraw-time (atom 0))

(def mouse-chan (chan))

; weight is 0 to 9
; (defn weight-to-hex-color [weight]
;   "Convert a weight (0 to 9) to CSS hex color."
;   (if (<= weight 0)
;     "#ffffff"
;     (let [part (.toString (int (+ (/ 100 weight) 50)) 16)  ;; convert to base 16
;           normalized-part (if (= (count part) 1) (str "0" part) part)] ;; turn "e" to "0e"
;       (str "#" normalized-part normalized-part normalized-part))))

(def color-mapping
  ["#09738A", "#adc5ad" "#c6c294" "#7c9a53" "#578633" "#3b621a" "#2d5010" "#26470b"])
   ; "#756e68" "#9b9186" "#c0b1a3" "#dacec2" "#f2efeb"])

(defn weight-to-hex-color [weight] (color-mapping (dec weight)))

(defn get-selected-tile-size []
  (let [tile-size-name (:world-size @appstate/app-state)
        value (get-in @appstate/app-state [:world-size-options tile-size-name :tile-size-px])]
    value))

(defn draw-rect-tile 
  [context row col color size]
  (let [y (* row size)
        x (* col size)]
    (set! (.-fillStyle context) color)
    (.fillRect context x y size size)))

(defn draw-circle
  [context row col color size]
  (let [radius (/ size 2)
        y (+ (* row size) radius)
        x (+ (* col size) radius)]
    (set! (.-fillStyle context) color)
    (.beginPath context)
    (.arc context x y radius 0 (* 2 Math/PI) false)
    (.closePath context)
    (set! (.-strokeStyle context) color)
    (.stroke context)))

(defn draw-start-node [context row col size]
  (let [color "#000000"]
    (draw-rect-tile context row col color size)))

(defn draw-finish-node [context row col size]
  (let [color "#d02426"]
    (draw-rect-tile context row col color size)))


(defn draw-path [context path size]
  (doseq [node path]
    (draw-circle context (nth node 0) (nth node 1) "#00ff00" size)))

(defn draw-visited [context visited size]
  (let [width 2
        pos-start (fn [n] (- (+ (* n size) (/ size 2)) 1))]
    (doseq [node visited]
      (let [y (pos-start (first node))
            x (pos-start (second node))]
        (set! (.-fillStyle context) "rgba(255, 255, 255, 0.5)")
        (.fillRect context x y width width)))))

;; context - Canvas context
;; row -
;; col
;; weight - 0 to 9
(defn draw-tile [context row col weight size]
  (let [color (weight-to-hex-color weight)]
    (draw-rect-tile context row col color size)))

(defn refresh-world [app-state owner dom-node-ref]
  (let [canvas (om/get-node owner dom-node-ref)
        context (.getContext canvas "2d")
        world (:world app-state)
        setup (:setup app-state)
        size (get-selected-tile-size)]
    ; clear canvas
    (set! (.-width canvas) (.-width canvas))

    ; draw world
    (dotimes [r (count world)]
      (let [row (nth world r)]
        (dotimes [c (count row)]
          (draw-tile context r c (nth row c) size))))

    ; draw start/finish
    (let [[start-row start-col] (get-in app-state [:setup :start])
          [finish-row finish-col] (get-in app-state [:setup :finish])]
      (draw-start-node context start-row start-col size)
      (draw-finish-node context finish-row finish-col size))

    ; draw path (if exists)
    (draw-path context (:path app-state) size)
    (when (:draw-visited app-state)
      (draw-visited context (:visited app-state) size))))

(defn mouse-pos-at
  [canvas e]
  "Returns relative x, y position of canvas, given goog mouse event."
  {:x (.-offsetX e) :y (.-offsetY e)})

(defn tile-pos-at
  "Returns map {:x x, :y y}."
  [canvas e]
  ;; TODO need to close tile-size elsewhere for perf?
  (let [{:keys [x y]} (mouse-pos-at canvas e)
        tile-size (get-selected-tile-size)]
    {:x (max 0 (int (/ x tile-size))) :y (max 0 (int (/ y tile-size)))}))

(defn update-world!
  "Increase cost at x, y position in the world passed in via the app-state
  cursor."
  [app-state x y multiplier]
  (let [brush-size (:brush-size @app-state)
        matrix (get-in @app-state [:brush-size-options brush-size :matrix])
        new-world (utils/update-world (:world @app-state)
                                      matrix
                                      x y multiplier)]
    (appstate/update-world-state! app-state new-world)))

(defn erase-at
  [app-state tile-pos]
  (let [{:keys [x y]} tile-pos]
    (update-world! app-state x y -1)))

(defn paint-at
  [app-state tile-pos]
  (let [{:keys [x y]} tile-pos]
    (update-world! app-state x y 1)))

(defn mouse-moving?
  "Returns true if which (:start or :finish) node is being moved. Other return
  false."
  [which]
  (get-in @appstate/app-state [:mouse-moving-setup which]))

(defn place-node
  "Update which (:start or :finish) node to given node. Stop mouse moving."
  [app-state which node]
  (om/update! app-state [:setup which] node)
  (om/update! app-state [:mouse-moving-setup which] false))

(defn place-or-pick-up-node
  "Update which (:start or :finish) node to given node if mouse is moving.
  Otherwise, pick up node by starting mouse moving mode."
  [app-state which node]
  (if (get-in @app-state [:mouse-moving-setup which])
    (place-node app-state which node)
    (om/update! app-state [:mouse-moving-setup which] true)))

(defn move-node-from-canvas
  "Given our canvas and mouseevent, move our `which` node (either :start or
  :finish) to the position of the mouse event."
  [app-state canvas mouseevent which]
  (let [tile-pos (tile-pos-at canvas (:event mouseevent))
        node-pos [(:y tile-pos) (:x tile-pos)]]
    (om/update! app-state [:setup which] node-pos)))

(defn world-canvas-component [app-state owner]
  (reify
    ; Lifecycles:
    ; http://facebook.github.io/react/docs/component-specs.html#lifecycle-methods

    om/IInitState
    (init-state [_]
      {:mouse-chan mouse-chan})

    om/IDidMount
    (did-mount [this]
      (refresh-world app-state owner "world-canvas-ref")
      (let [world-canvas (om/get-node owner "world-canvas-ref")]
        ; (.addEventListener world-canvas "mousedown" #(println "down!") false)))
        (events/listen world-canvas "click" #(put! mouse-chan {:event % :mouseevent :click})) 
        (events/listen world-canvas "mousedown" #(put! mouse-chan {:event % :mouseevent :mousedown}))
        (events/listen world-canvas "mouseup" #(put! mouse-chan {:event % :mouseevent :mouseup}))
        (events/listen world-canvas "mousemove" #(put! mouse-chan {:event % :mouseevent :mousemove})))
      (let [canvas (om/get-node owner "world-canvas-ref")
            mouse-chan (om/get-state owner :mouse-chan)]
        (go
          (while true
            (let [mouseevent (<! mouse-chan)]
              (case (:mouseevent mouseevent)
                :click
                (let [tile-pos (tile-pos-at canvas (:event mouseevent))
                      node-pos [(:y tile-pos) (:x tile-pos)]]
                  ;; TODO so many similar looking checks for placing and picking
                  ;; up nodes. Simplify!!
                  (condp = node-pos
                    ;; Clicked on the start node
                    (get-in @app-state [:setup :start])
                    (place-or-pick-up-node app-state :start node-pos)

                    ;; Clicked on the finish node
                    (get-in @app-state [:setup :finish])
                    (place-or-pick-up-node app-state :finish node-pos)

                    ;; Else, we clicked in a node that isn't the start/finish.
                    ;; See if we're trying to put one of our start/finish nodes here.
                    (cond
                      (mouse-moving? :start)
                      (place-node app-state :start node-pos)

                      (mouse-moving? :finish)
                      (place-node app-state :finish node-pos))))

                ;; On mousedown, user starts drawing phase.
                :mousedown
                (let [world (:world @app-state)]
                  (om/update! app-state :mouse-drawing true)
                  ;; Commit the previous world into history
                  (history/push-world world))

                ;; Consider a mouseup an atomic commit of the user
                ;; brush stroke.
                :mouseup
                (let [world (:world @app-state)]
                  (om/update! app-state :mouse-drawing false))

                ;; Actually draw when user moves mouse.
                :mousemove
                (cond
                  ;; TODO simplify
                  (mouse-moving? :start)
                  (move-node-from-canvas app-state canvas mouseevent :start)

                  (mouse-moving? :finish)
                  (move-node-from-canvas app-state canvas mouseevent :finish)

                  (:mouse-drawing @app-state)
                  (let [tile-pos (tile-pos-at canvas (:event mouseevent))]
                    (case (:brush @app-state)
                      :eraser (erase-at app-state tile-pos)
                      :brush (paint-at app-state tile-pos))))))))))

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
      (dom/canvas #js {:id "world-canvas" :width (get-in app-state [:canvas :width]) :height (get-in app-state [:canvas :height]) :className "world-canvas" :ref "world-canvas-ref"}))))

