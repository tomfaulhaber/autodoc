(ns autodoc.gen-docs
  (:use [clojure.contrib.pprint :only (pprint)]
        [autodoc.load-files :only (load-namespaces)]
        [autodoc.build-html :only (make-all-pages)]
        [autodoc.params :only (params params-from-dir)]
        [autodoc.branches :only (load-branch-data)]))

;; Old
;; (defn gen-docs 
;;   ([param-dir]
;;      (params-from-dir param-dir)
;;      (load-namespaces)
;;      (make-all-pages)))

(defn gen-docs 
  ([param-dir]
     (params-from-dir param-dir)
     (let [branch-spec (params :branches)]
       (pprint (load-branch-data branch-spec)))))
