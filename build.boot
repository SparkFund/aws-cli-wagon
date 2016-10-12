(def version "1.0.0")

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[adzerk/bootlaces "0.1.13" :scope "build"]
                 [org.apache.maven.wagon/wagon-provider-api "2.4"]])

(require '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
 pom {:project 'sparkfund/aws-cli-wagon
      :version "1.0.0"}
 aot {:namespace '[sparkfund.maven.wagon.aws-cli]}
 jar {:file "aws-cli-wagon.jar"})

(deftask build
  []
  (comp (aot) (pom) (jar) (target)))
