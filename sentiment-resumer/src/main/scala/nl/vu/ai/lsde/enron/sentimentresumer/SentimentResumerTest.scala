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

    // scalastyle:off method.length
    def main (args: Array[String]): Unit = {
        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        // LOADING SENTIMENT DATA
        val dfSentiment = sqlContext.read.parquet(Commons.ENRON_SENTIMENT_DATAFRAME)

        val allEmails = dfSentiment
            .explode("emails", "email") {emails: Seq[EmailWithSentiment] => emails}
            .drop("emails")

        // TODO: some dates have weird formats e.g. 0002-11-30
        val allEmailsNorm = allEmails
            .withColumn("date", functions.to_date(allEmails("email.date")))
            .withColumn("from", allEmails("email.from"))
            .withColumn("to", allEmails("email.to"))
            .withColumn("subject", allEmails("email.subject"))
            .withColumn("sentiment", allEmails("email.sentiment"))
            .drop("email")

        allEmailsNorm.show

        // TODO: many weird dates are being filtered!
        val sentimentPerDay = allEmailsNorm
            .where($"date" >= Date.valueOf("1997-01-01") && $"date" <= Date.valueOf("2003-12-31"))
            .groupBy("date")
            .avg("sentiment")
            .sort("date")
            .withColumnRenamed("avg(sentiment)","sentiment")

        sentimentPerDay.show

        // LOADING ENTRON STOCK DATA
        val csv = sqlContext.read
            .format("com.databricks.spark.csv")
            .option("header","true")
            .option("inferSchema","true")
            .load(Commons.ENRON_STOCK_PRICES_CSV)

        val enronStock = csv
            .withColumn("date_2", csv("date").cast("Date"))
            .drop("date")
            .drop("volume")
            .withColumnRenamed("date_2", "date")

        enronStock.show

        // JOIN ALL AND WRITE
//        val output = sentimentPerDay.join(enronStock, sentimentPerDay("date") === enronStock("date_new"), "outer")
        val output = sentimentPerDay.join(enronStock, Seq("date"), "outer")

        output.show(5000)

        Commons.deleteFolder(Commons.ENRON_SENTIMENT_RESUME_JSON)
        sc.parallelize(Seq(output.toJSON.collect().mkString("[", ",", "]"))).repartition(1).saveAsTextFile(Commons.ENRON_SENTIMENT_RESUME_JSON)
    }
}
