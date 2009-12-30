(ns autodoc.ant-wrapper
  (:import
   [java.io File]
   [org.apache.tools.ant Project ProjectHelper AntClassLoader DefaultLogger])
  (use [autodoc.params :only (params params-from-dir)]))

(def param-map
     {'work-root-dir :file-prefix,
      'src-dir :src-dir,
      'output-dir :output-directory,
      'built-clojure-jar :built-clojure-jar
      'clojure-contrib-jar :clojure-contrib-jar,
      'clojure-contrib-classes :clojure-contrib-classes,
      'ext-dir :ext-dir})

(defn ant-wrapper
  [param-dir build-target force]
  (params-from-dir param-dir)
  (let [p (Project.)
        helper (ProjectHelper/getProjectHelper)
        acl (AntClassLoader. p nil true)
        build-file (File. "build.xml")]
    (doto p
      (.addBuildListener (doto (DefaultLogger.)
                           (.setOutputPrintStream System/out)
                           (.setErrorPrintStream System/err)
                           (.setMessageOutputLevel Project/MSG_INFO)))
      (.setUserProperty "ant.file" (.getAbsolutePath build-file))
      (.setUserProperty "param-dir" param-dir)
      (.setUserProperty "force" (if force "true" "false"))
      (.setUserProperty "src-files" (str (params :src-dir) (params :src-root)))
      (.init)
      (.addReference "ant.projectHelper" helper))
    (doseq [item param-map] 
      (if-let [param (params (second item))]
        (.setUserProperty p (name (first item)) param)))
    (.parse helper p build-file)
    (.executeTarget p build-target)))


