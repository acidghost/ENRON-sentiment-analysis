package nl.vu.ai.lsde.enron.unzipper

import org.apache.spark.{SparkConf, SparkContext}

import scala.util.parsing.combinator.syntactical.StandardTokenParsers

class Parser extends App {

    case class Email(date: String, from: String, to: String, cc: Option[String], subject: String, body: String)

    val filesDir = "hdfs:///user/lsde03/enron/extracted_txt"
    val appName = "ENRON-parser"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val scalaParser = new StandardTokenParsers()
    scalaParser.lexical.delimiters ++= List("\n")
    scalaParser.lexical.reserved ++= List("Date:", "From:", "To:", "Cc:", "Subject:")

    sc.wholeTextFiles(filesDir).foreach { case (fileName, fileContent) =>

        parse(fileContent)
    }

    // parse text and returns Email obj
    def parse(text: String): Email = {
        // var scanner = new scalaParser.lexical.Scanner(text)
        // var email = Email
        // TODO
        Email("date", "from", "to", Some("cc"), "subj", "body")
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
