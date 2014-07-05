(ns planjure.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(defn item-selector-component [app-state owner]
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

