(ns autodoc.utils
  (:use 
   [clojure.contrib.duck-streams :only (with-in-reader)]))

(defmacro with-ns 
  [name & body]
  `(let [old-ns# *ns*]
     (try 
      (in-ns '~name)
      ~@body
      (finally
       (in-ns (.name old-ns#))))))

(defn load-params [param-file]
  (with-ns autodoc.params
      (load-file param-file)))

