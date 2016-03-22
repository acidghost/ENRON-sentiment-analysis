package nl.vu.ai.lsde.enron.sentimentresumer

import java.sql.{Date}

import nl.vu.ai.lsde.enron.{EmailWithSentiment, Commons}
import org.apache.spark.sql.{SaveMode, SQLContext, functions}
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


        val allEmails = dfSentiment
            .explode("emails", "email") {emails: Seq[EmailWithSentiment] => emails}
            .drop("emails")

        val allEmailsNorm = allEmails
            .withColumn("date", functions.to_date(allEmails("email.date"))) //TODO: some dates have weird formats e.g. 0002-11-30
            .withColumn("from", allEmails("email.from"))
            .withColumn("to", allEmails("email.to"))
            .withColumn("subject", allEmails("email.subject"))
            .withColumn("sentiment", allEmails("email.sentiment"))
            .drop("email")

        allEmailsNorm.show
        allEmailsNorm.printSchema


        val sentimentPerDay = allEmailsNorm
            .groupBy("date")
            .avg("sentiment")
            .where($"date" >= Date.valueOf("1997-01-01") && $"date" <= Date.valueOf("2015-12-25")) //TODO: there are too many weird dates filtered out!
            .sort("date")

        sentimentPerDay.show(5000)
        sentimentPerDay.printSchema()

        sentimentPerDay.write.mode(SaveMode.Overwrite).json(Commons.ENRON_SENTIMENT_RESUME_JSON)
    }
}
