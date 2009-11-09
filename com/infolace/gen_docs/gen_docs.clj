(ns com.infolace.gen-docs.gen-docs
  (:use [com.infolace.gen-docs.load-files :only (load-namespaces)]
        [com.infolace.gen-docs.build-html :only (make-all-pages)]
        [com.infolace.gen-docs.utils :only (load-params)]
        [com.infolace.gen-docs.params :only (*do-load*)]))

(defn gen-docs 
  ([param-dir]
     (alter-var-root
      (find-var 'com.infolace.gen-docs.params/*param-dir*)
      (constantly param-dir))
     (load-params (str param-dir "/params"))
     (when *do-load*
       (load-namespaces))
     (make-all-pages)))
