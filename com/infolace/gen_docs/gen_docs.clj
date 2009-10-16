(ns com.infolace.gen-docs.gen-docs
  (:use [com.infolace.gen-docs.load-files :only (load-contrib)]
        [com.infolace.gen-docs.build-html :only (make-all-pages)]))

(defn gen-docs 
  ([param-file]
     (load-contrib)
     (make-all-pages)))
