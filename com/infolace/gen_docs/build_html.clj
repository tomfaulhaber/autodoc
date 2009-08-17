(ns com.infolace.gen-docs.build-html
  (:refer-clojure :exclude [empty complement]) 
  (:import [java.util.jar JarFile]
           [java.io FileWriter BufferedWriter])
  (:require [clojure.contrib.str-utils2 :as str2])
  (:use net.cgrand.enlive-html
        com.infolace.gen-docs.params
        [clojure.contrib.pprint :only (cl-format)]
        [clojure.contrib.pprint.examples.json :only (print-json)]
        [clojure.contrib.pprint.utilities :only (prlabel)]
        [clojure.contrib.duck-streams :only (with-out-writer)]
        [clojure.contrib.shell-out :only (sh)]
        [com.infolace.gen-docs.collect-info :only (contrib-info)]))

(def *web-home* "http://richhickey.github.com/clojure-contrib/")
(def *output-directory* (str *file-prefix* "wiki-src/"))
(def *external-doc-tmpdir* "/tmp/autodoc/doc")

(def *layout-file* "layout.html")
(def *master-toc-file* "master-toc.html")
(def *local-toc-file* "local-toc.html")

(def *overview-file* "overview.html")
(def *namespace-api-file* "namespace-api.html")
(def *sub-namespace-api-file* "sub-namespace-api.html")
(def *index-html-file* "api-index.html")
(def *index-json-file* "api-index.json")

(defn template-for
  "Get the actual filename corresponding to a template"
  [base] 
  (str "templates/" base))

(defn get-template 
  "Get the html node corresponding to this template file"
  [base]
  (first (html-resource (template-for base))))

(defn content-nodes 
  "Strip off the <html><body>  ... </body></html> brackets that tag soup will add to
partial html data leaving a vector of nodes which we then wrap in a <div> tag"
  [nodes]
  {:tag :div, :content (:content (first (:content (first nodes))))})

(defmacro deffragment [name template-file args & body]
  `(defn ~name ~args
     (content-nodes
      (at (get-template ~template-file)
        ~@body))))

(defmacro with-template [template-file & body]
  `(content-nodes
    (at (get-template ~template-file)
      ~@body)))

;;; copied from enlive where this is private
(defn- xml-str
 "Like clojure.core/str but escapes < > and &."
 [x]
  (-> x str (.replace "&" "&amp;") (.replace "<" "&lt;") (.replace ">" "&gt;")))

;;; Thanks to Chouser for this regex
(defn expand-links 
  "Return an HTML string with links expanded into anchor tags."
  [s] 
  (str2/replace (xml-str s) 
                #"(\w+://.*?)([.>]*(?: |$))" 
                (fn [[_ url etc]] (str "<a href='" url "'>" url "</a>" etc))))

