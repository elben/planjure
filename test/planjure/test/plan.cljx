(ns planjure.test.plan
  (:require #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]
            [planjure.plan :refer :all])
  #+cljs (:require-macros
           [cemerick.cljs.test
            :refer (is deftest with-test run-tests testing test-var)]))

(deftest test-plan
  (is (= true true)))
