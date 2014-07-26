(ns planjure.components.toolbar
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [planjure.plan :as plan]
            [planjure.history :as history]))

(def plan-chan (chan))

(defn time-f [f]
  (fn [& args]
    (let [start (js/Date.)
          ret (apply f args)]
      {:time (- (js/Date.) start)
       :return ret})))

(def algorithms {:dijkstra {:name "Dijkstra" :fn (time-f plan/dijkstra)}
                 :dfs      {:name "Depth-first" :fn (time-f plan/dfs)}})

(defn item-selector-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IRenderState
    ;; configuration-chan - Config chan to push into.
    ;; tool-kind - The tool kind as specified by the appstate key. e.g.
    ;;             :world-size.
    ;; tool-name - Name of specific tool for this selector. e.g. :small.
    ;; tool-text - Display text.
    ;; is-disabled-fn - Optional fn that returns true when selector should be
    ;;                  disabled.
    (render-state [_ {:keys [configuration-chan tool-kind tool-name tool-text is-disabled-fn]}]
      (let [selected-css (when (= tool-name (tool-kind app-state)) "selected")
            disabled-css (when (and is-disabled-fn (is-disabled-fn)) "disabled")
            css-class (str selected-css " " disabled-css)]
        (dom/span
          #js {:className (str "item-selector " css-class)
               :onClick #(put! configuration-chan {:kind :tool-selector :tool-kind tool-kind :value tool-name})}
          tool-text)))))

(defn editor-component [app-state owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [configuration-chan]}]
      (dom/div
        nil
        (apply dom/div
               #js {:className "button-row"}
               (for [[brush-tool-name {:keys [text]}] (:brush-options app-state)]
                 (om/build item-selector-component app-state {:init-state {:configuration-chan configuration-chan
                                                                           :tool-kind :brush
                                                                           :tool-name brush-tool-name
                                                                           :tool-text text}})))
        (apply dom/div
               #js {:className "button-row"}
               (for [[size-name {:keys [text]}] (:brush-size-options app-state)]
                 (om/build item-selector-component app-state {:init-state {:configuration-chan configuration-chan
                                                                           :tool-kind :brush-size
                                                                           :tool-name size-name
                                                                           :tool-text text}})))))))

(defn size-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_] {})

    om/IRenderState
    (render-state [_ {:keys [configuration-chan]}]
      (let [selected-size (:world-size app-state)]
        (apply dom/div nil
               (for [[size-name {:keys [text] :as size-opts}] (:world-size-options app-state)]
                 (om/build item-selector-component
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
                (case (:kind v)
                  :algorithm
                  (om/update! app-state :algo (:value v))

                  :tool-selector
                  (case (:tool-kind v)
                    :world-size
                    (let [world-size (:value v)
                          world-num-tiles (get-in @app-state [:world-size-options world-size :size])
                          last-row-col (dec world-num-tiles)]
                      (om/update! app-state :world-size world-size)
                      (om/update! app-state :world (plan/random-world world-num-tiles world-num-tiles))
                      (om/update! app-state [:setup :finish] [last-row-col last-row-col])
                      (om/update! app-state :path [])
                      (history/reset))

                    :history
                    (case (:value v)
                      :undo
                      (history/undo)
                      :redo
                      (history/redo))

                    ;; Default case
                    (om/update! app-state (:tool-kind v) (:value v))))))))))

    om/IDidMount
    (did-mount [this] nil)

    om/IRenderState
    (render-state [this {:keys [configuration-chan]}]
      (dom/div
        nil
        (dom/div
          nil
          (dom/div #js {:className "section-title"} "World")
          (dom/div #js {:className "section-wrapper"}
            (om/build size-component
              app-state
              {:init-state {:configuration-chan configuration-chan}})))

        (dom/div
          nil
          (dom/div #js {:className "section-title"} "Editor")
          (dom/div #js {:className "section-wrapper"}
            (om/build editor-component
              app-state
              {:init-state {:configuration-chan configuration-chan}})))

        (dom/div
          nil
          (dom/div #js {:className "section-title"} "History")
          (dom/div #js {:className "section-wrapper"}
                   (dom/div
                     #js {:className "button-row"}
                     (om/build item-selector-component app-state {:init-state {:configuration-chan configuration-chan
                                                                               :tool-kind :history
                                                                               :tool-name :undo
                                                                               :tool-text "Undo"
                                                                               :is-disabled-fn (complement history/undoable)}})
                     (om/build item-selector-component app-state {:init-state {:configuration-chan configuration-chan
                                                                               :tool-kind :history
                                                                               :tool-name :redo
                                                                               :tool-text "Redo"
                                                                               :is-disabled-fn (complement history/redoable)}}))))
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
