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
        (dom/div
          #js {:className "button-row"}
          (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                           :tool-kind :brush
                                                           :tool-name :brush
                                                           :tool-text "Brush"}})
          (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                           :tool-kind :brush
                                                           :tool-name :eraser
                                                           :tool-text "Eraser"}}))
        (dom/div
          #js {:className "button-row"}
          (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                           :tool-kind :brush-size
                                                           :tool-name :size1
                                                           :tool-text "1"}})
          (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                           :tool-kind :brush-size
                                                           :tool-name :size2
                                                           :tool-text "2"}})
          (om/build tool-component app-state {:init-state {:configuration-chan configuration-chan
                                                           :tool-kind :brush-size
                                                           :tool-name :size3
                                                           :tool-text "3"}}))))))

(defn tool-component [app-state owner]
  (reify
    om/IInitState
    (init-state [_]
      {})

    om/IRenderState
    (render-state [_ {:keys [configuration-chan tool-kind tool-name tool-text selected]}]
      (let [css-class (if (= tool-name (tool-kind app-state)) "selected" "")]
        (dom/span
          #js {:className (str "item-selector " css-class)
               :onClick #(put! configuration-chan {:kind :tool-selector :tool-kind tool-kind :value tool-name})}
          tool-text)))))
