(ns planjure.utils)

(defn create-replacement-row
  [row mask offset multiplier]
  (let [mid-mask-idx (int (/ (count mask) 2))]
    (vec (map-indexed
           (fn [idx cost]
             (let [mask-idx (- (+ idx mid-mask-idx) offset)]
               (if (and (>= mask-idx 0)
                        (< mask-idx (count mask)))
                 ;; TODO generalize min/max
                 (max 1 (min (+ cost (* (mask mask-idx) multiplier)) 8))
                 cost)))
           row))))

(defn replace-world
  [world matrix x y multiplier]
  (let [mid-matrix-idx (int (/ (count matrix) 2))]
    (vec (map-indexed
           (fn [idx row]
             (let [matrix-idx (- (+ idx mid-matrix-idx) y)]
               (if (and (>= matrix-idx 0)
                        (< matrix-idx (count matrix)))
                 (create-replacement-row row (matrix matrix-idx) x multiplier)
                 row)))
           world))))


