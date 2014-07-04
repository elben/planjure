(ns planjure.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(defn editor-component [app-state owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [configuration-chan]}]
      (dom/div
        nil
        (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                         :tool-name :brush
                                                         :tool-text "Brush"}})
        (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                         :tool-name :eraser
                                                         :tool-text "Eraser"}})))))

(defn tool-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IRenderState
    (render-state [_ {:keys [configuration-chan tool-name tool-text]}]
      (dom/span
        #js {:className "item-selector"
             :onClick #(do (println tool-name)
                           (put! configuration-chan {:kind :tool-selector :value tool-name}))}
        tool-text))))
