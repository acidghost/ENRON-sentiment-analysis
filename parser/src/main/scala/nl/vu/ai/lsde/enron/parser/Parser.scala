package nl.vu.ai.lsde.enron.parser

import java.io.ByteArrayInputStream
import java.util.{Date, Properties}
import javax.mail.{Message, Session}
import javax.mail.internet.MimeMessage

import nl.vu.ai.lsde.enron.Commons
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class Parser extends App {

    case class Email(date: Date, from: Seq[String], to: Seq[String], cc: Seq[String], bcc: Seq[String], subject: String, body: String)

    val filesDir = "hdfs:///user/lsde03/enron/extracted_txt"
    val appName = "ENRON-parser"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val scalaParser = new StandardTokenParsers()
    scalaParser.lexical.delimiters ++= List("\n")
    scalaParser.lexical.reserved ++= List("Date:", "From:", "To:", "Cc:", "Subject:")

    val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)

    val allParsed = allExtracted.map { case (mailbox, emails) =>
        val parsedEmails = emails map parse
        (mailbox, parsedEmails)
    }

    // parse text and returns Email obj
    def parse(text: String): Email = {

        val session = Session.getDefaultInstance(new Properties())
        val message = new MimeMessage(session, new ByteArrayInputStream(text.getBytes))

        // there's also a received date!
        val date = message.getSentDate
        val from = message.getFrom.map(_.toString)
        val to = message.getRecipients(Message.RecipientType.TO).map(_.toString)
        val cc = message.getRecipients(Message.RecipientType.CC).map(_.toString)
        val bcc = message.getRecipients(Message.RecipientType.CC).map(_.toString)
        val subject = message.getSubject
        val body = message.getContent.toString

        Email(date, from, to, cc, bcc, subject, body)
    }

    //  insert SparkSQL table and returns success code
    def pushEntry(entry: Email): Int = {
        1
    }

}

// run with:
// scala-2.11 -cp unzipper/target/scala-2.10/unzipper_2.10-1.0.0.jar nl.vu.ai.lsde.enron.unzipper.Test
object Test extends App {

    // scalastyle:off line.size.limit
    var test = "Date: Wed, 7 Feb 2001 14:11:00 -0800 (PST)\nFrom: Harry Arora\nSubject: Allan Sommer\nX-SDOC: 528063\nX-ZLID: zl-edrm-enron-v2-arora-h-914.eml\n\n\n\n***********\nEDRM Enron Email Data Set has been produced in EML, PST and NSF format by ZL Technologies, Inc. This Data Set is licensed under a Creative Commons Attribution 3.0 United States License <http://creativecommons.org/licenses/by/3.0/us/> . To provide attribution, please cite to \"ZL Technologies, Inc. (http://www.zlti.com).\"\n***********"
    // scalastyle:on line.size.limit
    var p = new Parser()
    p.parse(test)
}
