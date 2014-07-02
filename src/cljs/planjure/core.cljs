(ns planjure.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [planjure.plan :as plan]
            [planjure.appstate :as appstate]
            [planjure.components.toolbar :as toolbar]
            [planjure.components.canvas :as canvas]))

(enable-console-print!)


(om/root canvas/world-canvas-component appstate/app-state
         {:target (. js/document (getElementById "world"))})

(om/root toolbar/toolbar-component appstate/app-state
         {:target (. js/document (getElementById "toolbar"))})

