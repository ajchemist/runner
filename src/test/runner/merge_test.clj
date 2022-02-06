(ns runner.merge-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [runner.merge :as merge]
   ))


(deftest main
  (is (=
        (:a (merge/merge-maps
              {}
              {:a [1 2]}
              {:a [3]}))
        [3])
      "Value of :a replaced because its value is not a map.")
  (is (=
        (:a (merge/merge-maps
              {}
              {:a [1 2]}
              {:a ^:append [2 3]}))
        [1 2 2 3])
      "append")
  (is (=
        (:a (merge/merge-maps
              {}
              {:a [1 2]}
              {:a ^{:append true
                    :meta-x true} [2 3]}))
        [2 3])
      "when multi meta keys")
  (is (=
        (:a (merge/merge-maps
              {}
              {:a [1 2]}
              {:a ^{:merge-rule :append
                    :meta-x     true} [2 3]}))
        [1 2 2 3])
      "append")
  (is (=
        (:a (merge/merge-maps
              {}
              {:a [1 2]}
              {:a ^:append-unique [2 3]}))
        [1 2 3]))
  (is (=
        (:a (merge/merge-maps
              {:a :append-unique}
              {:a [1 2]}
              {:a [2 3]}))
        [1 2 3]))
  (let [{:keys [key val]}
        (try
          (merge/merge-maps
            {:a :merge}
            {:a {1 2}}
            {:a [2 3 4 5]})
          (catch Throwable e (ex-data e)))]
    (is (and (= key :a) (= val [2 3 4 5])) "When last value is not a map, replaced by the last value"))
  (is (=
        (get-in
          (merge/merge-maps
            {}
            {:b {:x :y}}
            {:b {:x 1
                 :y :z}})
          [:b :x])
        1))
  )
