(ns autodoc.build-html
  (:refer-clojure :exclude [empty complement]) 
  (:import [java.util.jar JarFile]
           [java.io File FileWriter BufferedWriter StringReader]
           [java.util.regex Pattern])
  (:require [clojure.string :as str]
            [clojure.walk :as walk])
  (:use [net.cgrand.enlive-html :exclude (deftemplate)]
        [clojure.string :only (split-lines)]
        [clojure.java.io :only (as-file file writer)]
        [clojure.java.shell :only (sh)]
        [clojure.pprint :only (pprint cl-format)]
        [clojure.data.json :only (pprint-json)]
        [autodoc.params :only (params expand-classpath)]))

;;; TODO should these really be dynamic? I don't think so
(def ^:dynamic *layout-file* "layout.html")
(def ^:dynamic *master-toc-file* "master-toc.html")
(def ^:dynamic *local-toc-file* "local-toc.html")

(def ^:dynamic *overview-file* "overview.html")
(def ^:dynamic *description-file* "description.html")
(def ^:dynamic *namespace-api-file* "namespace-api.html")
(def ^:dynamic *sub-namespace-api-file* "sub-namespace-api.html")
(def ^:dynamic *index-html-file* "api-index.html")
(def ^:dynamic *index-clj-file* "index~@[-~a~].clj")
(def ^:dynamic *raw-index-clj-file* "raw-index~@[-~a~].clj")
(def ^:dynamic *index-json-file* "api-index.json")

(defn ns-to-class-name
  "Convert the namespace name into a class root name"
  [ns]
  (.replace ns "-" "_"))

(defn class-to-ns-name
  "Convert a class to the corresponding namespace name"
  [ns]
  (.replace ns "_" "-"))

(defn template-for
  "Get the actual filename corresponding to a template. We check in the project
specific directory first, then sees if a parameter with that name is set, then 
looks in the base template directory."
  [base] 
  (let [custom-template (File. (str (params :param-dir) "/templates/" base))]
    (if (.exists custom-template)
      custom-template
      (if-let [param (params (keyword (.replaceFirst base "\\.html$" "")))]
        (StringReader. param)
        (-> (clojure.lang.RT/baseLoader) (.getResourceAsStream (str "templates/" base)))))))

(def memo-nodes
     (memoize
      (fn [source]
        (if-let [source (template-for source)]
          (map annotate (select (html-resource source) [:body :> any-node]))))))

