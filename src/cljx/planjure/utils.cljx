(ns planjure.utils)

(defn update-row
  "Update row with the mask at the given offset. Consider the mid-point of the
  mask as idx 0 of offset."
  ([row mask offset]
   (update-row row mask offset 1 1 8))
  ([row mask offset multiplier]
   (update-row row mask offset multiplier 1 8))
  ([row mask offset multiplier cost-min cost-max]
   (let [mid-mask-idx (int (/ (count mask) 2))]
     ;; TODO can probably optimize by using assoc instead of creating new vecs.
     (vec (map-indexed
            (fn [idx cost]
              (let [mask-idx (- (+ idx mid-mask-idx) offset)]
                (if (and (>= mask-idx 0)
                         (< mask-idx (count mask)))
                  (max cost-min (min (+ cost (* (mask mask-idx) multiplier)) cost-max))
                  cost)))
            row)))))

(defn update-world
  "Update world with the 2-d matrix mask at x, y. Consider the mid-point of the
  matrix as x, y. Multiply mask by multiplier."
  ([world matrix x y multiplier]
   (update-world world matrix x y multiplier 1 8))
  ([world matrix x y multiplier cost-min cost-max]
   (let [mid-matrix-idx (int (/ (count matrix) 2))]
     ;; TODO can probably optimize by using assoc instead of creating new vecs.
     (vec (map-indexed
            (fn [idx row]
              (let [matrix-idx (- (+ idx mid-matrix-idx) y)]
                (if (and (>= matrix-idx 0)
                         (< matrix-idx (count matrix)))
                  (update-row row (matrix matrix-idx) x multiplier cost-min cost-max)
                  row)))
            world)))))


(defn time-f [f]
  (fn [& args]
    (let [start #+cljs (js/Date.) #+clj (System/currentTimeMillis)
          ret (apply f args)]
      {:time (- #+cljs (js/Date.) #+clj (System/currentTimeMillis) start)
       :return ret})))
