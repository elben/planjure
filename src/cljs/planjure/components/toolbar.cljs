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

(defn size-selector-component [app-state owner]
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

(defn size-component [app-state owner]
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
                   (om/build size-selector-component
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
              (println v)
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
                  (om/update! app-state :algo (:value v))

                  (= :tool-selector (:kind v))
                  (om/update! app-state :selected-tool (:value v)))))))))

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
          (dom/div nil (name (app-state :selected-tool))))))))