(defmacro deffragment
  [name source args & forms]
  `(def ~name
        (fn ~args
          (if-let [nodes# (memo-nodes ~source)]
            (flatmap (transformation ~@forms) nodes#)))))  

(def memo-html-resource
     (memoize
      (fn [source]
        (if-let [source (template-for source)]
          (html-resource source)))))

(defmacro deftemplate
  "A template returns a seq of string:
   Overridden from enlive to defer evaluation of the source until runtime.
   Builds in \"template-for\""
  [name source args & forms] 
  `(def ~name
        (comp emit* 
              (fn ~args
                (if-let [nodes# (memo-html-resource ~source)]
                  (flatmap (transformation ~@forms) nodes#))))))

(def url-regex #"\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

;;; Dropped in the 1.3 contrib architecture
(defn re-partition
  "Splits the string into a lazy sequence of substrings, alternating
between substrings that match the patthern and the substrings
between the matches. The sequence always starts with the substring
before the first match, or an empty string if the beginning of the
string matches.

For example: (re-partition #\"[a-z]+\" \"abc123def\")

Returns: (\"\" \"abc\" \"123\" \"def\")"
  [#^Pattern re string]
  (let [m (re-matcher re string)]
    ((fn step [prevend]
       (lazy-seq
        (if (.find m)
          (cons (.subSequence string prevend (.start m))
                (cons (re-groups m)
                      (step (+ (.start m) (count (.group m))))))
          (when (< prevend (.length string))
            (list (.subSequence string prevend (.length string)))))))
     0)))

(defn expand-links 
  "Return a seq of nodes with links expanded into anchor tags."
  [s]
  (when s
    (for [x (re-partition url-regex s)]
      (if (vector? x)
        [{:tag :a :attrs {:href (x 0)} :content [(x 0)]}]
        x))))

(deftemplate page *layout-file*
  [title prefix master-toc local-toc page-content]
  [:html :head :title] (content title)
  [:link] #(apply (set-attr :href (str prefix (:href (:attrs %)))) [%])
  [:img] #(apply (set-attr :src (str prefix (:src (:attrs %)))) [%])
  [:a#page-header] (content (or (params :page-title) (params :name)))
  [:div#leftcolumn] (content master-toc)
  [:div#right-sidebar] (content local-toc)
  [:div#content-tag] (content page-content)
  [:div#copyright] (content (params :copyright)))

(defn branch-subdir [branch-name] 
  (when branch-name (str "branch-" branch-name)))

(defn create-page [output-file branch title prefix master-toc local-toc page-content]
  (let [dir (if branch 
              (file (params :output-path) (branch-subdir branch))
              (file (params :output-path)))] 
    (when (not (.exists dir))
      (.mkdirs dir))
    (with-open [out  (writer (file dir output-file))] 
      (binding [*out* out]
        (print
         (apply str (page title prefix master-toc local-toc page-content)))))))

(defmulti ns-html-file class)

(defmethod ns-html-file clojure.lang.IPersistentMap [ns-info]
  (str (:short-name ns-info) "-api.html"))

(defmethod ns-html-file String [ns-name]
  (str ns-name "-api.html"))

(defn overview-toc-data 
  [ns-info]
  (for [ns ns-info] [(:short-name ns) (:short-name ns)]))

(defn var-tag-name [ns v] (str (:full-name ns) "/" (:name v)))

(defn var-toc-entries 
  "Build the var-name, <a> tag pairs for the vars in ns"
  [ns key]
  (seq (for [v (get ns key)] [(:name v) (var-tag-name ns v)
                              (for [proto-fn (:fns v)]
                                [(:name proto-fn) (var-tag-name ns proto-fn)])])))

(defn ns-toc-data [ns]
  (apply 
   vector 
   `(["Overview" "toc0"]
       ~@[(when-let [entries (var-toc-entries ns :protocols)]
            ["Protocols" "proto-section" entries])]
       ~@[(when-let [entries (var-toc-entries ns :types)]
            ["Types" "type-section" entries])]
       ~@[(when-let [entries (var-toc-entries ns :members)]
            ["Vars and Functions" "var-section" entries])]
       ;; TODO do all types for subspaces
       ~@(for [sub-ns (:subspaces ns)]
           [(:short-name sub-ns) (:short-name sub-ns)
            (concat
             (var-toc-entries sub-ns :protocols)
             (var-toc-entries sub-ns :types)
             (var-toc-entries sub-ns :members))]))))

(defn names-for-ns
  "Find all the names that we want to document in a namespace including
vars, types, protocols, and functions in protocols"
  [ns]
  (apply
   concat
   (:members ns)
   (:types ns)
   (:protocols ns)
   (for [proto (:protocols ns)]
     (:fns proto))))

(defn var-url
  "Return the relative URL of the anchored entry for a var on a namespace detail page"
  [ns v] (str (ns-html-file (:base-ns ns)) "#" (var-tag-name ns v)))

(defn add-ns-vars [ns]
  (clone-for [v (sort-by #(.toLowerCase (:name %)) (names-for-ns ns))]
             #(at % 
                [:a] (do->
                      (set-attr :href (var-url ns v))
                      (content (:name v))))))

(defn process-see-also
  "Take the variations on the see-also metadata and turn them into a canonical [link text] form"
  [see-also-seq]
  (map 
   #(cond
      (string? %) [% %] 
      (< (count %) 2) (repeat 2 %)
      :else %) 
   see-also-seq))

(defn see-also-links [ns]
  (if-let [see-also (seq (:see-also ns))]
    #(at %
       [:span#see-also-link] 
       (clone-for [[link text] (process-see-also (:see-also ns))]
         (fn [t] 
           (at t
             [:a] (do->
                   (set-attr :href link)
                   (content text))))))))

(defn external-doc-links [ns external-docs]
  (if-let [ns-docs (get external-docs (:short-name ns))]
    #(at %
       [:span#external-doc-link] 
       (clone-for [[link text] ns-docs]
         (fn [t] 
           (at t
             [:a] (do->
                   (set-attr :href (str "doc/" link))
                   (content text))))))))

(defn namespace-overview [ns template]
  (at template
    [:#namespace-tag] 
    (do->
     (set-attr :id (:short-name ns))
     (content (:short-name ns)))
    [:#author-line] (when (:author ns)
                 #(at % [:#author-name] 
                      (content (:author ns))))
    [:a#api-link] (set-attr :href (ns-html-file ns))
    [:pre#namespace-docstr] (content (expand-links (:doc ns)))
    [:span#var-link] (add-ns-vars ns)
    [:span#subspace] (if-let [subspaces (seq (:subspaces ns))]
                       (clone-for [s subspaces]
                         #(at % 
                            [:span#name] (content (:short-name s))
                            [:span#sub-var-link] (add-ns-vars s))))
    [:span#see-also] (see-also-links ns)
    [:.ns-added] (when (:added ns)
                   #(at % [:#content]
                        (content (str "Added in " (params :name)
                                      " version " (:added ns)))))
    [:.ns-deprecated] (when (:deprecated ns)
                        #(at % [:#content]
                             (content (str "Deprecated since " (params :name)
                                           " version " (:deprecated ns)))))))

(deffragment make-project-description *description-file* [])

(deffragment make-overview-content *overview-file* [branch-info ns-info]
  [:span#header-project] (content (or (params :name) "Project"))
  [:span#header-version] (content (:version branch-info))
  [:span#header-status-block] (when (:status branch-info)
                                #(at % [:span#header-status]
                                     (content (:status branch-info))))
  [:div#project-description] (content (or 
                                       (make-project-description)
                                       (params :description)))
  [:div#home-page] (when-let [home (params :project-home)]
                     #(at %
                          [:a] (do->
                                (set-attr :href home)
                                (content home))))
  [:div#namespace-entry] (clone-for [ns ns-info] #(namespace-overview ns %)))

(deffragment make-master-toc *master-toc-file* [ns-info branch-info all-branch-info prefix]
  [:#project-name] (content (:name params))
  [:#version] (content (:version branch-info))
  [:div.NamespaceTOC] #(when (> (count ns-info) 1)
                         (at %
                             [:ul#left-sidebar-list :li]
                             (clone-for [ns ns-info]
                                        (fn [n]
                                          (at n
                                              [:a] (do->
                                                    (set-attr :href (ns-html-file ns))
                                                    (content (:short-name ns))))))))
  [:div.BranchTOC] #(when (> (count all-branch-info) 1)
                      (at %
                          [:ul#left-sidebar-branch-list :li]
                          (clone-for [{:keys [version name status]}
                                      (filter (fn [{v :version}]
                                                (not (= v (:version branch-info))))
                                              all-branch-info)]
                                     (let [subdir (if (= version (:version (first all-branch-info)))
                                                    nil
                                                    (str (branch-subdir name) "/"))]
                                       (fn [n] 
                                         (at n 
                                             [:a] (do->
                                                   (set-attr :href (str prefix subdir "index.html"))
                                                   (content (cl-format nil "~a (~a)" version status))))))))))

(deffragment make-local-toc *local-toc-file* [toc-data]
  [:.toc-section] (clone-for [[text tag entries] toc-data]
                    #(at %
                       [:a] (do->
                             (set-attr :href (str "#" tag))
                             (content text))
                       [:.toc-entry] (clone-for [[subtext subtag subentries] entries]
                                       (fn [node]
                                         (at node
                                           [:a.toc-entry-anchor] (do->
                                                                  (set-attr :href (str "#" subtag))
                                                                  (content subtext))
                                           [:.toc-subentry] (clone-for [[subtext subtag] subentries]
                                                              (fn [node]
                                                                (at node
                                                                    [:a ] (do->
                                                                           (set-attr :href (str "#" subtag))
                                                                           (content subtext)))))))))))

(defn make-overview [ns-info master-toc branch-info prefix]
  (create-page "index.html"
               (when (not (:first? branch-info)) (:name branch-info))
               (cl-format nil "Overview - ~a~@[ ~a~] API documentation" (params :name) (:version branch-info))
               prefix
               master-toc
               (make-local-toc (overview-toc-data ns-info))
               (make-overview-content branch-info ns-info)))

(defn- add-gensyms
  "Walk the arglist tree and convert any symbols gensymed with #
back to their foo# form. This can happen when a function is created
with a macro. In addition to looking better, this keeps the
generated HTML files from having gratuitous diffs."
  [arglists]
  (let [xform (fn [s] (if (symbol? s)
                        (symbol (.replaceFirst (name s) "__\\d+__auto__" "#"))
                        s))]
    (walk/postwalk xform arglists)))

;;; TODO: redo this so the usage parts can be styled
(defn var-usage [v]
  (if-let [arglists (add-gensyms (:arglists v))]
    (cl-format nil
               "~<Usage: ~:i~@{~{(~a~{ ~a~})~}~^~:@_~}~:>~%"
               (map #(vector %1 %2) (repeat (:name v)) arglists))
    (if (= (:var-type v) "multimethod")
      "No usage documentation available")))

(defn- git-get-last-commit-hash [file branch]
  (let [hash (.trim (:out (sh "git" "rev-list" "--max-count=1" "HEAD" file
                              :dir (params :root))))]
    (when (not (or (zero? (count hash)) (.startsWith hash "fatal")))
      hash)))

(defn- hg-get-last-commit-hash [file branch]
  (let [res (.trim (:out (sh "hg" "log" "--limit" "1" file :dir (params :root))))]
    (when (not (or (zero? (count res)) (.startsWith res "fatal")))
      (->> res split-lines first (re-find #"\w+$")))))

(def 
 #^{:doc "Gets the commit hash for the last commit that included this file. We
do this for source links so that we don't change them with every commit (unless that file
actually changed). This reduces the amount of random doc file changes that happen."}
 get-last-commit-hash
 (memoize
  (fn [file branch]
    (case (params :scm-tool)
          "git" (git-get-last-commit-hash file branch)
          "hg" (hg-get-last-commit-hash file branch)))))

(defn web-src-file [file branch]
  (when-let [web-src-dir (params :web-src-dir)]
    (when-let [hash (get-last-commit-hash file branch)]
      (cl-format nil "~a~a/~a" web-src-dir hash file))))

(defn web-raw-src-file [file branch]
  (when-let [web-raw-src-dir (and (params :web-src-dir)
                                  (str/replace (params :web-src-dir) #"/blob/$" "/raw/"))]
    (when-let [hash (get-last-commit-hash file branch)]
      (cl-format nil "~a~a/~a" web-raw-src-dir hash file))))

(def src-prefix-length
  (memoize
   (fn []
     (.length (.getPath (File. (params :root)))))))

(def memoized-working-directory
     (memoize 
      (fn [] (.getAbsolutePath (file ".")))))

(def expand-src-file 
     (memoize
      (fn [f branch] 
        (let [fl (as-file f)]
          (if (.isAbsolute fl)
            f
            (if-let [result (first 
                             (filter #(.exists %)
                                     (map #(File. % f)
                                          (expand-classpath 
                                           branch
                                           (params :root) 
                                           (params :load-classpath)))))]
              (.getAbsolutePath result)
              (do 
                (cl-format *err* "No absolute path for file metadata ~a~%" f)
                nil)))))))

(defn var-base-file
  "strip off the prepended path to the source directory from the filename"
  [f branch]
  (let [f (or (expand-src-file f branch) f)]
    (cond
     (.startsWith f (params :root)) (.substring f (inc (src-prefix-length)))
     (.startsWith f (memoized-working-directory)) (.substring f (inc (.length (memoized-working-directory))))
     true (.getPath (file (first (params :source-path)) f))))) ;; TODO: we really should consider *all* elements of the source path here

(defn var-src-link [v branch]
  (when (and (:file v) (:line v))
    (when-let [web-file (web-src-file (var-base-file (:file v) branch) branch)]
      (let [link-format (case (params :scm-tool)
                              "git" "~a#L~d"
                              "hg" "~a#cl-~d"
                              "~a")]
        (cl-format nil link-format web-file (:line v))))))

;;; TODO: factor out var from namespace and sub-namespace into a separate template.
(defn var-details [ns v template branch-info]
  (at template 
    [:#var-tag] 
    (do->
     (set-attr :id (var-tag-name ns v))
     (content (:name v)))
    [:span#var-type] (content (:var-type v))
    [:pre#var-usage] (content (var-usage v))
    [:pre#var-docstr] (content (expand-links (:doc v)))
    [:a#var-source] (fn [n] (when-let [link (var-src-link v (:name branch-info))]
                              (apply (set-attr :href link) [n])))
    [:.var-added] (when (:added v)
                   #(at % [:#content]
                        (content (str "Added in " (params :name)
                                      " version " (:added v)))))
    [:.var-deprecated] (when (:deprecated v)
                        #(at % [:#content]
                             (content (str "Deprecated since " (params :name)
                                           " version " (:deprecated v)))))))

(declare common-namespace-api)

(deffragment render-sub-namespace-api *sub-namespace-api-file*
 [ns branch-info ns-info external-docs]
  (common-namespace-api ns branch-info ns-info external-docs))

(deffragment render-namespace-api *namespace-api-file*
 [ns branch-info ns-info external-docs]
  (common-namespace-api ns branch-info ns-info external-docs))

(defn make-ns-content [ns branch-info ns-info external-docs]
  (render-namespace-api ns branch-info ns-info external-docs))

(defn process-links
  "Take a list and build either a raw string or a link depending if we have the link"
  [list]
  (for [item list] (if (vector? item)
                     {:tag :a :attrs {:href (item 1)} :content (item 0)}
                     item)))

(defn flatten-namespaces
  "Build a list of namespaces and sub-namespaces"
  [ns-info]
  (mapcat #(conj (:subspaces %) %) ns-info))

(defn find-impls
  "Find all the implementations of this protocol in all the namespaces
that we're documenting"
  [p this-ns ns-info]
  (process-links
   (let [full-name (ns-to-class-name (str (:full-name this-ns) "." (:name p)))]
     (sort-by
      #(.toLowerCase (if (vector? %)
                       (first %)
                       (str %)))
      (apply
       concat
       (map #(if (nil? %) "nil" (str %))
            (:known-impls p))
       (for [ns (flatten-namespaces ns-info)]
         (for [type (:types ns)
               :when (some #(= full-name (str %)) (:protocols type))]
           (if (not= this-ns ns)
             ;; TODO Consider sub-namespaces and prefixes for branch directories
             [(str (:short-name ns) "." (:name type))
              (str (:short-name ns) "-api.html#" (:short-name ns) "/" (:name type))]
             [(:name type) (str "#" (:short-name ns) "/" (:name type))]))))))))

(defn proto-details [ns p loc branch-info ns-info]
  (at loc
      [:#proto-tag] 
      (do->
       (set-attr :id (var-tag-name ns p))
       (content (:name p)))
      [:pre#proto-docstr] (content (expand-links (:doc p)))
      [:span#proto-impls] (content (interpose ", " (find-impls p ns ns-info)))
      [:a#proto-source] (fn [n] (when-let [link (var-src-link p (:name branch-info))]
                                  (apply (set-attr :href link) [n])))
      [:.proto-added] (when (:added p)
                        #(at % [:#content]
                             (content (str "Added in " (params :name)
                                           " version " (:added p)))))
      [:.proto-deprecated] (when (:deprecated p)
                             #(at % [:#content]
                                  (content (str "Deprecated since " (params :name)
                                                " version " (:deprecated p)))))
      [:div#proto-var-entry] (clone-for [v (:fns p)]
                                        #(var-details ns v % branch-info))))

(defn render-protos
  [ns proto-list branch-info ns-info]
  (when (seq proto-list)
    (fn [loc]
      (at loc [:div#proto-entry]
          (clone-for [p proto-list]
                     #(proto-details ns p % branch-info ns-info))))))

(defn proto-with-link [ns-info this-ns proto-name]
  (let [[_ raw-ns base-name] (re-matches #"^(.*)\.([^.]+)$" (str proto-name))
        ns (class-to-ns-name raw-ns)]
    (if-let [target-ns (first (filter #(= (:full-name %) ns) ns-info))]
      (if (= ns (:full-name this-ns))
        {:tag :a
         :attrs {:href (str "#" ns "/" base-name)}
         :content base-name}
        {:tag :a
         :attrs {:href (str (:base-ns target-ns) "-api.html#" ns
                            "/" base-name)}
         :content (str ns "/" base-name)})
      (str ns "/" base-name))))

(defn type-details [ns t loc branch-info ns-info]
    (let [expanded-ns-info (flatten-namespaces ns-info)]
      (at loc
          [:#type-tag] 
          (do->
           (set-attr :id (var-tag-name ns t))
           (content (:name t)))
          [:span#type-type] (content (:var-type t))
          [:pre#type-docstr] (content (expand-links (:doc t))) ; Not yet supported
          [:span#type-fields] (content (str "[" (str/join " " (:fields t)) "]"))
          [:span#type-protocols] (content (interpose ", "
                                                     (map (partial proto-with-link
                                                                   expanded-ns-info
                                                                   ns)
                                                          (:protocols t))))
          [:span#type-interfaces] (content (str/join ", " (:interfaces t))))))

(defn render-types
  [ns type-list branch-info ns-info]
  (when (seq type-list)
    (fn [loc]
      (at loc [:div#type-entry]
          (clone-for [t type-list]
                     #(type-details ns t % branch-info ns-info))))))

(defn render-vars
  [ns var-list branch-info]
  (when (seq var-list)
    (fn [loc]
      (at loc [:div#var-entry]
          (clone-for [v var-list]
                     #(var-details ns v % branch-info))))))

(defn common-namespace-api [ns branch-info ns-info external-docs]
  (fn [node]
    (at node
        [:#namespace-name] (do->
                            (set-attr :id (:short-name ns))
                            (content (:short-name ns)))
        [:#header-project] (content (:name params))
        [:#header-version] (content (:version branch-info))
        [:#header-status-block] (when (:status branch-info)
                                  #(at % [:span#header-status]
                                       (content (:status branch-info))))
        [:span#author-line] (when (:author ns)
                              #(at % [:#author-name] 
                                   (content (:author ns))))
        [:span#long-name] (content (:full-name ns))
        [:div#home-page] (when (= (count ns-info) 1)
                           (when-let [home (params :project-home)]
                             #(at %
                                  [:a] (do->
                                        (set-attr :href home)
                                        (content home)))))
        [:pre#namespace-docstr] (content (expand-links (:doc ns)))
        [:span#see-also] (see-also-links ns)
        [:.ns-added] (when (:added ns)
                       #(at % [:#content]
                            (content (str "Added in " (params :name) " version " (:added ns)))))
        [:.ns-deprecated] (when (:deprecated ns)
                            #(at % [:#content]
                                 (content (str "Deprecated since " (params :name)
                                               " version " (:deprecated ns)))))
        [:span#external-doc] (external-doc-links ns external-docs)
        [:div#proto-section] (render-protos ns (:protocols ns) branch-info ns-info)
        [:div#type-section] (render-types ns (:types ns) branch-info ns-info)
        [:div#var-section] (render-vars ns (:members ns) branch-info)
        [:div#sub-namespaces]
        (substitute (map #(render-sub-namespace-api % branch-info ns-info external-docs)
                         (:subspaces ns))))))

(defn make-ns-page [unique-ns? ns master-toc external-docs branch-info prefix ns-info]
  (create-page (if unique-ns? "index.html" (ns-html-file ns))
               (when (not (:first? branch-info)) (:name branch-info))
               (cl-format nil "~a - ~a~@[ ~a~] API documentation"
                          (:short-name ns) (params :name) (:version branch-info))
               prefix
               master-toc
               (make-local-toc (ns-toc-data ns))
               (make-ns-content ns branch-info ns-info external-docs)))

(defn vars-by-letter 
  "Produce a lazy seq of two-vectors containing the letters A-Z and Other with all the 
vars in ns-info that begin with that letter"
  [ns-info]
  (let [chars (conj (into [] (map #(str (char (+ 65 %))) (range 26))) "Other")
        var-map (apply merge-with conj 
                       (into {} (for [c chars] [c [] ]))
                       (for [v (mapcat #(for [v (names-for-ns %)] [v %])
                                       (concat ns-info (mapcat :subspaces ns-info)))]
                         {(or (re-find #"[A-Z]" (-> v first :name .toUpperCase))
                              "Other")
                          v}))]
    (for [c chars] [c (sort-by #(-> % first :name .toUpperCase) (get var-map c))])))

(defn doc-prefix [v n]
  "Get a prefix of the doc string suitable for use in an index"
  (if-let [doc (:doc v)]
    (let [len (min (count doc) n)
          suffix (if (< len (count doc)) "..." ".")]
      (str (.replaceAll 
            (.replaceFirst (.substring doc 0 len) "^[ \n]*" "")
            "\n *" " ")
           suffix))
    ""))

(defn gen-index-line [v ns unique-ns?]
  (let [var-name (:name v)
        overhead (count var-name)
        short-name (:short-name ns)
        doc-len (+ 50 (min 0 (- 18 (count short-name))))]
    #(at %
         [:a] (do->
               (set-attr :href
                         (str (if unique-ns? "index.html" (ns-html-file (:base-ns ns)))
                              "#" (:full-name ns) "/" (:name v)))
               (content (:name v)))
         [:#line-content] (content 
                           (cl-format nil "~vt~a~vt~a~vt~a~%"
                                      (- 29 overhead)
                                      (:var-type v) (- 43 overhead)
                                      short-name (- 62 overhead)
                                      (doc-prefix v doc-len))))))

;; TODO: skip entries for letters with no members
(deffragment make-index-content *index-html-file* [branch-info vars-by-letter unique-ns?]
  [:#header-project] (content (:name params))
  [:#header-version] (content (:version branch-info))
  [:#header-status-block] (when (:status branch-info)
                            #(at % [:span#header-status]
                                 (content (:status branch-info))))
  [:#header-status] (content (:status branch-info))
  [:.project-name-span] (content (:name params))
  [:div#index-body] (clone-for [[letter vars] vars-by-letter]
                      #(at %
                         [:h2] (set-attr :id letter)
                         [:span#section-head] (content letter)
                         [:span#section-content] (clone-for [[v ns] vars]
                                                   (gen-index-line v ns unique-ns?)))))

(defn make-index-html [ns-info master-toc branch-info prefix]
  (create-page *index-html-file*
               (when (not (:first? branch-info)) (:name branch-info))
               (cl-format nil "Index - ~a~@[ ~a~] API documentation"
                          (params :name) (:version branch-info))
               prefix
               master-toc
               nil
               (make-index-content branch-info (vars-by-letter ns-info)
                                   (<= (count ns-info) 1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Make the JSON index
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ns-file 
  "Get the file name (relative to src/ in clojure.contrib) where a namespace lives" 
  [ns]
  (let [ns-name (.replaceAll (:full-name ns) "-" "_")
        ns-file (.replaceAll ns-name "\\." "/")]
    (str ns-file ".clj")))

(defn namespace-index-info [ns branch]
  (assoc (select-keys ns [:doc :author])
    :name (:full-name ns)
    :wiki-url (str (params :web-home) (ns-html-file ns))
    :source-url (web-src-file
                 (.getPath
                  (file (first (params :source-path)) ; TODO: consider *all* elements of the source path here
                        (ns-file ns))) branch)))

(defn var-index-info [v ns branch]
  (assoc (select-keys v [:name :doc :author :arglists :var-type :line :added :deprecated :dynamic])
    :namespace (:full-name ns)
    :wiki-url (str (params :web-home) "/" (var-url ns v))
    :source-url (var-src-link v branch)
    :raw-source-url (when (:file v)
                      (web-raw-src-file (var-base-file (:file v) branch) branch))
    :file (when (:file v)
            (var-base-file (:file v) branch))))

(defn structured-index 
  "Create a structured index of all the reference information about contrib"
  [ns-info branch]
  (let [namespaces (concat ns-info (mapcat :subspaces ns-info))
        all-vars (mapcat #(for [v (names-for-ns %)] [v %]) namespaces)]
     {:namespaces (map #(namespace-index-info % branch) namespaces)
      :vars (map (fn [[v ns]] (apply var-index-info v ns branch [])) all-vars)}))


(defn make-index-clj
  "Generate a Clojure formatted index file that can be consumed by other tools"
  [ns-info branch-info]
  (with-open  [out (writer (file (params :output-path) 
                                 (cl-format nil *index-clj-file*
                                            (:version branch-info))))]
    (binding [*out* out]
      (pprint (structured-index ns-info (:name branch-info))))))

(defn make-raw-index-clj [ns-info branch-info]
  (with-open [out (writer (file (params :output-path)
                                (cl-format nil *raw-index-clj-file*
                                           (:version branch-info))))]
    (binding [*out* out]
      (pprint ns-info))))

(defn make-index-json
  "Generate a json formatted index file that can be consumed by other tools"
  [ns-info branch-info]
  (when (params :build-json-index)
    (with-open  [out (writer (file (params :output-path)
                                   (str (when (not (:first? branch-info))
                                          (str (branch-subdir (:name branch-info)) "/")) 
                                        *index-json-file*)))]
      (binding [*out* out]
        (pprint-json (structured-index ns-info (:name branch-info)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Wrap the external doc
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-href-prefix [node prefix]
  (at node
    [:a] #(apply (set-attr :href (str prefix (:href (:attrs %)))) [%])))

(defmacro select-content-text [node selectr]
  `(first (:content (first (select [~node] ~selectr)))))

(defn get-title [node]
  (or (select-content-text node [:title])
      (select-content-text node [:h1])))

(defn external-doc-map [v]
  (apply 
   merge-with concat
   (map 
    (partial apply assoc {}) 
    (for [[offset title :as elem] v]
      (let [[_ dir nm] (re-find #"(.*/)?([^/]*)\.html" offset)
            package (if dir (apply str (interpose "." (into [] (.split dir "/")))))]
        (if dir
          [package [elem]]
          [nm [elem]]))))))

(defn wrap-external-doc [target-dir master-toc]
  (when target-dir
    (external-doc-map
     (doall          ; force the side effect (wrapping html files)
      (for [file (filter #(and (.isFile %) (.endsWith (.getPath %) ".html"))
                         (file-seq (java.io.File.
                                    (java.io.File. (params :output-path))
                                    target-dir)))]
        (let [path (.getAbsolutePath file)
              offset (.substring path (.length (params :output-path)))
              page-content (first (html-resource (java.io.File. path)))
              title (get-title page-content)
              prefix (apply str (repeat (dec (count (.split offset "/"))) "../"))]
          (create-page offset nil title prefix
                       (add-href-prefix master-toc prefix) nil page-content)
          [(.substring offset (inc (.length target-dir))) title]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Put it all together
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-all-pages 
  ([ns-info] (make-all-pages {:first? true} nil ns-info))
  ([branch-info all-branch-info ns-info]
     (let [doc-dir (str (when-not (:first? branch-info) 
                          (str (branch-subdir (:name branch-info)) "/")) 
                        "doc")]
       (let [prefix (if (:first? branch-info) nil "../")
             master-toc (make-master-toc ns-info branch-info all-branch-info prefix)
             external-docs (wrap-external-doc doc-dir master-toc)]
         (if (> (count ns-info) 1)
           (do (make-overview ns-info master-toc branch-info prefix)
               (doseq [ns ns-info]
                 (make-ns-page false ns master-toc external-docs branch-info prefix ns-info)))
           (make-ns-page true (first ns-info) master-toc external-docs branch-info prefix ns-info))
         (make-index-html ns-info master-toc branch-info prefix)
         (make-index-clj ns-info branch-info)
         (when (params :build-raw-index)
           (make-raw-index-clj ns-info branch-info))
         (make-index-json ns-info branch-info)))))

