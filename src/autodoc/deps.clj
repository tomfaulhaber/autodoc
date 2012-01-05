(ns autodoc.deps
  (:require [lancet.core :as lancet])
  (:use [clojure.java.io :only [file]])
  (:import (java.io File)
           (org.apache.maven.artifact.ant Authentication DependenciesTask
                                          RemoteRepository RepositoryPolicy)
           (org.apache.maven.artifact.repository ArtifactRepositoryPolicy)
           (org.apache.maven.model Dependency Exclusion))
  
  )

;;; This whole file is adapted from leiningen 1.6.1.
;;; The only reason we did this is that it's hard to use lein as a library
;;; and it causes probs when we try to do it inside leiningen.

(def default-repos {"central" {:url "http://repo1.maven.org/maven2"
                               :snapshots false}
                    ;; TODO: possibly separate releases/snapshots in 2.0.
                    "clojars" {:url "http://clojars.org/repo/"}})

(defn- init-settings [id settings]
  (cond (string? settings) {:url settings}
        ;; infer snapshots/release policy from repository id
        (= "releases" id) (merge {:snapshots false} settings)
        (= "snapshots" id) (merge {:releases false} settings)
        :else settings))

(defn make-exclusion [excl]
  (doto (Exclusion.)
    (.setGroupId (or (namespace excl) (name excl)))
    (.setArtifactId (name excl))))

