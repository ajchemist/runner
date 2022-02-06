(ns runner.integrant.config-source.file-or-resource-test
  (:require
   [clojure.test :as test :refer [deftest is are testing]]
   [runner.integrant :as runner.ig]
   [runner.integrant.config-source.file-or-resource :as f-o-r]
   ))


(deftest main
  (is (nil? (f-o-r/config-source "config/test-n.edn")))
  (is (nil? (runner.ig/slurp-edn-map (f-o-r/config-source "config/test-n.edn") nil)))
  (is (map? (runner.ig/slurp-edn-map (f-o-r/config-source "config/test.edn") nil)))

  (is (==
        (:x
         (runner.ig/merge-system-maps
           {}
           nil
           [(f-o-r/config-source "config/test.edn")
            (f-o-r/config-source "config/test-2.edn")]))
        1))


  (is
    (==
      (:x
       (runner.ig/merge-system-maps-2
         {}
         nil
         [(f-o-r/config-source "config/test.edn")
          (f-o-r/config-source "config/test-2.edn")]))
      2))
  )
