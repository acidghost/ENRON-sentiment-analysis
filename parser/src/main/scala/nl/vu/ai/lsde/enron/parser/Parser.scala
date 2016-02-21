package nl.vu.ai.lsde.enron.parser

import nl.vu.ai.lsde.enron.{Commons, Email, MailBox}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

object Parser extends App {

    val appName = "ENRON-parser"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)

    val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
        val parsedEmails = emails map parse
        MailBox(mailbox, parsedEmails)
    }

    val df = allParsed.toDF()
    df.write.parquet(Commons.ENRON_DATAFRAME)

    // parse text and returns Email obj
    def parse(text: String): Email = {
        val (headers, body) = text.split("\n\n") match {
            case s => (s.head, s.tail.mkString("\n\n"))
        }

        val bodyNoFooter = body.split("\n\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*\\*") match { case s => s.head }

        EmailHeadersParser.parse(headers) match {
            case Email(date, from, to, cc, bcc, subject, _) => Email(date, from, to, cc, bcc, subject, bodyNoFooter)
        }
    }

}

// run with:
// scala-2.11 -cp parser/target/scala-2.10/parser.jar nl.vu.ai.lsde.enron.parser.Test
object Test extends App {

    // scalastyle:off line.size.limit
    var test = "Date: Wed, 7 Feb 2001 14:11:00 -0800 (PST)\nFrom: Harry Arora\nTo: Enron\nSubject: Allan Sommer\nX-SDOC: 528063\nX-ZLID: zl-edrm-enron-v2-arora-h-914.eml\n\nHello, email!\n\n***********\nEDRM Enron Email Data Set has been produced in EML, PST and NSF format by ZL Technologies, Inc. This Data Set is licensed under a Creative Commons Attribution 3.0 United States License <http://creativecommons.org/licenses/by/3.0/us/> . To provide attribution, please cite to \"ZL Technologies, Inc. (http://www.zlti.com).\"\n***********"
    // scalastyle:on line.size.limit
    val email = Parser.parse(test)

    println(email)
}
