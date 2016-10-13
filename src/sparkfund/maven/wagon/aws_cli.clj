(ns sparkfund.maven.wagon.aws-cli
  (:require [clojure.java.shell :as shell])
  (:import
   [java.io FileInputStream]
   [org.apache.maven.wagon.resource Resource]
   [org.apache.maven.wagon.events TransferEvent]
   [org.apache.maven.wagon ResourceDoesNotExistException TransferFailedException]
   [org.apache.maven.wagon.authorization AuthorizationException])
  (:gen-class
   :extends org.apache.maven.wagon.AbstractWagon
   :init init))

(defn -init []
  [[]])

(defn -closeConnection
  [this])

(defn -openConnectionInternal
  [this])

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
        result (shell/sh "aws" "s3" "cp" source-path destination-path)
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
        result (shell/sh "aws" "s3" "cp" source-path destination-path)
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
