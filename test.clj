(comment
  (do 
    (refer 'com.infolace.gen-docs.collect-info)
    (refer 'com.infolace.gen-docs.build-html)
    (refer 'clojure.contrib.pprint))

  (do
    (load "com/infolace/gen_docs/build_html")
    (make-all-pages)
    )

  (def ns-info (contrib-info))
  (def master-toc (make-master-toc ns-info))
  (defn make-page [ns-name]
    (let [ns (first (filter #(= (:short-name %) ns-name) ns-info))]
      (make-ns-page ns master-toc)))


  (def ns-pprint (first (filter #(= (:short-name %) "pprint") ns-info )))
  (make-ns-page ns-pprint master-toc)
  )
