(ns planjure.components.toolbar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [planjure.plan :as plan]
            [planjure.components.editor :as editor]))

(def plan-chan (chan))

(defn time-f [f]
  (fn [& args]
    (let [start (js/Date.)
          ret (apply f args)]
      {:time (- (js/Date.) start)
       :return ret})))

(def algorithms {:dijkstra {:name "Dijkstra" :fn (time-f plan/dijkstra)}
                 :dfs      {:name "Depth-first" :fn (time-f plan/dfs)}})

(defn size-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_] {})

    om/IRenderState
    (render-state [_ {:keys [configuration-chan]}]
      (let [selected-size (:world-size app-state)]
        (apply dom/div nil
               (for [[size-name {:keys [text] :as size-opts}] (:world-size-options app-state)]
                 (om/build editor/item-selector-component
                           app-state
                           {:init-state {:configuration-chan configuration-chan
                                         :tool-kind :world-size
                                         :tool-name size-name
                                         :tool-text text}})))))))

(defn statistics-component [app-state owner]
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
        (dom/div
          #js {:className :running-time}
          (dom/div nil (str (/ (app-state :last-run-time) 1000) " seconds"))
          (dom/div nil (name (app-state :brush))))))))

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
              (println v)
              (when (= ch plan-chan)
                (let [algo-fn ((algorithms (:algo @app-state)) :fn)
                      result (algo-fn (:world @app-state) (:setup @app-state))]
                  ;; Need to @app-state here because cursors (in this case,
                  ;; app-state) are not guaranteed to be consistent outside of the
                  ;; React.js render/render-state cycles.
                  (om/update! app-state :last-run-time (result :time))
                  (om/update! app-state :path (result :return))))
              (when (= ch configuration-chan)
                (cond
                  (= :algorithm (:kind v))
                  (om/update! app-state :algo (:value v))

                  (= :tool-selector (:kind v))
                  (if (= (:tool-kind v) :world-size)
                    (let [world-size (:value v)
                          world-num-tiles (get-in @app-state [:world-size-options world-size :size])]
                      (om/update! app-state :world-size world-size)
                      (om/update! app-state :world (plan/random-world world-num-tiles world-num-tiles)))
                    (om/update! app-state (:tool-kind v) (:value v))))))))))

    om/IDidMount
    (did-mount [this] nil)

    om/IRender
    (render [this]
      (dom/div
        nil
        (dom/div
          nil
          (dom/div #js {:className "section-title"} "World")
          (dom/div #js {:className "section-wrapper"}
            (om/build size-component
              app-state
              {:init-state {:configuration-chan (om/get-state owner :configuration-chan)}})))

        (dom/div
          nil
          (dom/div #js {:className "section-title"} "Editor")
          (dom/div #js {:className "section-wrapper"}
            (om/build editor/editor-component
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
          (dom/div #js {:className "section-title"} "Statistics")
          (dom/div #js {:className "section-wrapper"}
            (om/build statistics-component app-state)))))))
