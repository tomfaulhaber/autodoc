(ns autodoc.gen-docs
  (:use [autodoc.load-files :only (load-namespaces)]
        [autodoc.build-html :only (make-all-pages)]
        [autodoc.utils :only (load-params)]
        [autodoc.params :only (*do-load*)]))

(defn gen-docs 
  ([param-dir]
     (alter-var-root
      (find-var 'autodoc.params/*param-dir*)
      (constantly param-dir))
     (load-params (str param-dir "/params.clj"))
     (when *do-load*
       (load-namespaces))
     (make-all-pages)))
