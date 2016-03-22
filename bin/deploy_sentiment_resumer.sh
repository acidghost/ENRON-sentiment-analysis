#!/usr/bin/env bash

class=nl.vu.ai.lsde.enron.sentimentresumer.SentimentResumerTest
driver_jar=./sentiment-resumer/target/scala-2.10/sentiment-resumer.jar

set -x
spark-submit \
    --master yarn \
    --deploy-mode cluster \
    --num-executors 8 \
	--executor-memory 12g \
	--driver-memory 12g \
	--executor-cores 4 \
	--driver-cores 4 \
    --class ${class} ${driver_jar}
set +x
