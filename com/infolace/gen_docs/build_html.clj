(ns com.infolace.gen-docs.build-html
  (:refer-clojure :exclude [empty complement]) 
  (:use net.cgrand.enlive-html
        [clojure.contrib.duck-streams :only (with-out-writer)]))

;; TODO: consolidate and DRY defs
(def *file-prefix* "../wiki-work-area/")
(def *output-directory* (str *file-prefix* "wiki-src/"))

(def *overview-file* "overview.html")

(deftemplate overview *overview-file* [ns-info]
  [:.toc-entry] (clone-for [ns ns-info]
                           #(at % [:a] 
                                (do->
                                 (set-attr :href (str "#" (:short-name ns)))
                                 (content (:short-name ns))))))

(defn make-overview [ns-info]
  (with-out-writer (str *output-directory* *overview-file*) 
    (print (apply str (overview ns-info)))))

