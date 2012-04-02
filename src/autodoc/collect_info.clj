(ns autodoc.collect-info
  (:use [autodoc.load-files :only (load-namespaces)]
        [autodoc.params :only (params params-from-dir params-from-file)]))

;; Build a single structure representing all the info we care about concerning
;; namespaces and their members 
;;
;; Assumes that all the relevant namespaces have already been loaded

;; namespace: { :full-name :short-name :doc :author :members :subspaces :see-also}
;; vars: {:name :doc :arglists :var-type :file :line :added :deprecated :dynamic :examples}


(def post-1-2? (let [{:keys [major minor]} *clojure-version*]
                 (or (>= major 2) (and (= major 1) (>= minor 2)))))

(def post-1-3? (let [{:keys [major minor]} *clojure-version*]
                 (or (>= major 2) (and (= major 1) (>= minor 3)))))

(defmacro defdynamic [var init]
  `(do
     (def  ~var ~init)
     (when post-1-3? (.setDynamic #'~var))))

(defdynamic saved-out nil)

(defn debug [& args]
  (binding [*out* (or saved-out *out*)]
    (apply println args)))

(if post-1-2?
  (do
    (load "reflect")
    (refer 'autodoc.reflect :only '[reflect]))
  (defn reflect [obj & options]))

(defn ns-to-class-name
  "Convert the namespece name into a class root name"
  [ns]
  (.replace (name (ns-name ns)) "-" "_"))

(defn class-to-ns-name
  "Convert a class to the corresponding namespace name"
  [ns]
  (.replace (name (ns-name ns)) "_" "-"))

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

(defn my-every-pred
  "every-pred didn't exist before clojure-1.3, so we replicate it here"
  [& ps]
  (fn [& args]
    (every? #(every? % args) ps)))

(defn protocol?
  "Return true if the var is a protocol definition. The only way we can tell
this is by looking at the map and seeing if it has the right keys, which
may not be foolproof."
  [v]
  (and v
       (.isBound v)
       (map? @v)
       ((my-every-pred :on-interface :on :sigs :var :method-map :method-builders) @v)))

(defn class-to-var
  "Take a class object that points to a var and return the Var object"
  [cls]
  (let [className (.replace (.getName cls) "_" "-")
        dot (.lastIndexOf className ".")
        ns (.substring className 0 dot)
        sym (.substring className (inc dot))]
    (when (find-ns (symbol ns))
      (find-var (symbol ns sym)))))

(defn protocol-class?
  "Return true if the class erpresents a protocol. We resolve this by finding the
associated var"
  [cls]
  (protocol? (class-to-var cls)))

(defn var-type 
  "Determing the type (var, function, macro, protocol) of a var from the metadata and
return it as a string."
  [v]
  (cond (:macro (meta v)) "macro"
        (= (:tag (meta v)) clojure.lang.MultiFn) "multimethod"
        (:arglists (meta v)) "function"
        (protocol? v) "protocol"
        :else "var"))

(defn vars-for-ns [ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (and (or (:wiki-doc (meta v)) (:doc (meta v)))
                   (not (protocol? v))
                   (not (:protocol (meta v)))
                   (not (:skip-wiki (meta v)))
                   (not (:private (meta v))))]
    v))

(defn var-info
  [v]
  (merge (select-keys (meta v) [:arglists :file :line
                                :added :deprecated :dynamic])
         {:name (name (:name (meta v)))
          :doc (remove-leading-whitespace (:doc (meta v))),
          :var-type (var-type v)
          :examples (:examples (meta v))}))

(defn vars-info [ns]
  (for [v (vars-for-ns ns)] 
    (var-info v)))



(let [primitive-map {Boolean/TYPE 'booleans,
                     Character/TYPE 'characters,
                     Byte/TYPE 'bytes,
                     Short/TYPE 'shorts,
                     Integer/TYPE 'ints,
                     Long/TYPE 'longs,
                     Float/TYPE 'floats,
                     Double/TYPE 'doubles,
                     Void/TYPE 'voids}]
  (defn expand-array-types
    "Expand array types to create a symbol like array-of-bytes so that it can parse
   (since [B doesn't parse as a symbol). Non-arrays are retuened as is."
    [#^Class cls]
    (cond
     (nil? cls) nil
     (.isArray cls) (symbol
                     (str "array-of-"
                          (name (expand-array-types
                                 (.getComponentType cls)))))
     (= cls java.lang.Object) 'Object
     (contains? primitive-map cls) (primitive-map cls)
     :else cls)))

(defn protos-for-ns
  "Find all the protocols in the namespace"
  [ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (and (protocol? v)
                   (or (:wiki-doc (meta v)) (:doc (meta v))
                       (seq (filter identity
                                    (map (comp :doc second)
                                         (:sigs @v))))))]
    v))

(defn proto-vars-info
  "Get the expanded list of functions for this protocol"
  [proto-var ns]
  (for [v (sort-by (comp :name meta) (vals (ns-interns ns)))
        :when (= (:protocol (meta v)) proto-var)]
    (var-info v)))

(defn protos-info
  "Build the info structure for the protocols"
  [ns]
  (for [p (protos-for-ns ns)]
    (merge (select-keys (meta p) [:file :line :added :deprecated])
           {:name (name (:name (meta p)))
            :doc (remove-leading-whitespace (:doc (meta p))),
            :var-type (var-type p)
            :fns (proto-vars-info p ns)
            :known-impls (map expand-array-types (keys (:impls @p)))})))

(defn types-for-ns
  "Discover the types and records in ns"
  [ns]
  ;; We rely on the fact that deftype creates a factory function in
  ;; the form ->TypeName to find the defined types in this namespace.
  (let [names (map #(.substring % 2)
                   (filter #(.startsWith % "->")
                           (sort
                            (map name
                                 (keys (ns-interns ns))))))
        ns-prefix (ns-to-class-name ns)
        ns-map (into
                {}
                (filter
                 second
                 (for [n names]
                   [n (try
                        (when-let [cls (Class/forName (str ns-prefix "." n))]
                          (reflect cls))
                        (catch Exception e))])))]
    (sort-by first ns-map)))

(def interfaces-to-skip #{'clojure.lang.IType 'clojure.lang.IRecord})

(defn types-info
  "Create the info structure for all the types in the namespace"
  [ns]
  (for [[type-name reflect-info] (types-for-ns ns)]
    (let [protocols (set (filter protocol-class? (:bases reflect-info)))
          record? ((:bases reflect-info) 'clojure.lang.IRecord)]
      {:name type-name
       :protocols (sort protocols)
       :interfaces (sort
                    (filter #(and (not (interfaces-to-skip %))
                                  (not (protocols %))
                                  (.isInterface (Class/forName (name %))))
                            (:bases reflect-info)))
       :var-type (if record? "record" "type")
       ;; Get the fields from the constructor function so they're in the right order
       :fields (first (:arglists (meta (get (ns-interns ns) (symbol (str "->" type-name))))))})))

(defn add-vars [ns-info]
  (merge ns-info {:members (vars-info (:ns ns-info))
                  :protocols (protos-info (:ns ns-info))
                  :types (types-info (:ns ns-info))}))

(defn relevant-namespaces []
  (filter #(not (:skip-wiki (meta %)))
          (map #(find-ns (symbol %)) 
               (filter #(some (fn [n] (or (= % n) (.startsWith % (str n "."))))
                              (params :namespaces-to-document))
                       (sort (map #(name (ns-name %)) (all-ns)))))))

(defn trim-ns-name [s]
  (let [trim-prefix (params :trim-prefix)]
    (if (and trim-prefix (.startsWith s trim-prefix))
      (subs s (count trim-prefix))
      s)))

(defn base-namespace
  "A nasty function that finds the shortest prefix namespace of this one"
  [ns relevant]
  (first 
   (drop-while 
    (comp not identity) 
    (map #(let [ns-part (find-ns (symbol %))]
            (if (and (not (:skip-wiki (meta ns-part)))
                     (relevant ns-part))
              ns-part))
         (let [parts (seq (.split (name (ns-name ns)) "\\."))]
           (map #(apply str (interpose "." (take (inc %) parts)))
                (range 0 (count parts)))))))) ;; TODO first arg to range was 0 for contrib

(defn base-relevant-namespaces []
  (let [relevant (relevant-namespaces)
        relevant-set (set relevant)]
    (filter #(= % (base-namespace % relevant-set)) relevant)))

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
  (merge (select-keys (meta ns) [:author :see-also :added :deprecated])
         {:full-name (name (ns-name ns)) :short-name (ns-short-name ns)
          :doc (remove-leading-whitespace (:doc (meta ns))) :ns ns}))

(defn build-ns-list [nss]
  (sort-by :short-name (map add-vars (map build-ns-entry nss))))

(defn add-subspaces [info]
     (assoc info :subspaces 
            (filter #(or (:doc %) (seq (:members %))
                         (seq (:types %)) (seq (:protocols %)))
                    (build-ns-list (sub-namespaces (:ns info))))))

(defn add-base-ns-info [ns]
  (assoc ns
    :base-ns (:short-name ns)
    :subspaces (map #(assoc % :base-ns (:short-name ns)) (:subspaces ns))))

(defn clean-ns-info 
  "Remove the back pointers to the namespace from the ns-info"
  [ns-info]
  (map (fn [ns] (assoc (dissoc ns :ns)
                  :subspaces (map #(dissoc % :ns) (:subspaces ns))))
       ns-info))

(defn project-info []
  (clean-ns-info
   (map add-base-ns-info
        (map add-subspaces
             (build-ns-list (base-relevant-namespaces))))))

(defn writer 
  "A version of duck-streams/writer that only handles file strings. Moved here for 
versioning reasons"
  [s]
  (java.io.PrintWriter.
   (java.io.BufferedWriter.
    (java.io.OutputStreamWriter. 
     (java.io.FileOutputStream. (java.io.File. s) false)
     "UTF-8"))))


(defn collect-info-to-file
  "build the file out-file with all the namespace info for the project described in param-dir"
  [param-file param-key param-dir out-file branch-name]
  (if (= param-file "nil")
    (params-from-dir param-dir)
    (params-from-file param-file param-key))
  (binding [params (merge params (some #(when (= branch-name (:name %)) (:params %)) (params :branches)))]
    (load-namespaces)
    (with-open [w (writer "/tmp/autodoc-debug.clj")] ; this is basically spit, but we do it
                                        ; here so we don't have clojure version issues
      (binding [saved-out *out*]
        (binding [*out* w]
          (pr (project-info)))))
    (with-open [w (writer out-file)] ; this is basically spit, but we do it
                                        ; here so we don't have clojure version issues
      (binding [saved-out *out*]
        (binding [*out* w]
          (pr (project-info)))))))
