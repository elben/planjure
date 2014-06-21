(ns planjure.plan
  (:require [tailrecursion.priority-map :refer [priority-map]]))

(def world
  [[1 1 1 1 1]
   [1 1 1 1 1]
   [9 9 9 1 1]
   [1 1 9 1 9]
   [1 9 9 1 9]
   [1 1 1 1 9]])

(def world2
  [[1 1 1 1 1]
   [3 3 1 1 1]
   [1 1 1 1 1]
   [1 1 1 1 1]
   [1 1 1 1 1]
   [1 1 1 1 1]])

(def world3
  [[0 0 0 0 0]
   [3 3 1 1 1]
   [0 0 0 0 0]
   [0 0 0 0 0]
   [0 0 0 0 0]
   [0 0 0 0 0]])

(def setup
  {:start [5 0]
   :finish [0 1]})

;; Assumes a path of start to finish
(def path [[5 0] [5 1] [5 2] [5 3] [4 3] [3 3] [2 3] [1 3] [1 2] [1 1] [0 1]])

(defn random-tile
  "Randomly generate a tile with chance of it being cost of 1."
  [chance]
  (if (< (rand) chance)
    1
    (+ 1 (rand-int 5))))

(defn random-world [rows cols]
  (vec (repeatedly rows
    (fn [] (vec (repeatedly cols #(random-tile 0.7)))))))

(defn create-empty-ascii
  [world]
  (let [height (count world)
        width (count (first world))
        ascii (vec (repeat height (vec (repeat width " "))))]
    ascii))

(defn build-basic-ascii
  [world]
  (vec (for [row world]
    (vec (for [node row]
      (if (> node 0)
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

(defn draw-world
  [world]
  (let [cols (count (first world))]
    (print "   ")
    (doseq [i (range cols)]
      (print i ""))
    (println)
    (doseq [i (range (count world))]
      (println i (world i)))))

(defn draw-path
  [world path]
  (let [ascii (build-ascii-path world path)]
    (doseq [row ascii]
      (println row))))

(defn plan
  "Plans a path in the world using specified algorithm."
  [algorithm world setup])

(defn cost
  "Cost of traversing from a neighboring node to specified node in world.  In
  theory, the edges hold the cost. But in our current world, the nodes hold the
  cost. Means that the cost of moving from any of its neighboring node to
  specified node is the value of the node cell."
  [world node]
  (let [[r c] node]
    ((world r) c)))

(defn find-path
  "Returns path from start to finish. Previous is a backtrack mapping of nodes,
  and setup is a map containing :start and :finish nodes. Returns partial path
  if no is available (e.g. no path or recursive path)."
  [previous {:keys [start finish] :as setup}]
  (if-not (contains? previous finish)
    []
    (loop [path []
           seen #{}
           node finish]
      (cond
        ;; This check needs to go first, before the contains? check, since
        ;; start is not in previous mapping.
        ;;
        ;; Done, found path from finish to start. Reverse so the path presented
        ;; is start to finish.
        (= node start) (reverse (conj path start))

        ;; Cannot complete path. Return best path, ending in finish.
        (not (contains? previous node)) (reverse path)

        ;; Seen before. Recursive. Cannot complete path.
        (contains? seen node) (reverse (conj path node))

        ;; Can backtrack, so recur.
        :else (recur (conj path node) (conj seen node) (previous node))))))


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
;     - calculate cost for node.g + cost(node, neigh)
;     - if this new cost is lower than the old cost (default cost is infinite):
;       - save g-costs(neigh)
  ;     - set previous(neigh) = node
;     - mark as visited
;     - push neigh into stack.
; - backtrack from start to finish.
; - draw path.
;
;
; finishing cases (nothing left in stack):
; - if there's nothing left, there's no work left to be done.
; - we can now examine finish node and see the cost (g-costs), and traverse back
;   to start (via previous)
; - if finish has un-initialized g-costs, or if we can't traverse back to start,
;   then the path of start to finish is infinitely high

(defn neighbors
  "Return the neighbors of node. Only four directions. Don't include start node
  as neighbor, because we assume it's never better to back through the start
  node (that is, we assume consistency in costs)."
  [world node {:keys [start]}]
  (let [rows (count world)
        cols (count (first world))
        find-neighbors (fn [neighs [row-mod col-mod]]
                         (let [row (+ (node 0) row-mod)
                               col (+ (node 1) col-mod)]
                           (if (and
                                 ;; Don't include start node
                                 (not (= [row col] start))

                                 ;; Check bounds.
                                 (< row rows)
                                 (>= row 0)
                                 (< col cols)
                                 (>= col 0))
                             (conj neighs [row col])
                             neighs)))]
    (reduce find-neighbors [] [[1 0] [0 1] [-1 0] [0 -1]])))

(defn lookup-g-cost
  [g-costs node]
  (if (contains? g-costs node)
    (g-costs node)
    0))

(defn nodes-with-improved-costs
  "Find nodes that would improve its cost if it were to arrive from node instead
  of the cost specified in g-cost. Exclude nodes that would increase its cost.
  Returns map of nodes to cost."
  [world g-costs base-node nodes]
  (let [rows (count world)
        cols (count (first world))
        collect-improved-nodes (fn
                                 [improved-nodes node]
                                 (let [new-cost (+ (lookup-g-cost g-costs base-node) (cost world node))]
                                   (if (or
                                         (not (contains? g-costs node))
                                         (< new-cost (lookup-g-cost g-costs node)))

                                     ;; Use this node if it wasn't in the g-costs
                                     ;; before (never been traversed to), or if it's
                                     ;; cheaper coming from this base-node than before.
                                     (assoc improved-nodes node new-cost)

                                     ;; If more expensive, don't use this node
                                     improved-nodes)))]
    (reduce collect-improved-nodes {} nodes)))

(defn dijkstra
  "Dijkstra's classic graph algorithm.
  
  Returns optimal path from start to finish."
  [world {:keys [start finish] :as setup}]
  (loop [pq (priority-map start 0)
         g-costs {}
         previous {}]
    (cond
      (empty? pq)
        ;; Never found finish. Plan as best as we can.
        (find-path previous setup)
      (= (first (first pq)) finish)
        ;; We're done.
        ;;
        ;; If we popped finish, that means that finish was pushed as a neighbor
        ;; and thus we have 'previous' set up already.
        (find-path previous setup)
      :else
        (let [node (first (first pq)) ;; Get highest priority node (throw away priority).
              old-pq pq
              pq (pop pq)
              neighs (neighbors world node setup)
              improved-neighbor-costs (nodes-with-improved-costs world g-costs node neighs)
              improved-neighbors (keys improved-neighbor-costs)
              updated-g-costs (merge g-costs improved-neighbor-costs)
              updated-previous (merge previous
                                      ;; Create map of neighbors to node
                                      ;; %1 is map, %2 neighbor.
                                      (reduce #(assoc %1 %2 node) {} improved-neighbors))]
          ;; Push new neighbors into priority queue
          (recur (into pq (vec improved-neighbor-costs)) updated-g-costs updated-previous)))))

(defn dfs
  "Depth-first search.

  Instead of terminating right away after finish is found, this DFS
  implementation exhausts the search space to find the global maxima.

  Returns optimal path from start to finish."
  [world {:keys [start finish] :as setup}]
  (loop [stack [start] ;; init stack with start node
         g-costs {} ;; map of node to cost
         previous {}] ;; map of node to node
    (cond
      (empty? stack)
        ;; We're done! get out of loop.
        (find-path previous setup)

      (= (last stack) finish)
        ;; If we popped finish, don't push its neighbors into stack, because
        ;; then we'd never finish. Instead, just pop off stack and continue
        ;; finding other paths. Goal here isn't to just find any path, but to
        ;; find the best path using DFS.
        (recur (pop stack) g-costs previous)

      :else
        ;; Else, we need to do work.
        ; - Find neighbors.
        ; - Figure out g costs from node to each of those neighbors
        ; - better-neighs = neighbors that are better through node
        ; - modify g-costs of better-neighs
        ; - modify previous of better-neighs
        ; - push better-neighs into stack
        (let [node (last stack)
              stack (pop stack)
              neighs (neighbors world node setup)
              improved-neighbor-costs (nodes-with-improved-costs world g-costs node neighs)
              improved-neighbors (keys improved-neighbor-costs)
              updated-g-costs (merge g-costs improved-neighbor-costs)
              updated-previous (merge previous
                                      ;; Create map of neighbors to node
                                      ;; %1 is map, %2 neighbor.
                                      (reduce #(assoc %1 %2 node) {} improved-neighbors))]
          ;; Push new neighbors into stack.
          (recur (into stack improved-neighbors) updated-g-costs updated-previous)))))

;; arrows could be cool for planning http://www.alanwood.net/unicode/arrows.html

