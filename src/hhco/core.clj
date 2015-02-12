(ns hhco.core
  "See README.md")

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)

(def TREE_CAPACITY 10)

(defn make-spinner
  "Makes a 0-arg spinner function with given penalty.  Returned function
  returns the number of fruits to move from tree -> picnic.  Can be negative,
  meaning the fruits move from picnic to tree."
  [penalty]
  (fn []
    (case (int (rand-int 7))
      0 1
      1 1
      2 2
      3 2
      4 3
      5 3
      6 penalty)))

(defn do-turn
  "Spin and update gamestate for player p.  Return new gamestate."
  [p g spin]
  (let [move-fruit #(-> (- % spin)
                         (min TREE_CAPACITY)
                         (max 0))
        g (cond-> g
            true (update-in [p :spins] inc)
            (pos? spin) (update-in [p :tree] move-fruit))
        g (if (neg? spin)
            ;; put spin fruits back on everyone's tree, if possible
            (reduce-kv (fn [acc player pstate]
                         (assoc acc
                                player
                                (update-in pstate [:tree] move-fruit)))
                       {}
                       g)
            g)]
    g))

(defn turns-to-win
  [num-players spinner]
  (let [g (zipmap (range num-players) (repeat {:tree TREE_CAPACITY :spins 0}))]
    (loop [p 0 turn 1 g g]
      (let [g' (do-turn p g (spinner))]
        (if (zero? (get-in g' [p :tree]))
          [p (get-in g' [p :spins])]
          (recur (long (mod (inc p) num-players))
                 (inc turn)
                 g'))))))

(defn -main [& args]
  (let [num-games (or (first args) 10000)
        num-players (or (second args) 3)
        penalty (nth args 2 -3)
        spinner (make-spinner penalty)]
    (loop [i 0
           sum 0]
      (let [[_ num-turns] (turns-to-win num-players spinner)]
        (if (< i num-games)
          (recur (inc i) (long (+ sum num-turns)))
          (/ sum (double i)))))))

