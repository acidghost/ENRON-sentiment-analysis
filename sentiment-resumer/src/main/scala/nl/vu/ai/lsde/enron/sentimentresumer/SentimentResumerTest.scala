package nl.vu.ai.lsde.enron.sentimentresumer

import java.sql.{Timestamp, Date}

import nl.vu.ai.lsde.enron.{Commons, EmailWithSentiment}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{SQLContext, SaveMode, functions}
import org.apache.spark.{SparkConf, SparkContext}



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
         
        val csv = sqlContext.read
            .format("com.databricks.spark.csv")
            .option("header","true")
            .option("inferSchema","true")
            .load("/user/lsde03/enron/enron_stock_prices.csv")

        val enronStock = csv
            .withColumn("date2", csv("date").cast("Date"))
            .drop("date")

        sentimentPerDay.repartition(1).write.mode(SaveMode.Overwrite).json(Commons.ENRON_SENTIMENT_RESUME_JSON)
    }
}