(deftemplate page (template-for *layout-file*)
  [title prefix master-toc local-toc page-content]
  [:html :head :title] (content title)
  [:link] #(assoc % :attrs (apply assoc (:attrs % {}) [:href (str prefix (:href (:attrs %)))]))
  [:div#leftcolumn] (content master-toc)
  [:div#right-sidebar] (content local-toc)
  [:div#content-tag] (content page-content))

(defn create-page [output-file title prefix master-toc local-toc page-content]
  (with-out-writer (str *output-directory* output-file) 
    (print
     (apply str (page title prefix master-toc local-toc page-content)))))

(defn ns-html-file [ns-info]
  (str (:short-name ns-info) "-api.html"))

(defn overview-toc-data 
  [ns-info]
  (for [ns ns-info] [(:short-name ns) (:short-name ns)]))

(defn var-tag-name [ns v] (str (:short-name ns) "/" (:name v)))

(defn var-toc-entries 
  "Build the var-name, <a> tag pairs for the vars in ns"
  [ns]
  (for [v (:members ns)] [(:name v) (var-tag-name ns v)]))

(defn ns-toc-data [ns]
  (apply 
   vector 
   ["Overview" "toc0" (var-toc-entries ns)]
   (for [sub-ns (:subspaces ns)]
     [(:short-name sub-ns) (:short-name sub-ns) (var-toc-entries sub-ns)])))

(defn var-url
  "Return the relative URL of the anchored entry for a var on a namespace detail page"
  [ns v] (str (ns-html-file (:base-ns ns)) "#" (var-tag-name ns v)))

(defn add-ns-vars [ns]
  (clone-for [v (:members ns)]
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

(defn namespace-overview [ns template]
  (at template
    [:#namespace-tag] 
    (do->
     (set-attr :id (:short-name ns))
     (content (:short-name ns)))
    [:#author] (content (or (:author ns) "unknown author"))
    [:a#api-link] (set-attr :href (ns-html-file ns))
    [:pre#namespace-docstr] (html-content (expand-links (:doc ns)))
    [:span#var-link] (add-ns-vars ns)
    [:span#subspace] (if-let [subspaces (seq (:subspaces ns))]
                       (clone-for [s subspaces]
                         #(at % 
                            [:span#name] (content (:short-name s))
                            [:span#sub-var-link] (add-ns-vars s))))
    [:span#see-also] (see-also-links ns)))

(deffragment make-overview-content *overview-file* [ns-info]
  [:div#namespace-entry] (clone-for [ns ns-info] #(namespace-overview ns %)))

(deffragment make-master-toc *master-toc-file* [ns-info]
  [:ul#left-sidebar-list :li] (clone-for [ns ns-info]
                                #(at %
                                   [:a] (do->
                                         (set-attr :href (ns-html-file ns))
                                         (content (:short-name ns))))))

(deffragment make-local-toc *local-toc-file* [toc-data]
  [:.toc-section] (clone-for [[text tag entries] toc-data]
                    #(at %
                       [:a] (do->
                             (set-attr :href (str "#" tag))
                             (content text))
                       [:.toc-entry] (clone-for [[subtext subtag] entries]
                                       (fn [node]
                                         (at node
                                           [:a] (do->
                                                 (set-attr :href (str "#" subtag))
                                                 (content subtext))))))))

(defn make-overview [ns-info master-toc]
  (create-page "index.html"
               "Clojure Contrib - Overview"
               nil
               master-toc
               (make-local-toc (overview-toc-data ns-info))
               (make-overview-content ns-info)))

;;; TODO: redo this so the usage parts can be styled
(defn var-usage [v]
  (if-let [arglists (:arglists v)]
    (cl-format nil
               "~<Usage: ~:i~@{~{(~a~{ ~a~})~}~^~:@_~}~:>~%"
               (map #(vector %1 %2) (repeat (:name v)) arglists))
    (if (= (:var-type v) "multimethod")
      "No usage documentation available")))

(def commit-hash-cache (ref {}))

(defn get-last-commit-hash
  "Gets the commit hash for the last commit that included this file. We
do this for source links so that we don't change them with every commit (unless that file
actually changed). This reduces the amount of random doc file changes that happen."
  [file]
  (dosync
   (if-let [hash (get @commit-hash-cache file)]
     hash
     (let [hash (.trim (sh "git" "rev-list" "--max-count=1" "HEAD" file 
                           :dir *src-dir*))]
       (alter commit-hash-cache assoc file hash)
       hash))))

(defn web-src-file [file]
  (cl-format nil "~a~a/src/~a" *web-src-dir* (get-last-commit-hash file) file))

(defn var-src-link [v]
  (when (and (:file v) (:line v))
    (cl-format nil "~a#L~d" (web-src-file (:file v)) (:line v))))

;;; TODO: factor out var from namespace and sub-namespace into a separate template.
(defn var-details [ns v template]
  (at template 
    [:#var-tag] 
    (do->
     (set-attr :id (var-tag-name ns v))
     (content (:name v)))
    [:span#var-type] (content (:var-type v))
    [:pre#var-usage] (content (var-usage v))
    [:pre#var-docstr] (html-content (expand-links (:doc v)))
    [:a#var-source] (set-attr :href (var-src-link v))))

(declare render-namespace-api)

(defn make-ns-content [ns]
  (render-namespace-api *namespace-api-file* ns))

(defn render-namespace-api [template-file ns]
  (with-template template-file
    [:#namespace-name] (content (:short-name ns))
    [:span#author] (content (or (:author ns) "Unknown"))
    [:span#long-name] (content (:full-name ns))
    [:pre#namespace-docstr] (html-content (expand-links (:doc ns)))
    [:span#see-also] (see-also-links ns)
    [:div#var-entry] (clone-for [v (:members ns)] #(var-details ns v %))
    [:div#sub-namespaces]
    (clone-for [sub-ns (:subspaces ns)]
      (fn [_] (render-namespace-api *sub-namespace-api-file* sub-ns)))))

(defn make-ns-page [ns master-toc]
  (create-page (ns-html-file ns)
               (str "clojure contrib - " (:short-name ns) " API reference")
               nil
               master-toc
               (make-local-toc (ns-toc-data ns))
               (make-ns-content ns)))

(defn vars-by-letter 
  "Produce a lazy seq of two-vectors containing the letters A-Z and Other with all the 
vars in ns-info that begin with that letter"
  [ns-info]
  (let [chars (conj (into [] (map #(str (char (+ 65 %))) (range 26))) "Other")
        var-map (apply merge-with conj 
                       (into {} (for [c chars] [c [] ]))
                       (for [v (mapcat #(for [v (:members %)] [v %]) ns-info)]
                         {(or (re-find #"[A-Z]" (-> v first :name .toUpperCase))
                              "Other")
                          v}))]
    (for [c chars] [c (sort-by #(-> % first :name .toUpperCase) (get var-map c))])))

(defn doc-prefix [v n]
  "Get a prefix of the doc string suitable for use in an index"
  (let [doc (:doc v)
        len (min (count doc) n)
        suffix (if (< len (count doc)) "..." ".")]
    (str (.replaceAll (.substring doc 0 len) "\n *" " ") suffix)))

(defn gen-index-line [v ns]
  (let [var-name (:name v)
        overhead (count var-name)
        short-name (:short-name ns)
        doc-len (+ 50 (min 0 (- 18 (count short-name))))]
    #(at %
       [:a] (do->
             (set-attr :href (str (ns-html-file ns) "#" (:name v)))
             (content (:name v)))
       [:#line-content] (content 
                        (cl-format nil "~vt~a~vt~a~vt~a~%"
                                   (- 29 overhead)
                                   (:var-type v) (- 43 overhead)
                                   short-name (- 62 overhead)
                                   (doc-prefix v doc-len))))))

;; TODO: skip entries for letters with no members
(deffragment make-index-content *index-html-file* [vars-by-letter]
  [:div#index-body] (clone-for [[letter vars] vars-by-letter]
                      #(at %
                         [:span#section-head] (content letter)
                         [:span#section-content] (clone-for [[v ns] vars]
                                                   (gen-index-line v ns)))))

(defn make-index-html [ns-info master-toc]
  (create-page *index-html-file*
               "Clojure Contrib - Index"
               nil
               master-toc
               nil
               (make-index-content (vars-by-letter ns-info))))

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

(defn namespace-index-info [ns]
  (assoc (select-keys ns [:doc :author])
    :name (:full-name ns)
    :wiki-url (str *web-home* (ns-html-file ns))
    :source-url (web-src-file (ns-file ns))))

(defn var-index-info [v ns]
  (assoc (select-keys v [:name :doc :author :arglists])
    :namespace (:full-name ns)
    :wiki-url (str *web-home* "/" (var-url ns v))
    :source-url (var-src-link v)))

(defn structured-index 
  "Create a structured index of all the reference information about contrib"
  [ns-info]
  (let [namespaces (concat ns-info (mapcat :subspaces ns-info))
        all-vars (mapcat #(for [v (:members %)] [v %]) namespaces)]
     {:namespaces (map namespace-index-info namespaces)
      :vars (map #(apply var-index-info %) all-vars)}))


(defn make-index-json
  "Generate a json formatted index file that can be consumed by other tools"
  [ns-info]
  (with-out-writer (BufferedWriter. (FileWriter. (str *output-directory* *index-json-file*)))
    (print-json (structured-index ns-info))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Wrap the external doc
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro select-content-text [node selectr]
  `(first (:content (first (select [~node] ~selectr)))))

(defn get-title [node]
  (or (select-content-text node [:title])
      (select-content-text node [:h1])))

(defn wrap-external-doc [staging-dir target-dir master-toc]
  (doall
   (for [file (filter #(.isFile %) (file-seq (java.io.File. staging-dir)))]
     (let [source-path (.getAbsolutePath file)
           offset (.substring source-path (inc (.length staging-dir)))
           target-path (str target-dir "/" offset)
           page-content (first (html-resource (java.io.File. source-path)))
           title (get-title page-content)
           prefix (apply str (repeat (count (.split offset "/")) "../"))]
       (create-page target-path title prefix master-toc nil page-content)
       [offset title]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Put it all together
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-all-pages []
  (let [ns-info (contrib-info)
        master-toc (make-master-toc ns-info)
        external-docs (wrap-external-doc *external-doc-tmpdir* "doc" master-toc)]
    (make-overview ns-info master-toc)
    (doseq [ns ns-info]
      (make-ns-page ns master-toc))
    (make-index-html ns-info master-toc)
    (make-index-json ns-info)))

