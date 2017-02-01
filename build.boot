(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[adzerk/bootlaces "0.1.13" :scope "build"]
                 [org.apache.maven.wagon/wagon-provider-api "2.4"]])

(def version "1.0.4")

(require '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
 pom {:project 'sparkfund/aws-cli-wagon
      :version version}
 aot {:namespace '[sparkfund.maven.wagon.aws-cli]}
 jar {:file "aws-cli-wagon.jar"})

(deftask build
  []
  (comp (aot) (build-jar) (target)))

(deftask local-install
  []
  (comp (pom) (aot) (build-jar) (install)))

(deftask snapshot
  []
  (comp (aot) (build-jar) (push-snapshot)))

(deftask release
  []
  (comp (aot) (build-jar) (push-release)))
