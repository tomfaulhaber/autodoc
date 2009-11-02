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
      (load param-file)))


(defn get-param* [param-name] 
  (if-let [param-var (find-var (symbol 'com.infolace.gen-docs.params (name param-name)))]
    (var-get param-var)))

(defmacro param [param-name]
  `(get-param* '~param-name))

