(defproject clj-play "0.1.0-SNAPSHOT"
  :description "Path-planning for ClojureScript."
  :url "http://github.com/elben/clj-play"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.6.2"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src/cljs"]

  :cljsbuild {
   :builds [{:id "clj-play"
             :source-paths ["src"]
             :compiler {
               :output-to "clj_play.js"
               :output-dir "out"
               :optimizations :none
               :source-map true}}]})
