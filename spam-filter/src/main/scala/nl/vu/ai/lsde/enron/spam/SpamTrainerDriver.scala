package nl.vu.ai.lsde.enron.spam

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}



object SpamTrainerDriver extends App {

    val appName = "ENRON-spam-trainer"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val spamRaw: RDD[String] = sc.wholeTextFiles(s"${Commons.ENRON_SPAM_DATA}/enron*/spam/*.txt").values
    val spamTokenized: RDD[Seq[String]] = spamRaw map Tokenizer.tokenize

}
