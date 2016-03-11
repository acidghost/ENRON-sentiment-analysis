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
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryoserializer.buffer.max", "512m")
    conf.set("spark.driver.maxResultSize", "0")
    conf.registerKryoClasses(Array(classOf[Properties], classOf[StanfordCoreNLP]))
    val sc = new SparkContext(conf)

    def main(args: Array[String]): Unit = {

        import org.apache.log4j.{Level, Logger}
        val level = Level.WARN
        Logger.getLogger("org").setLevel(level)
//        Logger.getLogger("akka").setLevel(level)

        // Testing on a sub-sample
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT).take(1)

        // get custodians from csv file stored in HDFS
        val csv = sc.textFile(Commons.ENRON_CUSTODIANS_CSV_HDFS).map { line => line.split(",") }
        var custodians = sc.broadcast(csv.map { record => Custodian(record(0), record(1), Option(record(2))) }.collect().toSeq)

        // parse emails
        val allParsed: Array[MailBox] = allExtracted.map { case (mailbox, emails) =>
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

        val dfFull = sc.parallelize(allParsed).toDF()

        println("\n\n\n*******************")
        println("dfFull")
        println("*******************")
        dfFull.printSchema()
        println("*******************")
        dfFull.show()

        dfFull.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_FULL_DATAFRAME)

        // clean mem
        dfFull.unpersist(true)
        custodians.destroy()


        // load sentiment annotator pipeline
        val nlpProps = new Properties
        nlpProps.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma, sentiment")
        val pipeline = new StanfordCoreNLP(nlpProps)

        sc.broadcast(nlpProps)
        sc.broadcast(pipeline)

        // classify sentiment and save w/o body
        val mailboxesSentiment = allParsed.map { mailbox =>
            // annotation
            val emailsWithSentiment = mailbox.emails.map { email =>
                val document = new Annotation(email.body)
                pipeline.annotate(document)
                val sentiment = document.get[String](classOf[SentimentCoreAnnotations.ClassName])
                EmailWithSentiment(email.date, email.from, email.to ++ email.cc ++ email.bcc, email.subject, sentiment)
            }

            MailBoxWithSentiment(mailbox.name, emailsWithSentiment)
        }

        val dfSentiment = sc.parallelize(mailboxesSentiment).toDF()

        println("\n\n\n*******************")
        println("dfSentiment")
        println("*******************")
        dfSentiment.printSchema()
        println("*******************")
        dfSentiment.show()

        dfSentiment.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_SENTIMENT_DATAFRAME)

    }

}
