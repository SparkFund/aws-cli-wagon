(ns sparkfund.maven.wagon.aws-cli
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import
   [java.io FileInputStream]
   [java.util UUID]
   [org.apache.maven.wagon.resource Resource]
   [org.apache.maven.wagon.events TransferEvent]
   [org.apache.maven.wagon ResourceDoesNotExistException TransferFailedException]
   [org.apache.maven.wagon.authorization AuthorizationException])
  (:gen-class
   :extends org.apache.maven.wagon.AbstractWagon
   :init init
   :state state))

(defn obtain-role-env
  "Obtains temporary credentials for the given role. This return a map of
   environment variables suitable for use by `clojure.java.shell/sh`"
  [role]
  (let [session (str (UUID/randomUUID))
        query "Credentials.[AccessKeyId,SecretAccessKey,SessionToken]"
        result (shell/sh "aws" "sts" "assume-role"
                         ;; TODO possibly explicitly infer this
                         ;; "--profile" "default"
                         "--role-arn" role
                         "--role-session-name" session
                         "--duration-seconds" "900"
                         "--query" query
                         "--output" "text")
        {:keys [exit out err]} result
        xs (when (zero? exit)
             (string/split (string/trimr out) #"\t"))
        [access-key-id secret-access-key token] xs]
    (when-not (= 3 (count xs))
      (throw (ex-info "Invalid sts response" {:out out})))
    {"AWS_ACCESS_KEY_ID" access-key-id
     "AWS_SECRET_ACCESS_KEY" secret-access-key
     "AWS_SESSION_TOKEN" token
     "AWS_SECURITY_TOKEN" token
     ;; TODO possibly discover these from the outerlying env
     ;; "AWS_DEFAULT_PROFILE" "default"
     ;; "AWS_PROFILE" "default"
     }))

(defn -init []
  (let [role (System/getenv "AWS_CLI_MAVEN_ROLE")
        state (when role (obtain-role-env role))]
    [[] state]))

(defn -closeConnection
  [this])

(defn -openConnectionInternal
  [this])

(defn exec
  "Executes the given command in the environment context of the given wagon"
  [this & args]
  (let [env (.state this)
        args (cond-> (into [] args)
               (seq env)
               (into [:env env]))]
    (apply shell/sh args)))

(defn -get
  [this resource-name destination]
  (let [repository (.getRepository this)
        bucket (.getHost repository)
        root (.getBasedir repository)
        source-path (format "s3://%s%s%s" bucket root resource-name)
        destination-path (.getAbsolutePath destination)
        resource (Resource. resource-name)
        _ (.fireGetInitiated this resource destination)
        _ (.fireGetStarted this resource destination)
        result (exec this "aws" "s3" "cp" source-path destination-path)
        {:keys [exit out err]} result]
    (when (pos? exit)
      (let [ex (condp re-find err
                 #"Unable to locate credentials"
                 (AuthorizationException. err)
                 #"404"
                 (ResourceDoesNotExistException. err)
                 (TransferFailedException. err))]
        (throw ex)))
    (let [transfer-event (TransferEvent. this resource
                                         TransferEvent/TRANSFER_PROGRESS
                                         TransferEvent/REQUEST_GET)
          bytes (byte-array (.length destination))]
      (with-open [input (FileInputStream. destination)]
        (.read input bytes))
      (.fireTransferProgress this transfer-event bytes (count bytes)))
    (.fireGetCompleted this resource destination)))

(defn -getIfNewer
  [this resource-name destination timestamp]
  ;; TODO this should be easy with a pre-check using s3api head-object or maybe
  ;; even doing the whole thing with get-object
  (throw (Exception. "Not yet supported")))

(defn -resourceExists
  [this resource-name]
  (let [repository (.getRepository this)
        bucket (.getHost repository)
        root (.getBasedir repository)
        source-path (format "s3://%s%s%s" bucket root resource-name)
        result (exec this "aws" "s3" "ls" source-path)
        {:keys [exit out err]} result]
    (case exit
      0 true
      1 false
      255 (throw (AuthorizationException. err))
      (throw (TransferFailedException. err)))))

(defn -put
  [this source resource-name]
  (let [repository (.getRepository this)
        bucket (.getHost repository)
        root (.getBasedir repository)
        source-path (.getAbsolutePath source)
        destination-path (format "s3://%s%s%s" bucket root resource-name)
        resource (Resource. resource-name)
        _ (.firePutInitiated this resource source)
        _ (.firePutStarted this resource source)
        result (exec this "aws" "s3" "cp" source-path destination-path)
        {:keys [exit out err]} result]
    (when (pos? exit)
      (let [ex (condp re-find err
                 #"Unable to locate credentials"
                 (AuthorizationException. err)
                 #"404"
                 (ResourceDoesNotExistException. err)
                 (TransferFailedException. err))]
        (throw ex)))
    (let [transfer-event (TransferEvent. this resource
                                         TransferEvent/TRANSFER_PROGRESS
                                         TransferEvent/REQUEST_PUT)
          bytes (byte-array (.length source))]
      (with-open [input (FileInputStream. source)]
        (.read input bytes))
      (.fireTransferProgress this transfer-event bytes (count bytes)))
    (.firePutCompleted this resource source)))
