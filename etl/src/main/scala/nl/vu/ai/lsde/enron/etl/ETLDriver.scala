package nl.vu.ai.lsde.enron.etl

import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import nl.vu.ai.lsde.enron._
import nl.vu.ai.lsde.enron.etl.EmailParser.EmailParsingException
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConversions._


object ETLDriver {

    val appName = "ENRON-etl"
    val conf = new SparkConf().setAppName(appName)
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryoserializer.buffer.max", "512m")
    // conf.set("spark.driver.maxResultSize", "0")
    // conf.registerKryoClasses(Array(classOf[Properties], classOf[StanfordCoreNLP]))
    val sc = new SparkContext(conf)

    def main(args: Array[String]): Unit = {

        import org.apache.log4j.{Level, Logger}
        val level = Level.WARN
        Logger.getLogger("org").setLevel(level)
//        Logger.getLogger("akka").setLevel(level)

        // Testing on a sub-sample
        val allExtracted = sc.objectFile[(String, Seq[String])](Commons.ENRON_EXTRACTED_TXT).sample(withReplacement = false, .1, 42L)

        // get custodians from csv file stored in HDFS
        val csv = sc.textFile(Commons.ENRON_CUSTODIANS_CSV_HDFS).map { line => line.split(",") }
        val custodians = sc.broadcast(csv.map { record => Custodian(record(0), record(1), Option(record(2))) }.collect().toSeq)

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

        println("\n\n\n*******************")
        println("dfFull")
        println("*******************")
        dfFull.printSchema()
        println("*******************")

        // this will print on the executors
        // TODO fix parsing! some emails contain the footer and a LOT have empty body
        dfFull.select('emails).flatMap(r => r.getSeq[Email](0)).sample(withReplacement = false, .1, 42L).foreach(println)

        dfFull.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_FULL_DATAFRAME)

        // clean mem
        // dfFull.unpersist(true)
        // custodians.destroy()

        // repartition allParsed (increase partitions)
        println(s"N partitions before resize: ${allParsed.partitions.length}")
        val repartitioned = allParsed.repartition(allParsed.partitions.length * 10)
        println(s"N partitions after resize: ${repartitioned.partitions.length}")

        // classify sentiment and save w/o body
        val mailboxesSentiment = repartitioned.map { mailbox =>
            // load sentiment annotator pipeline
            val nlpProps = new Properties
            nlpProps.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma, sentiment")
            val pipeline = new StanfordCoreNLP(nlpProps)

            // annotation
            val emailsWithSentiment = mailbox.emails.map { email =>
                val document = new Annotation(email.body)
                pipeline.annotate(document)

                val sentences = document.get(classOf[SentencesAnnotation])

                val sentiments = sentences.toList.map { sentence =>
                    val tree = sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree])
                    val sentenceSentiment = RNNCoreAnnotations.getPredictedClass(tree)
                    sentenceSentiment
                }

                // TODO because some emails have empty body, the array is empty, so 0 / 0 = NaN
                val sentiment = sentiments.sum / sentiments.length.toDouble

                println(s"${mailbox.name} - $sentiment - ${email.subject}")
                EmailWithSentiment(email.date, email.from, email.to ++ email.cc ++ email.bcc, email.subject, sentiment)
            }

            MailBoxWithSentiment(mailbox.name, emailsWithSentiment)
        }

        val dfSentiment = mailboxesSentiment.toDF()

        println("\n\n\n*******************")
        println("dfSentiment")
        println("*******************")
        dfSentiment.printSchema()
        println("*******************")
        // dfSentiment.show()

        dfSentiment.write.mode(SaveMode.Overwrite).parquet(Commons.ENRON_SENTIMENT_DATAFRAME)

    }

}
