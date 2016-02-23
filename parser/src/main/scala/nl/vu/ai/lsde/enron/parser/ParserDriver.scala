package nl.vu.ai.lsde.enron.parser

import nl.vu.ai.lsde.enron.parser.EmailParser.EmailParsingException
import nl.vu.ai.lsde.enron.{Commons, MailBox}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}

object ParserDriver extends App {

    val appName = "ENRON-parser"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)

    val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
        val parsedEmails = emails flatMap { email =>
            try Some(EmailParser.parse(email))
            catch { case e: EmailParsingException => None }
        }

        MailBox(mailbox, parsedEmails)
    }


    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    val df = allParsed.toDF()
    df.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_DATAFRAME)

}
