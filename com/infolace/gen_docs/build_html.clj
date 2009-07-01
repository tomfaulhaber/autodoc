(ns com.infolace.gen-docs.build-html
  (:refer-clojure :exclude [empty complement]) 
  (:use net.cgrand.enlive-html
        [clojure.contrib.pprint.utilities :only (prlabel)]
        [clojure.contrib.duck-streams :only (with-out-writer)]))

;; TODO: consolidate and DRY defs
(def *file-prefix* "../wiki-work-area/")
(def *output-directory* (str *file-prefix* "wiki-src/"))

(def *layout-file* "layout.html")
(def *master-toc-file* "master-toc.html")
(def *local-toc-file* "local-toc.html")

(def *overview-file* "overview.html")

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


(defn ns-html-file [ns-info]
  (str (:short-name ns-info) "-api.html"))

(defn add-ns-vars [ns]
  (clone-for [f (:members ns)]
             #(at % 
                  [:a] (let [link (name (:name f))]
                         (do->
                          (set-attr :href
                                    (str (ns-html-file ns) "#" link))
                          (content link))))))

(defn process-see-also
  "Take the variations on the see-also metadata and turn them into a canonical [link text] form"
  [see-also-seq]
  (map 
   #(cond
      (string? %) [% %] 
      (< (count %) 2) (repeat 2 %)
      :else %) 
   see-also-seq))

(defn namespace-overview [ns template]
  (at template
    [:#namespace-tag] 
    (do->
     (set-attr :id (:short-name ns))
     (content (:short-name ns)))
    [:#author] (content (or (:author ns) "unknown author"))
    [:a#api-link] (set-attr :href (ns-html-file ns))
    [:pre#namespace-docstr] (content (:doc ns))
    [:span#var-link] (add-ns-vars ns)
    [:span#subspace] (if-let [subspaces (seq (:subspaces ns))]
                       (clone-for [s subspaces]
                         #(at % 
                            [:span#name] (content (:short-name s))
                            [:span#sub-var-link] (add-ns-vars s))))
    [:span#see-also] (if-let [see-also (seq (:see-also ns))]
                       #(at % 
                          [:span#see-also-link] 
                          (clone-for [[link text] (process-see-also (:see-also ns))]
                            (fn [t] 
                              (at t
                                [:a] (do->
                                      (set-attr :href link)
                                      (content text)))))))))

(deftemplate overview (template-for *overview-file*) [ns-info]
  [:div#namespace-entry] (clone-for [ns ns-info] #(namespace-overview ns %)))

(deffragment make-master-toc *master-toc-file* [ns-info]
  [:ul#left-sidebar-list :li] (clone-for [ns ns-info]
                                #(at %
                                   [:a] (do->
                                         (set-attr :href (ns-html-file ns))
                                         (content (:short-name ns))))))

;;;TODO: factor out the ns-info so we can make different kinds of TOCs
(deffragment make-local-toc *local-toc-file* [ns-info]
  [:.toc-entry] (clone-for [ns ns-info]
                  #(at %
                     [:a] (do->
                           (set-attr :href (str "#" (:short-name ns)))
                           (content (:short-name ns))))))

(deftemplate page (template-for *layout-file*)
  [title master-toc local-toc page-content]
  [:html :head :title] (content title)
  [:div#leftcolumn] (content master-toc)
  [:div#right-sidebar] (content local-toc)
  [:div#content-tag] (content page-content))

(defn make-overview [ns-info]
  (with-out-writer (str *output-directory* *overview-file*) 
    (print (apply str (overview ns-info)))))

