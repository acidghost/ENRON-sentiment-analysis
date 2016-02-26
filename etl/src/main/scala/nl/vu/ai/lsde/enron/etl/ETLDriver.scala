package nl.vu.ai.lsde.enron.etl

import java.util.Properties

import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import nl.vu.ai.lsde.enron.etl.EmailParser.EmailParsingException
import nl.vu.ai.lsde.enron.{Commons, EmailWithSentiment, MailBox, MailBoxWithSentiment}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}

object ETLDriver {

    val appName = "ENRON-etl"
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)

    def main (args: Array[String]) {
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT)

        // get custodians from csv file
        // FIXME this doesn't work on the cluster (- object not serializable (class: scala.io.BufferedSource
        val custodians = sc.broadcast(Commons.getCustodians)
   
        val allParsed: RDD[MailBox] = allExtracted.map { case (mailbox, emails) =>
            val parsedEmails = emails flatMap { email =>
                try Some(EmailParser.parse(email, custodians.value))
                catch {
                    case e: EmailParsingException => None
                }
            }

            MailBox(mailbox, parsedEmails)
        }


        // load sentiment annotator pipeline
        val nlpProps = new Properties
        nlpProps.setProperty("annotators", "sentiment")
        val pipeline = new StanfordCoreNLP(nlpProps)
        // classify sentiment and save w/o body
        val mailboxesSentiment = allParsed.map { mailbox =>
            val emailsWithSentiment = mailbox.emails.map { email =>
                val document = new Annotation(email.body)
                pipeline.annotate(document)
                val sentiment = document.get[String](classOf[SentimentCoreAnnotations.ClassName])
                EmailWithSentiment(email.date, email.from, email.to ++ email.cc ++ email.bcc, email.subject, sentiment)
            }
            MailBoxWithSentiment(mailbox.name, emailsWithSentiment)
        }


        val sqlContext = new SQLContext(sc)
        import sqlContext.implicits._

        val dfFull = allParsed.toDF()
        dfFull.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_FULL_DATAFRAME)

        val dfSentiment = mailboxesSentiment.toDF()
        dfSentiment.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_SENTIMENT_DATAFRAME)
    }

}
