(ns com.infolace.gen-docs.collect-info)

;; Build a single structure representing all the info we care about concerning
;; namespaces and their members 
;;
;; Assumes that all the relevant namespaces have already been loaded

;; namespace: { :full-name :short-name :doc :author :members :subspaces}
;; vars: {:name :doc :arglists :var-type :file :line}

(defn vars-for-ns [ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (and (or (:wiki-doc ^v) (:doc ^v)) (not (:skip-wiki ^v)) (not (:private ^v)))] v))

(defn vars-info [ns]
  (for [v (vars-for-ns ns)] 
    (select-keys ^v [:name :arglists])))

(defn contrib-namespaces []
  (filter #(not (:skip-wiki ^%))
          (map #(find-ns (symbol %)) 
               (filter #(.startsWith % "clojure.contrib.")
                       (sort (map #(name (ns-name %)) (all-ns)))))))

(defn trim-ns-name [s]
  (if (.startsWith s "clojure.contrib.")
    (subs s 16)
    s))

(defn base-namespace
  "A nasty function that finds the shortest prefix namespace of this one"
  [ns]
  (first 
   (drop-while 
    (comp not identity) 
    (map #(let [ns-part (find-ns (symbol %))]
            (if (not (:skip-wiki ^ns-part))
              ns-part))
         (let [parts (seq (.split (name (ns-name ns)) "\\."))]
           (map #(apply str (interpose "." (take (inc %) parts)))
                (range 2 (count parts))))))))

(defn base-contrib-namespaces []
  (filter #(= % (base-namespace %)) (contrib-namespaces)))

(defn sub-namespaces 
  "Find the list of namespaces that are sub-namespaces of this one. That is they 
have the same prefix followed by a . and then more components"
  [ns]
  (let [pat (re-pattern (str (.replaceAll (name (ns-name ns)) "\\." "\\.") "\\..*"))]
    (sort-by
     #(name (ns-name %))
     (filter #(and (not (:skip-wiki ^%)) (re-matches pat (name (ns-name %)))) (all-ns)))))

(defn ns-short-name [ns]
  (trim-ns-name (name (ns-name ns))))

(defn build-ns-entry [ns]
  {:full-name (name (ns-name ns)) :short-name (ns-short-name ns)
   :doc (:doc ^ns) :author (:author ^ns) :ns ns})

(defn build-ns-list [nss]
  (sort-by :short-name (map add-vars (map build-ns-entry nss))))

(defn add-subspaces [info]
     (assoc info :subspaces (build-ns-list (sub-namespaces (:ns info)))))

(defn contrib-info [] 
  (map add-subspaces
       (build-ns-list (base-contrib-namespaces))))
