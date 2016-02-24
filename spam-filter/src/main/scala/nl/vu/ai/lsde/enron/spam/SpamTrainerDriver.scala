package nl.vu.ai.lsde.enron.spam

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object SpamTrainerDriver extends App {

    val appName = "ENRON-spam-trainer"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val tf = new HashingTF()

    val spamRaw: RDD[String] = sc.wholeTextFiles(s"${Commons.ENRON_SPAM_DATA}/enron*/spam/*.txt").values
    val spamTokenized: RDD[Seq[String]] = spamRaw map Tokenizer.tokenize
    val spamVectors: RDD[Vector] = tf transform spamTokenized

    val hamRaw: RDD[String] = sc.wholeTextFiles(s"${Commons.ENRON_SPAM_DATA}/enron*/ham/*.txt").values
    val hamTokenized: RDD[Seq[String]] = hamRaw map Tokenizer.tokenize
    val hamVectors: RDD[Vector] = tf transform hamTokenized

    spamVectors.cache()
    hamVectors.cache()

    val idf = new IDF() fit spamVectors ++ hamVectors
    val spamTfidf: RDD[Vector] = idf transform spamVectors
    val hamTfidf: RDD[Vector] = idf transform hamVectors

    Commons.deleteFolder(Commons.ENRON_SPAM_TF)
    sc.parallelize(Seq(tf)).saveAsObjectFile(Commons.ENRON_SPAM_TF)
    Commons.deleteFolder(Commons.ENRON_SPAM_IDF)
    sc.parallelize(Seq(idf)).saveAsObjectFile(Commons.ENRON_SPAM_IDF)

    val spamLabeled = spamTfidf map { v => LabeledPoint(0.0, v) }
    val hamLabeled = hamTfidf map { v => LabeledPoint(1.0, v) }
    val wholeDataset = spamLabeled ++ hamLabeled

    val splits = wholeDataset.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0)
    val test = splits(1)

    val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

    val collectedTest = test.collect()
    val predictionAndLabel = collectedTest.map(p => (model.predict(p.features), p.label))
    val accuracy = 1.0 * predictionAndLabel.count(x => x._1 == x._2) / collectedTest.length

    println(s"\n\n\n\nAccuracy for spam model is: $accuracy\n\n\n\n")

    Commons.deleteFolder(Commons.ENRON_SPAM_MODEL)
    model.save(sc, Commons.ENRON_SPAM_MODEL)

}
