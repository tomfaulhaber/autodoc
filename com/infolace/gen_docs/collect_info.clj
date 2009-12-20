(ns com.infolace.gen-docs.collect-info
  (:use [clojure.contrib.pprint.utilities :only [prlabel]]
        [com.infolace.gen-docs.params :only (*namespaces-to-document* *trim-prefix*)]))

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
  (cond (:macro (meta v)) "macro"
        (= (:tag (meta v)) clojure.lang.MultiFn) "multimethod"
        (:arglists (meta v)) "function"
        :else "var"))

(defn vars-for-ns [ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (and (or (:wiki-doc (meta v)) (:doc (meta v))) (not (:skip-wiki (meta v))) (not (:private (meta v))))] v))

(defn vars-info [ns]
  (for [v (vars-for-ns ns)] 
    (merge (select-keys (meta v) [:arglists :file :line])
           {:name (name (:name (meta v)))
            :doc (remove-leading-whitespace (:doc (meta v))),
            :var-type (var-type v)})))

(defn add-vars [ns-info]
  (merge ns-info {:members (vars-info (:ns ns-info))}))

(defn relevant-namespaces []
  (filter #(not (:skip-wiki (meta %)))
          (map #(find-ns (symbol %)) 
               (filter #(some (fn [n] (or (= % n) (.startsWith % (str n "."))))
                              *namespaces-to-document*)
                       (sort (map #(name (ns-name %)) (all-ns)))))))

(defn trim-ns-name [s]
  (if (and *trim-prefix* (.startsWith s *trim-prefix*))
    (subs s (count *trim-prefix*))
    s))

(defn base-namespace
  "A nasty function that finds the shortest prefix namespace of this one"
  [ns]
  (first 
   (drop-while 
    (comp not identity) 
    (map #(let [ns-part (find-ns (symbol %))]
            (if (not (:skip-wiki (meta ns-part)))
              ns-part))
         (let [parts (seq (.split (name (ns-name ns)) "\\."))]
           (map #(apply str (interpose "." (take (inc %) parts)))
                (range 0 (count parts)))))))) ;; TODO first arg to range was 0 for contrib

(defn base-relevant-namespaces []
  (filter #(= % (base-namespace %)) (relevant-namespaces)))

(defn sub-namespaces 
  "Find the list of namespaces that are sub-namespaces of this one. That is they 
have the same prefix followed by a . and then more components"
  [ns]
  (let [pat (re-pattern (str (.replaceAll (name (ns-name ns)) "\\." "\\.") "\\..*"))]
    (sort-by
     #(name (ns-name %))
     (filter #(and (not (:skip-wiki (meta %))) (re-matches pat (name (ns-name %)))) (all-ns)))))

(defn ns-short-name [ns]
  (trim-ns-name (name (ns-name ns))))

(defn build-ns-entry [ns]
  {:full-name (name (ns-name ns)) :short-name (ns-short-name ns)
   :doc (remove-leading-whitespace (:doc (meta ns))) :author (:author (meta ns))
   :see-also (:see-also (meta ns)) :ns ns})

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
                             (build-ns-list (base-relevant-namespaces)))))
