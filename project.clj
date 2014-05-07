(defproject clj-play "0.1.0-SNAPSHOT"
  :description "Playing in Clojure. Random data structures and algorithms."
  :url "http://github.com/elben/clj-play"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :main clj-play.core
  :profiles {:uberjar {:aot :all}})
