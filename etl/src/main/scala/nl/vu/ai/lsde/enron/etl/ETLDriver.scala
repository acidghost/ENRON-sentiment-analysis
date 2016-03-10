package nl.vu.ai.lsde.enron.etl

import java.util.Properties

import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import nl.vu.ai.lsde.enron.etl.EmailParser.EmailParsingException
import nl.vu.ai.lsde.enron._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}


object ETLDriver {

    val appName = "ENRON-etl"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    def main(args: Array[String]): Unit = {
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)
        // Testing on a sub-sample
        //        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT).sample(false, 0.01, 42)

        // get custodians from csv file stored in HDFS
        val csv = sc.textFile(Commons.ENRON_CUSTODIANS_CSV_HDFS).map { line => line.split(",") }
        var custodians = sc.broadcast(csv.map { record => Custodian(record(0), record(1), Option(record(2))) }.collect().toSeq)

        // parse emails
        val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
            val parsedEmails = emails flatMap { email =>
                try Some(EmailParser.parse(email, custodians.value))
                catch {
                    case e: EmailParsingException => None
                }
            }

            MailBox(mailbox, parsedEmails)
        }

        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        val dfFull = allParsed.toDF()
        dfFull.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_FULL_DATAFRAME)
        dfFull.unpersist()
        allExtracted.unpersist()

        // classify sentiment and save w/o body
        val mailboxesSentiment = allParsed.map { mailbox =>
            // annotation
            val emailsWithSentiment = mailbox.emails.map { email =>
                // load sentiment annotator pipeline
                val nlpProps = new Properties
                nlpProps.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma, sentiment")
                val pipeline = new StanfordCoreNLP(nlpProps)

                val document = new Annotation(email.body)
                pipeline.annotate(document)
                val sentiment = document.get[String](classOf[SentimentCoreAnnotations.ClassName])
                EmailWithSentiment(email.date, email.from, email.to ++ email.cc ++ email.bcc, email.subject, sentiment)
            }

            MailBoxWithSentiment(mailbox.name, emailsWithSentiment)
        }

        val dfSentiment = mailboxesSentiment.toDF()
        dfSentiment.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_SENTIMENT_DATAFRAME)
    }

}
