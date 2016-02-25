package nl.vu.ai.lsde.enron.spam

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


/**
  *  This program is responsible to train a Naive Bayes model for spam
  *  classification using the ENRON-spam corpus. It also saves on disk
  *  the HashingTF, IDF and trained models for future use.
  */
object SpamTrainerDriver extends App {

    val appName = "ENRON-spam-trainer"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val tf = new HashingTF()

    // load, tokenize and extract term frequencies from spam documents
    val spamRaw: RDD[String] = sc.wholeTextFiles(s"${Commons.ENRON_SPAM_DATA}/enron*/spam/*.txt").values
    val spamTokenized: RDD[Seq[String]] = spamRaw map Tokenizer.tokenize
    val spamVectors: RDD[Vector] = tf transform spamTokenized

    // load, tokenize and extract term frequencies from ham documents
    val hamRaw: RDD[String] = sc.wholeTextFiles(s"${Commons.ENRON_SPAM_DATA}/enron*/ham/*.txt").values
    val hamTokenized: RDD[Seq[String]] = hamRaw map Tokenizer.tokenize
    val hamVectors: RDD[Vector] = tf transform hamTokenized

    spamVectors.cache()
    hamVectors.cache()

    // compute TF-IDF features
    val idf = new IDF() fit spamVectors ++ hamVectors
    val spamTfidf: RDD[Vector] = idf transform spamVectors
    val hamTfidf: RDD[Vector] = idf transform hamVectors

    // store HashingTF model on disk
    Commons.deleteFolder(Commons.ENRON_SPAM_TF)
    sc.parallelize(Seq(tf)).saveAsObjectFile(Commons.ENRON_SPAM_TF)
    // store IDFModel on disk
    Commons.deleteFolder(Commons.ENRON_SPAM_IDF)
    sc.parallelize(Seq(idf)).saveAsObjectFile(Commons.ENRON_SPAM_IDF)

    // convert dataset to labeled points
    val spamLabeled = spamTfidf map { v => LabeledPoint(0.0, v) }
    val hamLabeled = hamTfidf map { v => LabeledPoint(1.0, v) }
    val wholeDataset = spamLabeled ++ hamLabeled

    // split train and test sets
    val splits = wholeDataset.randomSplit(Array(0.6, 0.4), seed = 11L)
    val training = splits(0)
    val test = splits(1)

    // train NB multinomial model
    val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

    // compute accuracy on test set
    val collectedTest = test.collect()
    val predictionAndLabel = collectedTest.map(p => (model.predict(p.features), p.label))
    val accuracy = 1.0 * predictionAndLabel.count(x => x._1 == x._2) / collectedTest.length

    println(s"\n\n\n\nAccuracy for spam model is: $accuracy\n\n\n\n")

    // save trained NB model on disk
    Commons.deleteFolder(Commons.ENRON_SPAM_MODEL)
    model.save(sc, Commons.ENRON_SPAM_MODEL)

}
