(ns com.infolace.gen-docs.utils)

(defmacro with-ns 
  [name & body]
  `(let [old-ns# *ns*]
     (try 
      (in-ns '~name)
      ~@body
      (finally
       (in-ns (.name old-ns#))))))

(defn load-params [param-file]
  (with-ns com.infolace.gen-docs.params
      (load (str "../../../" param-file))))

