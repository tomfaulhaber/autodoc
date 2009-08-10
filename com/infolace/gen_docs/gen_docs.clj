(ns com.infolace.gen-docs.gen-docs
  (:use [clojure.contrib.pprint.utilities :only (prlabel)]
        [clojure.contrib.pprint :only (cl-format)]
        [com.infolace.gen-docs.params :only [*web-src-dir*]]
        [com.infolace.gen-docs.load-files :only (load-contrib)]
        [com.infolace.gen-docs.build-html :only (make-all-pages)]))

(defn gen-docs 
  ([] (gen-docs "master"))
  ([commit-hash] ; TODO: I don't think we need to pass this anymore (change build.xml too)
     (load-contrib)
     (make-all-pages)))
