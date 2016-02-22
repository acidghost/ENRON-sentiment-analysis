package nl.vu.ai.lsde.enron.parser

import nl.vu.ai.lsde.enron.{Commons, MailBox}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SaveMode, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}

object Parser extends App {

    val appName = "ENRON-parser"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)

    val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
        val parsedEmails = emails map EmailParser.parse
        MailBox(mailbox, parsedEmails)
    }


    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    val df = allParsed.toDF()
    df.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_DATAFRAME)

}

// run with:
// scala-2.11 -cp parser/target/scala-2.10/parser.jar nl.vu.ai.lsde.enron.parser.Test
object Test extends App {

    // scalastyle:off line.size.limit
    var test1 = "Date: Wed, 7 Feb 2001 14:11:00 -0800 (PST)\nFrom: Harry Arora\nTo: Enron\nSubject: Allan Sommer\nX-SDOC: 528063\nX-ZLID: zl-edrm-enron-v2-arora-h-914.eml\n\nHello, email!\n\n***********\nEDRM Enron Email Data Set has been produced in EML, PST and NSF format by ZL Technologies, Inc. This Data Set is licensed under a Creative Commons Attribution 3.0 United States License <http://creativecommons.org/licenses/by/3.0/us/> . To provide attribution, please cite to \"ZL Technologies, Inc. (http://www.zlti.com).\"\n***********"
    var test2 = "Date: Tue, 29 Jan 2002 14:45:09 -0800 (PST)\nSubject: Notice to Employees of Bankruptcy Court Order\nFrom: Legal - James Derrick Jr. <mbx_annclegal@ENRON.com>\nTo: DL-GA-all_enron_worldwide3 <DL-GA-all_enron_worldwide3@ENRON.com>\nX-SDOC: 105986\nX-ZLID: zl-edrm-enron-v2-townsend-j-127.eml\n\nThis is to inform you that on January 25, 2002 the Honorable Arthur Gonzalez, United States Bankruptcy Judge for the Southern District of New York, entered an order as follows:\n\nIt is hereby Ordered that until further Order of this Court, Enron Corp., its affiliated debtors-in-possession in these jointly administered chapter 11 proceedings (collectively \"Enron\"), and Enron's employees shall preserve, and refrain from destroying or disposing of, any of Enron's records, either in electronic or paper form.\n\nObviously, any violation of this order will carry serious consequences.  We know that we can depend on you to comply with it fully.\n\nThank you.\n\n***********\nEDRM Enron Email Data Set has been produced in EML, PST and NSF format by ZL Technologies, Inc. This Data Set is licensed under a Creative Commons Attribution 3.0 United States License <http://creativecommons.org/licenses/by/3.0/us/> . To provide attribution, please cite to \"ZL Technologies, Inc. (http://www.zlti.com).\"\n***********"
    // scalastyle:on line.size.limit
    val email = EmailParser.parse(test2)

    println(email)
}
