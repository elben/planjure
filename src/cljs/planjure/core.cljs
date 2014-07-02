(ns planjure.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [planjure.plan :as plan]))

(enable-console-print!)

(def plan-chan (chan))

(defn time-f [f]
  (fn [& args]
    (let [start (js/Date.)
          ret (apply f args)]
      {:time (- (js/Date.) start)
       :return ret})))


(def algorithms {:dijkstra {:name "Dijkstra" :fn (time-f plan/dijkstra)}
                 :dfs      {:name "Depth-first" :fn (time-f plan/dfs)}})

(def app-state
  (atom {:world (plan/random-world 20 20)
         :setup {:start [0 0] :finish [19 19]}
         :path []
         :algo :dijkstra
         :last-run-time 0
         :world-size-config {
                             :width-px 400 :height-px 400
                             :selected-size :small
                             :options { :small  { :name :small :text "Small"  :size 20  :tile-size 20 }
                                       :medium { :name :medium :text "Medium" :size 40  :tile-size 10 }
                                       :large  { :name :large :text "Large"  :size 200 :tile-size 2 }}}}))

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
  (let [tile-size-name (get-in @app-state [:world-size-config :selected-size])
        value (get-in @app-state [:world-size-config :options tile-size-name :tile-size])]
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
    (draw-path context (:path app-state))
    ))


(defn world-canvas-component [app-state owner]
  (reify
    ;; Lifecycles:
    ;; http://facebook.github.io/react/docs/component-specs.html#lifecycle-methods

    om/IDidMount
    (did-mount [this]
      (refresh-world app-state owner "world-canvas-ref"))

    ;; Invoked directly after rendering. What triggers a render? An update in
    ;; the component's data. And since what we passed to this component was the
    ;; global app-state, any changes there wil cause this component to render
    ;; and update.
    ;;
    ;; TODO: change this to only update if the :path, :world, :setup changes.
    ;; E.g. no need to update if :algo changes
    om/IDidUpdate
    (did-update [a b c]
      ;; a - this
      ;; b - previous properties (old app-state)
      ;; c - previous state (internal state data of component; not used in Om?)
      (refresh-world app-state owner "world-canvas-ref"))

    om/IRender
    (render [this]
      (dom/canvas #js {:id "world-canvas" :width (get-in app-state [:world-size-config :height-px]) :height (get-in app-state [:world-size-config :width-px]) :className "world-canvas" :ref "world-canvas-ref"}))))

(defn world-size-selector-component [app-state owner]
  ;; Expected app-state is one of the :options map in the world-size-config,
  ;; along with a :selected-size whose value is the currently selected size.
  (reify
    om/IInitState
    (init-state [_]
      {})

    ;; Use IRenderState here because we need to pass in the configuration-chan.
    om/IRenderState
    (render-state [_ {:keys [configuration-chan]}]
      (dom/span
        #js {:className (let [selected-class (if (:selected app-state) "selected")]
                          (str "world-size-selector item-selector " selected-class))
             ;; Need to @app-state here because cursors (in this case,
             ;; app-state) are not guaranteed to be consistent outside of the
             ;; React.js render cycles. Even though the cursor is used in the
             ;; RenderState lexical scope, it is in a javascript callback, thus
             ;; outside of the lifecycles.
             :onClick #(put! configuration-chan {:kind :world-size-selector :value @app-state})}
        (:text app-state)))))

(defn world-size-selector-main-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_] {})

    om/IRenderState
    (render-state [_ {:keys [configuration-chan]}]
      (let [selected-size (get-in app-state [:world-size-config :selected-size])]
        (apply dom/div nil
               (map
                 (fn
                   [size-name]
                   (om/build world-size-selector-component
                             (assoc (get-in app-state [:world-size-config :options size-name]) :selected (= selected-size size-name))
                             {:init-state {:configuration-chan configuration-chan}}))
                 [:small :medium :large]))))))

