package nl.vu.ai.lsde.enron.sentimentresumer

import java.sql.Date

import nl.vu.ai.lsde.enron.{Commons, EmailWithSentiment}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext, functions}
import org.apache.spark.{SparkConf, SparkContext}



/**
  * This driver program is responsible to compute the overall
  * sentiment on a user-defined time-unit basis (e.g. 1 day)
  */
object SentimentResumerTest {

    val appName = "ENRON-sentiment-resumer"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    def main(args: Array[String]): Unit = {

        // LOADING SENTIMENT DATA
        val dfSentiment = sqlContext.read.parquet(Commons.ENRON_SENTIMENT_DATAFRAME)

        val allEmails = dfSentiment
            .explode("emails", "email") {emails: Seq[EmailWithSentiment] => emails}
            .drop("emails")

        // TODO: some dates have weird formats e.g. 0002-11-30
        val allEmailsNorm = allEmails
            .withColumn("mailbox", allEmails("name"))
            .withColumn("date", functions.to_date(allEmails("email.date")))
            .withColumn("from", allEmails("email.from"))
            .withColumn("to", allEmails("email.to"))
            .withColumn("subject", allEmails("email.subject"))
            .withColumn("sentiment", allEmails("email.sentiment"))
            .drop("email")

        allEmailsNorm.show()

        // TODO: many weird dates are being filtered!
        val sentimentPerDay = allEmailsNorm
            .where($"date" >= Date.valueOf("1997-01-01") && $"date" <= Date.valueOf("2003-12-31"))
            .groupBy("date")
            .avg("sentiment")
            .sort("date")
            .withColumnRenamed("avg(sentiment)","sentiment")

        sentimentPerDay.show()

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

        enronStock.show()

        // JOIN ALL AND WRITE
//        val output = sentimentPerDay.join(enronStock, sentimentPerDay("date") === enronStock("date_new"), "outer")
        val output = sentimentPerDay.join(enronStock, Seq("date"), "outer")

        output.show(5000)

        Commons.deleteFolder(Commons.ENRON_SENTIMENT_RESUME_JSON)
        asSingleJSON(output).saveAsTextFile(Commons.ENRON_SENTIMENT_RESUME_JSON)


        // produce a JSON file for each mailbox
        val mailBoxesNames = dfSentiment.select("name").map(r => r.getString(0)).collect()
        val aggregatedMailboxes = mailBoxesNames.map(resumeMailbox(_, allEmailsNorm))

        aggregatedMailboxes.map { resumed =>
            (resumed._1, resumed._2.join(enronStock, Seq("date"), "outer"))
        }.foreach { resumed =>
          val filename = Commons.ENRON_SENTIMENT_RESUME_MB_JSON.format(resumed._1)
          Commons.deleteFolder(filename)
          asSingleJSON(resumed._2).saveAsTextFile(filename)
        }
    }


    def resumeMailbox(mailboxName: String, dfSentiment: DataFrame): (String, DataFrame) = {
        val df = dfSentiment.where($"mailbox" === mailboxName)

        val dfPerDay = df.where($"date" >= Date.valueOf("1997-01-01") && $"date" <= Date.valueOf("2003-12-31"))
            .groupBy("date").avg("sentiment").sort("date").withColumnRenamed("avg(sentiment)","sentiment")


        (mailboxName, dfPerDay)
    }


    private def asSingleJSON(df: DataFrame): RDD[String] =
        sc.parallelize(Seq(df.toJSON.collect().mkString("[", ",", "]"))).repartition(1)


}
