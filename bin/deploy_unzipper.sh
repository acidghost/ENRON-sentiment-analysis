#!/usr/bin/env bash

class=nl.vu.ai.lsde.enron.unzipper.UnzipperDriver
driver_jar=./unzipper/target/scala-2.10/unzipper.jar

set -x
spark-submit \
    --master yarn \
    --deploy-mode cluster \
    --verbose \
    --num-executors 24 \
    --executor-memory 6g \
    --driver-memory 6g \
    --executor-cores 4 \
    --driver-cores 4 \
    --class ${class} ${driver_jar}
set +x
