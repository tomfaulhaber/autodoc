(ns com.infolace.gen-docs.build-html
  (:refer-clojure :exclude [empty complement]) 
  (:use net.cgrand.enlive-html
        [clojure.contrib.pprint.utilities :only (prlabel)]
        [clojure.contrib.duck-streams :only (with-out-writer)]))

;; TODO: consolidate and DRY defs
(def *file-prefix* "../wiki-work-area/")
(def *output-directory* (str *file-prefix* "wiki-src/"))

(def *overview-file* "overview.html")

(defn ns-html-file [ns-info]
  (str (:short-name ns-info) "-api.html"))

(defn namespace-overview [ns template]
  (at template
      [:#namespace-tag] 
      (do->
       (set-attr :id (:short-name ns))
       (content (:short-name ns)))
      [:#author] (content (or (:author ns) "unknown author"))
      [:a#api-link] (set-attr :href (ns-html-file ns))
      [:pre#namespace-docstr] (content (:doc ns))
      [:span#var-link] (clone-for [f (:members ns)]
                                  #(at % 
                                       [:a] (let [link (name (:name f))]
                                              (do->
                                               (set-attr :href
                                                         (str (ns-html-file ns) "#" link))
                                               (content link)))))))

(deftemplate overview *overview-file* [ns-info]
  [:.toc-entry] (clone-for [ns ns-info]
                           #(at % [:a] 
                                (do->
                                 (set-attr :href (str "#" (:short-name ns)))
                                 (content (:short-name ns)))))
  [:ul#left-sidebar-list] (clone-for [ns ns-info]
                                     #(at % [:a] 
                                          (do->
                                           (set-attr :href (ns-html-file ns))
                                           (content (:short-name ns)))))
  [:div#namespace-entry] (clone-for [ns ns-info] #(namespace-overview ns %)))

(defn make-overview [ns-info]
  (with-out-writer (str *output-directory* *overview-file*) 
    (print (apply str (overview ns-info)))))

