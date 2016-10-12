# AWS CLI Wagon

This project is a Maven Wagon for Amazon S3, particularly for private S3 buckets. It differs from the existing S3 private wagons in that it uses the AWS cli commands instead of any of the Java libraries.

## Usage

For boot:

``` clojure
(set-env!
 :wagons '[[sparkfund/aws-cli-wagon "1.0.0"]]
 :repositories
 #(conj % '["private {:url "s3p://bucket-name/releases/"}]))
```

## Motivation

Dependency hell is real. Both leiningen and boot run their plugins in Java contexts that provide certain underlying dependencies, specifically Jackson. This constraints the versions of the Amazon Java SDK that they may use, as they have hard dependencies on Jackson as well. Using certain features of AWS, specifically STS, requires versions of the Amazon Java SDK that are not necessarily compatible with leiningen and boot. Moreover, there are more ways to credential an AWS session than are dreamt of in any of their philosophies.

Rather than continue to play whack-a-mole trying to resolve version mismatches, this library takes and end run around the whole mess. We assume that the AWS CLI has access to credentials somehow (e.g. credentials file or environmental variables) and simply run its commands to implement the wagon api.

## Limitations

This doesn't yet support get-if-newer or putting resources. We anticipate supporting them shortly.
