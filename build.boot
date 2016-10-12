(def version "1.0.0")

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.apache.maven.wagon/wagon-provider-api "2.4"]])

(task-options!
 pom {:project 'sparkfund/aws-cli-wagon
      :version "1.0.0"}
 aot {:namespace '[sparkfund.maven.wagon.aws-cli]}
 jar {:file "aws-cli-wagon.jar"})

(deftask build
  []
  (comp (aot) (pom) (jar) (target)))