(defn toolbar-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_]
      {:plan-chan plan-chan
       :configuration-chan (chan)})

    om/IWillMount
    (will-mount [_]
      (let [configuration-chan (om/get-state owner :configuration-chan)]
        (go
          (while true
            (let [[v ch] (alts! [plan-chan configuration-chan])]
              (when (= ch plan-chan)
                (let [algo-fn ((algorithms (:algo @app-state)) :fn)
                      result (algo-fn (:world @app-state) (:setup @app-state))]
                  ;; Need to use @ here because of cursors. I don't understand, but get
                  ;; this error:  Cannot manipulate cursor outside of render phase, only
                  ;; om.core/transact!, om.core/update!, and cljs.core/deref operations
                  ;; allowed. Explained here:
                  ;; https://github.com/swannodette/om/wiki/Basic-Tutorial#debugging-om-components
                  (om/update! app-state :last-run-time (result :time))
                  (om/update! app-state :path (result :return))))
              (when (= ch configuration-chan)
                (cond
                  (= :world-size-selector (:kind v))
                  (let [world-size (get-in v [:value :size])
                        selected-size (get-in v [:value :name])]
                    (om/update! app-state [:world-size-config :selected-size] selected-size)
                    (om/update! app-state :world (plan/random-world world-size world-size)))

                  (= :algorithm (:kind v))
                  (om/update! app-state :algo (:value v)))))))))

    om/IDidMount
    (did-mount [this] nil)

    om/IRender
    (render [this]
      (dom/div
        nil
        (dom/div
          nil
          (dom/div #js {:className "section-title"} "World Size")
          (dom/div #js {:className "section-wrapper"}
            (om/build world-size-selector-main-component
              app-state
              {:init-state {:configuration-chan (om/get-state owner :configuration-chan)}})))

        (dom/div
          nil
          (dom/div #js {:className "section-title"} "Algorithm")
          (dom/div #js {:className "section-wrapper"}
            (dom/div
              nil
              (apply dom/select #js {:id "algorithm"
                                     ; use name to convert keyword to string. Easier to deal
                                     ; with strings in DOM, instead of keywords.
                                     :value (name (:algo app-state))
                                     :onChange #(put! (om/get-state owner :configuration-chan)
                                                      ; Grab event's event.target.value, which
                                                      ; will be the selected option.
                                                      ; See: http://facebook.github.io/react/docs/forms.html#why-select-value
                                                      {:kind :algorithm :value (keyword (.. % -target -value))})}

                     ;; Value is the string version of the key name.
                     ;; Display text is the name of the algorithm.
                     (map #(dom/option #js {:value (name (first %))} (:name (last %))) algorithms))

              ;; This uses core.async to communicate between components (namely, on
              ;; click, update the path state).
              ;; https://github.com/swannodette/om/wiki/Basic-Tutorial#intercomponent-communication
              ;; This grabs plan-chan channel, and puts "plan!" in the channel. The
              ;; world canvas component listens to this global channel to plan new paths.
              (dom/button #js {:onClick #(put! (om/get-state owner :plan-chan) "plan!")} "Plan Path"))))
        
        (dom/div
          nil
          (dom/div #js {:className "section-title"} "Status")
          (dom/div #js {:className "section-wrapper"}
            (om/build status-component app-state)))))))

(defn status-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IWillMount
    (will-mount [_] nil)

    om/IDidMount
    (did-mount [_] nil)

    om/IRender
    (render [_]
      (dom/div
        nil
        ; (dom/h2 nil "Status")
        (dom/div
          #js {:className :running-time}
          (dom/span nil "Running time: ")
          (dom/span nil (str (/ (app-state :last-run-time) 1000) " seconds")))))))


(om/root world-canvas-component app-state
         {:target (. js/document (getElementById "world"))})

(om/root toolbar-component app-state
         {:target (. js/document (getElementById "toolbar"))})

; (om/root status-component app-state
;          {:target (. js/document (getElementById "status"))})
