(ns clj-play.path-planning)

(def world
  [[0 0 0 0 0]
   [0 0 0 0 0]
   [1 1 1 0 0]
   [0 0 1 0 1]
   [0 1 1 0 1]
   [0 0 0 0 1]])

(def setup
  {:start [5 0]
   :finish [0 1]})

(def path [[5 0] [5 1] [5 2] [5 3] [4 3] [3 3] [2 3] [1 3] [1 2] [1 1] [0 1]])

(defn create-empty-ascii
  [world]
  (let [height (count world)
        width (count (first world))
        ascii (vec (repeat height (vec (repeat width " "))))]
    ascii))

(defn build-basic-ascii
  [world]
  (vec (for [row world]
    (vec (for [cell row]
      (if (> cell 0)
        "#"
        " "))))))


(defn build-ascii-path
  [world path]
  (loop [ascii (build-basic-ascii world)
         path path]
    (if-not (empty? path)
      (let [[r c] (first path)
            row (ascii r)]
        (recur (assoc ascii r (assoc row c "@"))
               (rest path)))
      ascii)))

(defn draw-path
  [world path]
  (let [ascii (build-ascii-path world path)]
    (doseq [row ascii]
      (println row))))

(defn plan
  "Plans a path in the world using specified algorithm."
  [algorithm world setup])

; dfs algorithm
; - each node, we note whether or not we've gone through this, storing the
;   lowest cost.
; - push current node into stack
; - while true:
;   - pop node n from stack.
;   - if empty, done.
;   - if found finish, done.
;
;   - for each neighbor neigh:
;     - calculate cost for n.g + cost(neigh) and save it neigh.g.
;     - point previous pointer to n
;     - mark as visited
;     - push into stack.
; - backtrack from start to finish.
; - draw path.
;

(defn dfs
  "Depth-first search."
  [world {:keys [start finish] :as setup}]
  (let [stack []
        g-cost {}
        previous {}]
    ))

;; arrows could be cool for planning http://www.alanwood.net/unicode/arrows.html

