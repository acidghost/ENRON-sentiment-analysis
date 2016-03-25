# ENRON sentiment analysis
###### Large Scale Data Engineering assignment - VU MSc AI

### Web view
[http://acidghost.github.io/ENRON-sentiment-analysis/visualization/](http://acidghost.github.io/ENRON-sentiment-analysis/visualization/)

### Setup
1. `git clone https://github.com/acidghost/ENRON-sentiment-analysis.git`  
2. `git submodule update --depth 1 --init --recursive`  
3. `./hathi-client/bin/get.sh hadoop`  
4. `./hathi-client/bin/get.sh spark`
5. Install the [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html)

### Usage
1. `eval $(./hathi-client/bin/env.sh)`
2. `kinit USRNAME`

#### Dependencies
Because CoreNLP models are huge (~200MB), they are marked as `provided` in the build.
To download the JAR type `wget https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/3.4.1/stanford-corenlp-3.4.1-models.jar`
and then upload it to the cluster with `hdfs dfs -put ./stanford-corenlp-3.4.1-models.jar hdfs:///user/lsde03/enron/jars/`.
Then don't forget to add it to the job's classpath with the option `spark-submit --jars hdfs:///user/lsde03/enron/jars/stanford-corenlp-3.4.1-models.jar ...`.
Because the cluster only supports Java 7 we are forced to use CoreNLP version 3.4.1 because later versions only support Java8.

#### Launching jobs
Package fat JARs using `./bin/assembly.sh`.
All those scripts assume that you are in the "cluster environment" and logged in via kerberos.
- Unzipper: using `./bin/deploy_unzipper.sh`.
- ETL: using `./bin/deploy_etl.sh`.


#### Notes
```enron_stock_prices.csv``` source: http://www.gilardi.com/pdf/enro13ptable.pdf
