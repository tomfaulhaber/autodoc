(ns com.infolace.gen-docs.collect-info)

;; Build a single structure representing all the info we care about concerning
;; namespaces and their members 
;;
;; Assumes that all the relevant namespaces have already been loaded

;; namespace: { :full-name :short-name :doc :author :members :subspaces :see-also}
;; vars: {:name :doc :arglists :var-type :file :line}

(defn remove-leading-whitespace 
  "Find out what the minimum leading whitespace is for a doc block and remove it.
We do this because lots of people indent their doc blocks to the indentation of the 
string, which looks nasty when you display it."
  [s]
  (when s
    (let [lines (.split s "\\n") 
          prefix-lens (map #(count (re-find #"^ *" %)) 
                           (filter #(not (= 0 (count %))) 
                                   (next lines)))
          min-prefix (when (seq prefix-lens) (apply min prefix-lens))
          regex (when min-prefix (apply str "^" (repeat min-prefix " ")))]
      (if regex
        (apply str (interpose "\n" (map #(.replaceAll % regex "") lines)))
        s))))

(defn var-type 
  "Determing the type (var, function, macro) of a var from the metadata and
return it as a string."
  [v]
  (cond (:macro ^v) "macro"
        (= (:tag ^v) clojure.lang.MultiFn) "multimethod"
        (:arglists ^v) "function"
        :else "var"))

(defn vars-for-ns [ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (and (or (:wiki-doc ^v) (:doc ^v)) (not (:skip-wiki ^v)) (not (:private ^v)))] v))

(defn vars-info [ns]
  (for [v (vars-for-ns ns)] 
    (merge (select-keys ^v [:arglists :file :line])
           {:name (name (:name ^v))
            :doc (remove-leading-whitespace (:doc ^v)),
            :var-type (var-type v)})))

(defn add-vars [ns-info]
  (merge ns-info {:members (vars-info (:ns ns-info))}))

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
   :doc (remove-leading-whitespace (:doc ^ns)) :author (:author ^ns)
   :see-also (:see-also ^ns) :ns ns})

(defn build-ns-list [nss]
  (sort-by :short-name (map add-vars (map build-ns-entry nss))))

(defn add-subspaces [info]
     (assoc info :subspaces 
            (filter #(or (:doc %) (seq (:members %)))
                    (build-ns-list (sub-namespaces (:ns info))))))

(defn add-base-ns-info [ns]
  (assoc ns
    :base-ns ns
    :subspaces (map #(assoc % :base-ns ns) (:subspaces ns))))

(defn contrib-info [] 
  (map add-base-ns-info (map add-subspaces
                             (build-ns-list (base-contrib-namespaces)))))