(defn make-dependency
  "Makes a dependency from a seq. The seq (usually a vector) should
contain a symbol to define the group and artifact id, then a version
string. The remaining arguments are combined into a map. The value for
the :classifier key (if present) is the classifier on the
dependency (as a string). The value for the :exclusions key, if
present, is a seq of symbols, identifying group ids and artifact ids
to exclude from transitive dependencies."
  ([dependency]
     (make-dependency dependency {}))
  ([dependency project]
     (make-dependency dependency project nil))
  ([dependency project scope]
     (when (and dependency (not (vector? dependency)))
       (throw (Exception. "Dependencies must be specified as vector:" #_dependency)))
     (let [[dep version & extras] dependency
           extras-map (apply hash-map extras)
           exclusions (:exclusions extras-map)
           classifier (:classifier extras-map)
           type (:type extras-map)
           es (map make-exclusion (concat exclusions
                                          (:exclusions project)))]
       (doto (Dependency.)
         ;; Allow org.clojure group to be omitted from clojure/contrib deps.
         (.setGroupId (if (and (nil? (namespace dep))
                               ;; TODO: drop contrib special-case in 2.0
                               (re-find #"^clojure(-contrib)?$" (name dep)))
                        "org.clojure"
                        (or (namespace dep) (name dep))))
         (.setArtifactId (name dep))
         (.setVersion version)
         (.setScope scope)
         (.setClassifier classifier)
         (.setType (or type "jar"))
         (.setExclusions es)))))

(defn repositories-for
  "Return a map of repositories including or excluding defaults."
  [project]
  (merge (when-not (:omit-default-repositories project)
           default-repos)
         (into {} (for [[id settings] (:repositories project)]
                    [id (init-settings id settings)]))))

(def update-policies {:daily ArtifactRepositoryPolicy/UPDATE_POLICY_DAILY
                      :always ArtifactRepositoryPolicy/UPDATE_POLICY_ALWAYS
                      :never ArtifactRepositoryPolicy/UPDATE_POLICY_NEVER})

(def checksum-policies {:fail ArtifactRepositoryPolicy/CHECKSUM_POLICY_FAIL
                        :ignore ArtifactRepositoryPolicy/CHECKSUM_POLICY_IGNORE
                        :warn ArtifactRepositoryPolicy/CHECKSUM_POLICY_WARN})

(defn- make-policy [policy-settings enabled?]
  (doto (RepositoryPolicy.)
    (.setUpdatePolicy (update-policies (:update policy-settings :daily)))
    ;; TODO: change default to :fail in 2.0
    (.setChecksumPolicy (checksum-policies (:checksum policy-settings :warn)))
    (.setEnabled (boolean enabled?))))

(defn- set-policies [repo {:keys [snapshots releases] :as settings}]
  (.addSnapshots repo (make-policy snapshots (:snapshots settings true)))
  (.addReleases repo (make-policy releases (:releases settings true))))

(defn make-auth [settings]
  (let [user-options (when-let [user-opts (resolve 'user/leiningen-auth)]
                       (get @user-opts (:url settings)))
        {:keys [username password passphrase
                private-key] :as settings} (merge user-options settings)
        auth (Authentication.)]
    (when (seq settings)
      (when username (.setUserName auth username))
      (when password (.setPassword auth password))
      (when passphrase (.setPassphrase auth passphrase))
      (when private-key (.setPrivateKey auth private-key))
      auth)))

(defn make-repository [[id settings]]
  (let [repo (RemoteRepository.)]
    (set-policies repo settings)
    (.setId repo id)
    (.setUrl repo (:url settings))
    (when-let [auth (make-auth settings)]
      (.addAuthentication repo auth))
    repo))

(defn make-repositories [project]
  (map make-repository (repositories-for project)))

;; Add symlinking to Lancet's toolbox.
(lancet/define-ant-task symlink symlink)

(defmulti copy-dependencies (fn [k destination flatten? fileset] k))

(defmethod copy-dependencies :default [k destination flatten? fileset]
  (lancet/copy {:todir destination :flatten (if flatten? "on" "off")}
               fileset))

;; TODO: remove in 2.0; with local-repo-classpath it's unnecessary
(defmethod copy-dependencies :symlink [k destination flatten? fileset]
  (let [files (.getIncludedFiles
               (.getDirectoryScanner fileset lancet/ant-project))
        dir (.getDir fileset)]
    ;; In principle, this should work... but it doesn't.
    ;; Instead we link each file in turn.
    #_(symlink {:action "record" :linkfilename destination}
               fileset)
    (doseq [f files]
      (symlink {:link destination
                :resource (.getCanonicalPath (File. dir f))}))))

(defn make-deps-task [project deps-set]
  (let [deps-task (DependenciesTask.)]
    (.setProject deps-task lancet/ant-project)
    ;; in maven-ant-tasks (at least 2.0.10 and 2.1.1) if there's an
    ;; exception thrown, there must be a call to
    ;; AbstractArtifactTask.getContainer() made to set some local
    ;; state on the task, before the exception happens, or else you
    ;; don't see stack traces. getContainer is a protected
    ;; method. Since we don't have contrib, we can't use
    ;; wall-hack-method, and clojure.lang.Reflector doesn't call
    ;; private methods, we'll call a public method that we know calls
    ;; getContainer, getSupportedProtocols.
    (.getSupportedProtocols deps-task)
    (.setBasedir lancet/ant-project (:root project))
    (.setFilesetId deps-task "dependency.fileset")
    (.setPathId deps-task (:name project))
    (doseq [repo (make-repositories project)]
      (.addConfiguredRemoteRepository deps-task repo))
    (doseq [dep (project deps-set)]
      (.addDependency deps-task (make-dependency dep project)))
    deps-task))

(defn do-deps [project deps-set]
  (let [deps-task (make-deps-task project deps-set)]
    (when (seq (deps-set project))
      (.execute deps-task)
      (when-not (and (:local-repo-classpath project)
                     (= :dependencies deps-set))
        (.mkdirs (File. (:library-path project)))
        (copy-dependencies (:jar-behavior project)
                           ;; Leiningen's process only has access to lib/dev.
                           (if (or (= :dev-dependencies deps-set)
                                   (and (:eval-in-leiningen project)
                                        (not= "leiningen" (:name project))))
                             (str (:root project) "/lib/dev")
                             (:library-path project))
                           true (.getReference lancet/ant-project
                                               (.getFilesetId deps-task)))))
    (.getReference lancet/ant-project (.getFilesetId deps-task))))

(defn- fileset-paths [fileset]
  (-> fileset
      (.getDirectoryScanner lancet/ant-project)
      (.getIncludedFiles)))

(defn- find-local-repo-jars [project]
  ;; TODO: Shut up, ant. You are useless. Nobody cares about what you say.
  ;; Removing ant-project loggers and redirecting their output streams
  ;; does nothing. How to suppress output?
  (for [path (fileset-paths (do-deps project :dependencies))]
    (file (System/getProperty "user.home") ".m2" "repository" path)))

;; TODO: memoize when not in tests
(defn ^{:internal true} find-jars
  "Returns a seq of Files for all the jars in the project's library directory."
  [project]
  (filter #(.endsWith (.getName %) ".jar")
          (concat (find-local-repo-jars project)
                  ;; This must be hard-coded because it's used in
                  ;; bin/lein and thus can't be changed in project.clj.
                  (.listFiles (file (:root project) "lib/dev")))))
