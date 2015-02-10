(ns hhco.core
  "Figure out expected number of turns to win terrible kids game 'HiHo!
  Cherry-O Mickey Mouse Clubhouse Edition'

  Don't buy: http://www.amazon.com/Cherry-O-Disney-Mickey-Clubhouse-Edition/dp/B00IFWSO8K

  Why? Because the rules supplied are inscrutable, so the only rational path
  forward is to write a simulation in an obscure programming language to to
  test alternatives.

  Said differently, I did this to answer the question 'can this game really
  take this long???'

  The spinner has seven equally likely spots, each dictating how many apples to
  move from the current player's tree to the picnic. Probably. There are many
  interpretations of the spinner, but the least ridiculous one seems to be to
  count the number of fruits on the spot, and ignore the numbers and colours.

  One of the spots, 'Pluto',  causes all players to remove 3 apples from the
  picnic and put them back on their respective trees.  Potentially.  Or maybe
  it's one apple each for a total of 3.  Only God and Hasbro know for sure.

  Simple EV (in apples moving from tree->picnic) of a spin is:
  (1+1+2+2+3+3-3)/7 == 9/7 == 1.285714286

  For a 1 player game you might expect 10/(9/7) == 7.778 spins. Problem is that
  negative valued spins before you have sufficient apples at the picnic don't
  hurt you as badly, so 'experimentally' the expected number of spins to win a
  1 player game seems to be around 7.59:

  (hhco.core/-main 100000 1) ;7.58982

  With three players, you've got more people rolling negatives and prolonging
  the game.  Expect the winner to take ~9.5 spins in this case.

  (hhco.core/-main 100000 3) ;9.49713

  This is the number of spins for *the winner*, so for three players we're
  looking at ~28.5 spins, which is an eternity for kids. I suspect I have
  the Pluto rule wrong.

  I fear the real math for this might be easier than I think, but I am terrible
  at probability and wanted to write a program, which is guaranteed to be bug
  free.
  ")

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)

(def TREE_CAPACITY 10)

(defn spin
  "Returns the number of apples to move from tree -> picnic.  Can be negative,
  meaning the apples move from picnic to tree.
  "
  []
  (case (int (rand-int 7))
    0 1
    1 1
    2 2
    3 2
    4 3
    5 3
    6 -3))

(defn do-turn
  "Spin and update gamestate for player p.  Return new gamestate."
  [p g]
  (let [spin (spin)
        ;_ (prn p spin)
        move-apples #(-> (- % spin)
                         (min TREE_CAPACITY)
                         (max 0))
        g (cond-> g
            true (update-in [p :spins] inc)
            (pos? spin) (update-in [p :tree] move-apples))
        g (if (neg? spin)
            ;; put spin apples back on everyone's tree, if possible
            (reduce-kv (fn [acc player pstate]
                         (assoc acc
                                player
                                (update-in pstate [:tree] move-apples)))
                       {}
                       g)
            g)]
    g))

(defn turns-to-win
  [num-players]
  (let [g (zipmap (range num-players) (repeat {:tree TREE_CAPACITY :spins 0}))]
    (loop [p 0 turn 1 g g]
      (let [g' (do-turn p g)]
        (if (zero? (get-in g' [p :tree]))
          [p (get-in g' [p :spins])]
          (recur (long (mod (inc p) num-players))
                 (inc turn)
                 g'))))))

(defn -main [& args]
  (let [num-games (or (first args) 10000)
        num-players (or (second args) 3)]
    (loop [i 0
           sum 0]
      (let [[_ num-turns] (turns-to-win num-players)]
        (if (< i num-games)
          (recur (inc i) (long (+ sum num-turns)))
          (/ sum (double i)))))))

