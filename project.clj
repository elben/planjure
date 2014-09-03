(defproject planjure "0.1.0-SNAPSHOT"
  :description "Path-planning for ClojureScript."
  :url "http://github.com/elben/planjure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.7.0"]
                 [org.clojure/data.priority-map "0.0.5"]
                 [tailrecursion/cljs-priority-map "1.1.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.1"]
            [com.keminglabs/cljx "0.4.0"]]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated-src"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/generated-src"
                   :rules :cljs}

                  {:source-paths ["test"]
                   :output-path "target/generated-test"
                   :rules :clj}
                  ]}

  :hooks [cljx.hooks]

  :source-paths ["src" "target/generated-src"]
  :test-paths ["target/generated-test"]

  :cljsbuild {
   :test-commands {"unit-tests" ["phantomjs" :runner "target/cljs/testable.cljs"]}
   :builds [
            ;; cljs build for distribution
            {:id "planjure"
             :source-paths ["src/cljs" "target/generated-src"]
             :compiler {
               :output-to "planjure.js"
               :output-dir "out"
               :optimizations :none
               :source-map true}}

            ;; cljs tests build
            {:id "planjure-test"
             :source-paths ["src/cljs" "target/generated-src" "test"]
             :compiler {
               :output-to "target/cljs/testable.cljs"
               ; :output-dir "out-test"
               :pretty-print true
               :optimizations :whitespace}}
            ]})
