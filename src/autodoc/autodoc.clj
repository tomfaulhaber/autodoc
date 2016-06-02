(ns autodoc.autodoc
  (:use
   [clojure.pprint :only (cl-format)]
   [clojure.java.io :only [file make-parents]]
   [clojure.tools.namespace.find :only [find-namespaces-in-dir]]
   [autodoc.build-html :only (make-all-pages)]
   [autodoc.collect-info-wrapper :only (do-collect)]
   [autodoc.copy-statics :only (copy-statics)]
   [autodoc.gen-docs :only (gen-branch-docs)]
   [autodoc.params :only [merge-params params params-from-dir params-from-file
                          params-help process-command-line]])
  (:import
   [java.io File FileNotFoundException])
  (:gen-class))

(defn make-doc-dir [] (make-parents (file (params :output-path) "foo")))

(defn build-html
  "Build the documentation (default command)"
  [& _]
  (make-doc-dir)
  (copy-statics)
  ;; If load-classpath has been set to a list (as leiningen does, but you can too)
  ;; then spawn a separate process woth the right dependencies to scoop up the
  ;; doc info
  (let [ns-info (do-collect nil)]
    (make-all-pages ns-info)))

(defn sym-to-var [sym]
  (find-var (symbol "autodoc.autodoc" (name sym))))

(declare commands)

(defn help
  "Print this help message"
  [& _]
  (cl-format true
             "Usage: autodoc [params] cmd args~%~%")
  ;; TODO: We should have a fixed width tab (~20t) between the terms below, but something's funky
  (cl-format true "Available commands:~%~:{   ~a: ~a~%~}~%"
             (for [cmd commands]
               [cmd (:doc (meta (sym-to-var cmd)))]))
    (params-help *out*))

(defn list-keys
  "List the keys available in the specified --param-file arg"
  []
  (if-let [param-file (params :param-file)]
    (cl-format true "Keys specified in ~a:~%~{~a~%~}" param-file
               (sort (keys (load-file param-file))))
    (cl-format true "list-keys command requires that you specify --param-file option~%")))

(def commands ['build-html 'help 'list-keys])

(defn directory-name []
  (.replaceFirst (.getParent (.getAbsoluteFile (file "."))) ".*/" ""))

;;; Leiningen likes to make the source path include the absolute path to this directory
;;; for whatever reason, so we clean it up on the way in.
(defn clean-params [params]
  (if (and (params :root) (> (count (params :source-path)) 0))
    (update-in params [:source-path]
               #(vec (for [path %]
                       (if (.startsWith path (params :root))
                         (.substring path (inc (count (params :root))))
                         path))))
    params))

;;; We really shouldn't be special-casing this!
(defn has-branches? []
  (not (let [branches (params :branches)]
         (and (== (count branches) 1)
              (nil? (ffirst branches))))))

(defn autodoc
  ([myparams] (autodoc myparams nil))
  ([myparams cmd & cmd-args]
     (merge-params {:name (directory-name)})
     (when-let [dir (myparams :param-dir)]
       (if (.exists (File. (File. dir) "params.clj"))
         (params-from-dir dir)))
     (when-not (= cmd "list-keys")
       (when-let [f (myparams :param-file)]
         (when-not (.exists (File. f))
           (throw (FileNotFoundException. (str "Parameter file \"" f "\" doesn't exist."))))
         (params-from-file f (myparams :param-key))))
     (merge-params (clean-params myparams))
     (if (has-branches?)
       (do
         (copy-statics)
         (gen-branch-docs))
       (do
         (when (nil? (params :namespaces-to-document))
           (merge-params {:namespaces-to-document
                          (map
                           name
                           (mapcat
                            #(find-namespaces-in-dir
                              (file (params :root) %))
                            (params :source-path)))}))
         (if-let [cmd-sym ((set commands) (symbol (or cmd 'build-html)))]
           (apply (sym-to-var cmd-sym) cmd-args)
           (do
             (cl-format true "Unknown autodoc command: ~a~%" cmd)
             (help)))))))

(defn execute
  "Parse the command line arguments and call autodoc with the params"
  [& args]
  (if-let [[params args] (try
                           (process-command-line args)
                           (catch RuntimeException e
                             (println (.getMessage e))
                             (prn)
                             (help)))]
    (apply autodoc params args)))

(defn -main [& args]
  (try
    (apply execute args)
    (finally (shutdown-agents))))
