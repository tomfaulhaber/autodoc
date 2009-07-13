(ns com.infolace.gen-docs.gen-docs
  (:use [clojure.contrib.pprint.utilities :only (prlabel)]
        [clojure.contrib.duck-streams :only (with-out-writer)]
        [com.infolace.gen-docs.load-files :only (load-contrib)]
        [com.infolace.gen-docs.build-html :only (make-all-pages)]))

(defn gen-docs []
  (load-contrib)
  (make-all-pages))
