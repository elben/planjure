(ns planjure.demo
  (:require [om.core :as om :include-macros true]
            [planjure.appstate :as appstate]
            [planjure.components.toolbar :as toolbar]
            [planjure.components.canvas :as canvas]))

(enable-console-print!)


(om/root canvas/world-canvas-component appstate/app-state
         {:target (. js/document (getElementById "world"))})

(om/root toolbar/toolbar-component appstate/app-state
         {:target (. js/document (getElementById "toolbar"))})

