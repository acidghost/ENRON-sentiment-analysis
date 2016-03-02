package nl.vu.ai.lsde.enron.sentimentresumer

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}


/**
  * This driver program is responsible to compute the overall
  * sentiment on a user-defined time-unit basis (e.g. 1 day)
  */
object SentimentResumerTest {

    val appName = "ENRON-sentiment-resumer"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    def main (args: Array[String]): Unit = {
        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        // load sentiment dataframe
        val dfSentiment = sqlContext.read.parquet(Commons.ENRON_SENTIMENT_DATAFRAME)

        dfSentiment.registerTempTable("df")

        //TODO for each day compute overall sentiment

        //TODO Save the timeseries as CSV

    }
}
