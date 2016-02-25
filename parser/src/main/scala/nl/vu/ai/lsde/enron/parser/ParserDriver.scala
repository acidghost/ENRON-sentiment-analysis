package nl.vu.ai.lsde.enron.parser

import nl.vu.ai.lsde.enron.parser.EmailParser.EmailParsingException
import nl.vu.ai.lsde.enron.{Custodian, Commons, MailBox}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}

object ParserDriver {

    val appName = "ENRON-parser"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    // scalastyle:off
    def main (args: Array[String]) {
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)

        // get custodians from csv file
        val csv = sc.textFile(Commons.ENRON_CUSTODIANS_CSV).map{line => line.split(",")}
        var custodians = csv.map{record => Custodian(record(0),record(1),Option(record(2)))}.collect().toSeq
   
        val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
            val parsedEmails = emails flatMap { email =>
                try Some(EmailParser.parse(email))
                catch {
                    case e: EmailParsingException => None
                }
            }

            MailBox(mailbox, parsedEmails)
        }


        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        val df = allParsed.toDF()
        df.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_DATAFRAME)
    }

}
