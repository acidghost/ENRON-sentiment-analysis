#!/usr/bin/env bash

class=nl.vu.ai.lsde.enron.etl.ETLDriver
jars=hdfs:///user/lsde03/enron/jars/etl-deps.jar,hdfs:///user/lsde03/enron/jars/stanford-corenlp-3.4.1-models.jar
driver_jar=./etl/target/scala-2.10/etl.jar

set -x
spark-submit \
	--master yarn \
	--deploy-mode cluster \
	--num-executors 24 \
	--class ${class} \
	--jars ${jars} ${driver_jar}  
set +x
