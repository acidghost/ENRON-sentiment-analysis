#!/usr/bin/env bash

class=nl.vu.ai.lsde.enron.unzipper.UnzipperDriver
driver_jar=./unzipper/target/scala-2.10/unzipper.zip

set -x
spark-submit --master yarn  --deploy-mode cluster --class ${class} ${driver_jar}
set +x
