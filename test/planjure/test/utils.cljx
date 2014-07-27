(ns planjure.test.utils
  (:require #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]
            [planjure.utils :refer :all])
  #+cljs (:require-macros
           [cemerick.cljs.test
            :refer (is deftest with-test run-tests testing test-var)]))

(deftest test-update-row
  (testing "base cases"
    (is (= [] (update-row [] [] 10 10)))
    (is (= [4] (update-row [1] [3] 0 1))))
  (testing "with mask"
    (is (= [1 1 4 1 1 1 1] (update-row [1 1 1 1 1 1 1] [3] 2 1)))
    (is (= [3 4 1 1 1 1 1] (update-row [1 1 1 1 1 1 1] [1 2 3] 0 1)))
    (is (= [1 1 2 3 4 1 1] (update-row [1 1 1 1 1 1 1] [1 2 3] 3 1)))
    (is (= [1 1 1 1 1 2 3] (update-row [1 1 1 1 1 1 1] [1 2 3] 6 1))))
  (testing "multiplier"
    (is (= [1 1 -1 -3 -5 1 1] (update-row [1 1 1 1 1 1 1] [1 2 3] 3 -2 -100 100)))))

(deftest test-update-world
  (testing "base cases"
    (is (= [[]] (update-world [[]] [[]] 10 10 1)))
    (is (= [[4]] (update-world [[1]] [[3]] 0 0 1))))
  (testing "with matrix"
    (is (= [[3 3 1 1]
            [3 3 1 1]
            [1 1 1 1]
            [1 1 1 1]]
           (update-world [[1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]]
                         [[2 2 2]
                          [2 2 2]
                          [2 2 2]]
                         0 0 1)))
    (is (= [[1 1 1 1]
            [1 3 3 3]
            [1 3 3 3]
            [1 3 3 3]]
           (update-world [[1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]]
                         [[2 2 2]
                          [2 2 2]
                          [2 2 2]]
                         2 2 1)))

    (is (= [[1 1 1 1]
            [1 1 1 1]
            [1 1 3 3]
            [1 1 3 3]]
           (update-world [[1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]]
                         [[2 2 2]
                          [2 2 2]
                          [2 2 2]]
                         3 3 1))))
  (testing "multiplier"
    (is (= [[1 1 1 1]
            [1 7 7 7]
            [1 7 7 7]
            [1 7 7 7]]
           (update-world [[1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]
                          [1 1 1 1]]
                         [[2 2 2]
                          [2 2 2]
                          [2 2 2]]
                         2 2 3)))))

