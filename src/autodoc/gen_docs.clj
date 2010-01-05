(ns autodoc.gen-docs
  (:use [autodoc.load-files :only (load-namespaces)]
        [autodoc.build-html :only (make-all-pages)]
        [autodoc.params :only (params params-from-dir)]))

(defn gen-docs 
  ([param-dir]
     (params-from-dir param-dir)
     (load-namespaces)
     (make-all-pages)))
