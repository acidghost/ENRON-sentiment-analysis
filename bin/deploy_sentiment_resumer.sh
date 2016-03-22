#!/usr/bin/env bash

class=nl.vu.ai.lsde.enron.sentimentresumer.SentimentResumerTest
driver_jar=./sentiment-resumer/target/scala-2.10/sentiment-resumer.jar

set -x
spark-submit \
    --master yarn \
    --deploy-mode cluster \
    --packages com.databricks:spark-csv_2.11:1.2.0 \
    --verbose \
    --num-executors 12 \
	--executor-memory 8g \
	--driver-memory 8g \
	--executor-cores 4 \
	--driver-cores 4 \
    --class ${class} ${driver_jar}
set +x
